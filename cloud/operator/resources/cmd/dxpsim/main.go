// Command dxpsim simulates the parts of Liferay's portal-k8s-agent that the
// ClientExtension operator interacts with. It is a test double, not Liferay.
//
// It reproduces the observable contract:
//
//   - watches ConfigMaps labeled lxc.liferay.com/metadataType=ext-provision in
//     one namespace, exactly as AgentPortalK8sConfigMapModifier does;
//   - reads the labels as configuration properties, so the serviceId and
//     virtualInstanceId labels win over whatever the payload declares, matching
//     LabelsPortalK8sConfigurationPropertiesMutator;
//   - resolves .serviceAddress and baseURL from the mainDomain annotation, as
//     BaseURLPortalK8sConfigurationPropertiesMutator does;
//   - provisions an OAuth2 application per oAuthApplication* entry and writes
//     the credentials back into <serviceId>-<webId>-lxc-ext-init-metadata,
//     matching BaseConfigurationFactory;
//   - withdraws those entries when the ext-provision ConfigMap is deleted,
//     matching the agent's delete handler.
//
// It also publishes the <webId>-lxc-dxp-metadata ConfigMap that Liferay creates
// per virtual instance.
package main

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"strings"
	"time"

	corev1 "k8s.io/api/core/v1"
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	kubernetes "k8s.io/client-go/kubernetes"
	rest "k8s.io/client-go/rest"
)

const (
	labelMetadataType    = "lxc.liferay.com/metadataType"
	labelProjectName     = "ext.lxc.liferay.com/projectName"
	labelServiceID       = "ext.lxc.liferay.com/serviceId"
	labelVirtualInstance = "dxp.lxc.liferay.com/virtualInstanceId"

	annotationMainDomain = "ext.lxc.liferay.com/mainDomain"

	metadataTypeDXP          = "dxp"
	metadataTypeExtInit      = "ext-init"
	metadataTypeExtProvision = "ext-provision"
)

func main() {
	namespace := envOrDefault("NAMESPACE", "liferay")
	virtualInstanceIDs := strings.Split(envOrDefault("VIRTUAL_INSTANCE_IDS", "liferay.com"), ",")
	virtualHost := envOrDefault("VIRTUAL_HOST", "localhost")
	interval := durationOrDefault("INTERVAL", 3*time.Second)

	config, error := rest.InClusterConfig()

	if error != nil {
		log.Fatalf("dxpsim: unable to read in-cluster config: %v", error)
	}

	clientSet, error := kubernetes.NewForConfig(config)

	if error != nil {
		log.Fatalf("dxpsim: unable to build client: %v", error)
	}

	context := context.Background()

	for _, virtualInstanceID := range virtualInstanceIDs {
		if error := publishDXPMetadata(
			context, clientSet, namespace, strings.TrimSpace(virtualInstanceID), virtualHost,
		); error != nil {
			log.Fatalf("dxpsim: unable to publish dxp metadata: %v", error)
		}
	}

	log.Printf(
		"dxpsim: watching namespace %q for %s=%s",
		namespace, labelMetadataType, metadataTypeExtProvision,
	)

	for {
		if error := reconcile(context, clientSet, namespace); error != nil {
			log.Printf("dxpsim: reconcile error: %v", error)
		}

		time.Sleep(interval)
	}
}

// reconcile provisions every ext-provision ConfigMap and withdraws ext-init
// data whose source ConfigMap has gone away.
func reconcile(context context.Context, clientSet *kubernetes.Clientset, namespace string) error {
	provisions, error := clientSet.CoreV1().ConfigMaps(namespace).List(
		context, metav1.ListOptions{LabelSelector: labelMetadataType + "=" + metadataTypeExtProvision},
	)

	if error != nil {
		return error
	}

	var live = map[string]bool{}

	for index := range provisions.Items {
		provision := &provisions.Items[index]

		serviceID := provision.Labels[labelServiceID]
		virtualInstanceID := provision.Labels[labelVirtualInstance]

		if serviceID == "" || virtualInstanceID == "" {
			log.Printf("dxpsim: skipping %q, missing serviceId or virtualInstanceId label", provision.Name)

			continue
		}

		extInitName := fmt.Sprintf("%s-%s-lxc-ext-init-metadata", serviceID, virtualInstanceID)
		live[extInitName] = true

		if error := provisionOne(context, clientSet, namespace, provision, extInitName); error != nil {
			return error
		}
	}

	extInits, error := clientSet.CoreV1().ConfigMaps(namespace).List(
		context, metav1.ListOptions{LabelSelector: labelMetadataType + "=" + metadataTypeExtInit},
	)

	if error != nil {
		return error
	}

	for index := range extInits.Items {
		extInit := &extInits.Items[index]

		if live[extInit.Name] {
			continue
		}

		log.Printf("dxpsim: withdrawing %q, its ext-provision source is gone", extInit.Name)

		if error := clientSet.CoreV1().ConfigMaps(namespace).Delete(
			context, extInit.Name, metav1.DeleteOptions{},
		); error != nil && !apierrors.IsNotFound(error) {
			return error
		}
	}

	return nil
}

func provisionOne(
	context context.Context, clientSet *kubernetes.Clientset, namespace string,
	provision *corev1.ConfigMap, extInitName string,
) error {
	serviceID := provision.Labels[labelServiceID]
	virtualInstanceID := provision.Labels[labelVirtualInstance]
	mainDomain := provision.Annotations[annotationMainDomain]

	var externalReferenceCodes []string

	for _, document := range provision.Data {
		var payload map[string]map[string]any

		if error := json.Unmarshal([]byte(document), &payload); error != nil {
			log.Printf("dxpsim: %q has an unparseable payload: %v", provision.Name, error)

			return nil
		}

		for pid, entry := range payload {
			extensionType, _ := entry["type"].(string)

			if !strings.HasPrefix(extensionType, "oAuthApplication") {
				continue
			}

			_, externalReferenceCode, _ := strings.Cut(pid, "~")

			externalReferenceCodes = append(externalReferenceCodes, externalReferenceCode)
		}
	}

	existing, error := clientSet.CoreV1().ConfigMaps(namespace).Get(
		context, extInitName, metav1.GetOptions{},
	)

	var data = map[string]string{}

	if error == nil {
		for key, value := range existing.Data {
			data[key] = value
		}
	} else if !apierrors.IsNotFound(error) {
		return error
	}

	homePageURL := "http://" + mainDomain

	if mainDomain == "" {
		homePageURL = "http://" + virtualInstanceID
	}

	var changed bool

	for _, externalReferenceCode := range externalReferenceCodes {
		if _, found := data[externalReferenceCode+".oauth2.headless.server.client.id"]; found {
			continue
		}

		changed = true

		data[externalReferenceCode+".oauth2.authorization.uri"] = "/o/oauth2/authorize"
		data[externalReferenceCode+".oauth2.headless.server.audience"] = homePageURL
		data[externalReferenceCode+".oauth2.headless.server.client.id"] = randomHex(16)
		data[externalReferenceCode+".oauth2.headless.server.client.secret"] = randomHex(32)
		data[externalReferenceCode+".oauth2.home.page.uri"] = homePageURL
		data[externalReferenceCode+".oauth2.introspection.uri"] = "/o/oauth2/introspect"
		data[externalReferenceCode+".oauth2.jwks.uri"] = "/o/oauth2/jwks"
		data[externalReferenceCode+".oauth2.redirect.uris"] = "/o/oauth2/redirect"
		data[externalReferenceCode+".oauth2.token.uri"] = "/o/oauth2/token"
	}

	// A client extension with no OAuth2 application still gets an ext-init
	// ConfigMap so the workload has something to mount, matching the routes
	// directory Liferay writes per project.
	if len(data) == 0 {
		data["com.liferay.lxc.ext.main.domain"] = mainDomain
		changed = true
	}

	if !changed && error == nil {
		return nil
	}

	desired := &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Labels: map[string]string{
				labelMetadataType:    metadataTypeExtInit,
				labelProjectName:     provision.Labels[labelProjectName],
				labelServiceID:       serviceID,
				labelVirtualInstance: virtualInstanceID,
			},
			Name:      extInitName,
			Namespace: namespace,
		},
		Data: data,
	}

	if apierrors.IsNotFound(error) {
		log.Printf("dxpsim: provisioning %q from %q", extInitName, provision.Name)

		_, createError := clientSet.CoreV1().ConfigMaps(namespace).Create(
			context, desired, metav1.CreateOptions{},
		)

		return createError
	}

	log.Printf("dxpsim: updating %q from %q", extInitName, provision.Name)

	desired.ResourceVersion = existing.ResourceVersion

	_, updateError := clientSet.CoreV1().ConfigMaps(namespace).Update(
		context, desired, metav1.UpdateOptions{},
	)

	return updateError
}

func publishDXPMetadata(
	context context.Context, clientSet *kubernetes.Clientset, namespace string,
	virtualInstanceID string, virtualHost string,
) error {
	name := virtualInstanceID + "-lxc-dxp-metadata"

	desired := &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Labels: map[string]string{
				labelMetadataType:    metadataTypeDXP,
				labelVirtualInstance: virtualInstanceID,
			},
			Name:      name,
			Namespace: namespace,
		},
		Data: map[string]string{
			"com.liferay.lxc.dxp.domains":         virtualHost,
			"com.liferay.lxc.dxp.main.domain":     virtualHost,
			"com.liferay.lxc.dxp.mainDomain":      virtualHost,
			"com.liferay.lxc.dxp.server.protocol": "http",
		},
	}

	existing, error := clientSet.CoreV1().ConfigMaps(namespace).Get(
		context, name, metav1.GetOptions{},
	)

	if apierrors.IsNotFound(error) {
		log.Printf("dxpsim: publishing %q", name)

		_, createError := clientSet.CoreV1().ConfigMaps(namespace).Create(
			context, desired, metav1.CreateOptions{},
		)

		return createError
	}

	if error != nil {
		return error
	}

	desired.ResourceVersion = existing.ResourceVersion

	_, updateError := clientSet.CoreV1().ConfigMaps(namespace).Update(
		context, desired, metav1.UpdateOptions{},
	)

	return updateError
}

func durationOrDefault(name string, fallback time.Duration) time.Duration {
	value := os.Getenv(name)

	if value == "" {
		return fallback
	}

	parsed, error := time.ParseDuration(value)

	if error != nil {
		return fallback
	}

	return parsed
}

func envOrDefault(name string, fallback string) string {
	if value := os.Getenv(name); value != "" {
		return value
	}

	return fallback
}

func randomHex(length int) string {
	var buffer = make([]byte, length)

	if _, error := rand.Read(buffer); error != nil {
		return strings.Repeat("0", length*2)
	}

	return hex.EncodeToString(buffer)
}
