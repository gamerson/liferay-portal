// Mock provisioning serves the provisioning API that the DXP operator and DXP
// call, so that activation and heartbeats can be exercised in a local cluster
// without the real service. It verifies every request's signature the way the
// real service does, issues a license whose owner is the requesting
// environment, and exposes admin endpoints that change what it answers.
package main

import (
	"archive/zip"
	"bytes"
	"crypto"
	"crypto/rsa"
	"crypto/sha256"
	"crypto/x509"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"os"
	"slices"
	"strconv"
	"strings"
	"sync"
	"time"

	env "github.com/caarlos0/env/v11"
)

const (
	addOnVirtualEntryID = 77
	dateLayout          = "Monday, January 2, 2006 3:04:05 PM MST"
)

func (mockProvisioningServer *mockProvisioningServer) Handler() http.Handler {
	serveMux := http.NewServeMux()

	serveMux.HandleFunc(
		"GET /admin/environments", mockProvisioningServer.listEnvironments,
	)
	serveMux.HandleFunc(
		"POST /admin/environments/{environmentID}/max-cluster-nodes",
		mockProvisioningServer.setMaxClusterNodes,
	)
	serveMux.HandleFunc(
		"POST /admin/reachable", mockProvisioningServer.setReachable,
	)
	serveMux.HandleFunc(
		"POST /cloud/v1/environments/{environmentID}/activation",
		mockProvisioningServer.activate,
	)
	serveMux.HandleFunc(
		"POST /cloud/v1/environments/{environmentID}/manifest",
		mockProvisioningServer.manifest,
	)
	serveMux.HandleFunc(
		"POST /marketplace/virtual-entry/{virtualEntryID}",
		mockProvisioningServer.downloadAddOn,
	)

	return serveMux
}

func (mockProvisioningServer *mockProvisioningServer) activate(
	responseWriter http.ResponseWriter,
	request *http.Request,
) {
	environmentID := request.PathValue("environmentID")

	signedToken, error := readSignedToken(request)

	if error != nil {
		http.Error(responseWriter, error.Error(), http.StatusBadRequest)

		return
	}

	publicKey, error := parsePublicKey(fmt.Sprint(signedToken.Claims["publicKey"]))

	if error != nil {
		http.Error(responseWriter, error.Error(), http.StatusBadRequest)

		return
	}

	if error := signedToken.verify(environmentID, publicKey); error != nil {
		http.Error(responseWriter, error.Error(), http.StatusUnauthorized)

		return
	}

	if !slices.Contains(
		mockProvisioningServer.config.ActivationCodes,
		fmt.Sprint(signedToken.Claims["activationCode"]),
	) {
		slog.Info("Rejected activation", "environmentID", environmentID)

		http.NotFound(responseWriter, request)

		return
	}

	mockProvisioningServer.mutex.Lock()

	defer mockProvisioningServer.mutex.Unlock()

	provisionedEnvironment, ok := mockProvisioningServer.provisionedEnvironments[environmentID]

	if !ok {
		provisionedEnvironment = mockProvisioningServer.newProvisionedEnvironment()

		mockProvisioningServer.provisionedEnvironments[environmentID] = provisionedEnvironment
	}

	provisionedEnvironment.EnvironmentName = fmt.Sprint(
		signedToken.Claims["environmentName"],
	)
	provisionedEnvironment.publicKey = publicKey

	slog.Info(
		"Activated environment",
		"environmentID", environmentID,
		"environmentName", provisionedEnvironment.EnvironmentName,
	)
}

func (mockProvisioningServer *mockProvisioningServer) authenticate(
	environmentID string,
	signedToken *signedToken,
) (*provisionedEnvironment, int, error) {
	mockProvisioningServer.mutex.Lock()

	provisionedEnvironment, ok := mockProvisioningServer.provisionedEnvironments[environmentID]

	mockProvisioningServer.mutex.Unlock()

	if !ok {
		return nil, http.StatusNotFound, fmt.Errorf(
			"environment %s is not activated", environmentID,
		)
	}

	if error := signedToken.verify(
		environmentID, provisionedEnvironment.publicKey,
	); error != nil {
		return nil, http.StatusUnauthorized, error
	}

	return provisionedEnvironment, http.StatusOK, nil
}

func (mockProvisioningServer *mockProvisioningServer) downloadAddOn(
	responseWriter http.ResponseWriter,
	request *http.Request,
) {
	signedToken, error := readSignedToken(request)

	if error != nil {
		http.Error(responseWriter, error.Error(), http.StatusBadRequest)

		return
	}

	if _, status, error := mockProvisioningServer.authenticate(
		fmt.Sprint(signedToken.Claims["environmentID"]), signedToken,
	); error != nil {
		http.Error(responseWriter, error.Error(), status)

		return
	}

	responseWriter.Header().Set("Content-Type", "application/octet-stream")

	responseWriter.Write(mockProvisioningServer.addOn)
}

func (mockProvisioningServer *mockProvisioningServer) licenseXML(
	environmentID string,
	maxClusterNodes int32,
) string {
	now := time.Now().UTC()

	return fmt.Sprintf(
		`<?xml version="1.0"?>
<licenses>
	<license>
		<account-name>Mock Provisioning</account-name>
		<expiration-date>%s</expiration-date>
		<license-type>virtual-cluster</license-type>
		<max-cluster-nodes>%d</max-cluster-nodes>
		<owner>%s</owner>
		<product-name>Liferay DXP</product-name>
		<start-date>%s</start-date>
	</license>
</licenses>
`,
		now.Add(mockProvisioningServer.config.LicenseValidity).Format(dateLayout),
		maxClusterNodes, environmentID, now.Format(dateLayout),
	)
}

func (mockProvisioningServer *mockProvisioningServer) listEnvironments(
	responseWriter http.ResponseWriter,
	request *http.Request,
) {
	mockProvisioningServer.mutex.Lock()

	defer mockProvisioningServer.mutex.Unlock()

	responseWriter.Header().Set("Content-Type", "application/json")

	json.NewEncoder(responseWriter).Encode(
		map[string]any{
			"environments": mockProvisioningServer.provisionedEnvironments,
			"reachable":    mockProvisioningServer.reachable,
		},
	)
}

func main() {
	config, error := env.ParseAs[config]()

	if error != nil {
		slog.Error("Unable to read configuration", "error", error)

		os.Exit(1)
	}

	mockProvisioningServer, error := newMockProvisioningServer(config)

	if error != nil {
		slog.Error("Unable to build the mock add-on", "error", error)

		os.Exit(1)
	}

	slog.Info(
		"Starting mock provisioning",
		"activationCodes", config.ActivationCodes,
		"address", config.Address,
		"maxClusterNodes", config.MaxClusterNodes,
	)

	if error := http.ListenAndServe(
		config.Address, mockProvisioningServer.Handler(),
	); error != nil {
		slog.Error("Unable to serve", "error", error)

		os.Exit(1)
	}
}

func (mockProvisioningServer *mockProvisioningServer) manifest(
	responseWriter http.ResponseWriter,
	request *http.Request,
) {
	environmentID := request.PathValue("environmentID")

	signedToken, error := readSignedToken(request)

	if error != nil {
		http.Error(responseWriter, error.Error(), http.StatusBadRequest)

		return
	}

	provisionedEnvironment, status, error := mockProvisioningServer.authenticate(
		environmentID, signedToken,
	)

	if error != nil {
		http.Error(responseWriter, error.Error(), status)

		return
	}

	mockProvisioningServer.mutex.Lock()

	maxClusterNodes := provisionedEnvironment.MaxClusterNodes
	reachable := mockProvisioningServer.reachable

	mockProvisioningServer.mutex.Unlock()

	if !reachable {
		http.Error(
			responseWriter, "provisioning is unavailable",
			http.StatusServiceUnavailable,
		)

		return
	}

	addOnChecksum := sha256.Sum256(mockProvisioningServer.addOn)

	responseWriter.Header().Set("Content-Type", "application/json")

	json.NewEncoder(responseWriter).Encode(
		map[string]any{
			"add-ons": []map[string]any{
				{
					"downloadURL": fmt.Sprintf(
						"%s/marketplace/virtual-entry/%d",
						mockProvisioningServer.config.BaseURL,
						addOnVirtualEntryID,
					),
					"productId":      "mock-add-on",
					"productName":    "Mock Add-on",
					"sha256Checksum": hex.EncodeToString(addOnChecksum[:]),
					"version":        "1.0.0",
					"virtualEntryId": addOnVirtualEntryID,
				},
			},
			"licenseXML": base64.StdEncoding.EncodeToString(
				[]byte(
					mockProvisioningServer.licenseXML(
						environmentID, maxClusterNodes,
					),
				),
			),
			"maxClusterNodes": maxClusterNodes,
		},
	)

	slog.Info(
		"Issued entitlements",
		"environmentID", environmentID,
		"maxClusterNodes", maxClusterNodes,
	)
}

func newAddOn() ([]byte, error) {
	var buffer bytes.Buffer

	zipWriter := zip.NewWriter(&buffer)

	writer, error := zipWriter.Create("liferay-marketplace.properties")

	if error != nil {
		return nil, error
	}

	if _, error := writer.Write(
		[]byte("title=Mock Add-on\nversion=1.0.0\n"),
	); error != nil {
		return nil, error
	}

	if error := zipWriter.Close(); error != nil {
		return nil, error
	}

	return buffer.Bytes(), nil
}

func newMockProvisioningServer(config config) (*mockProvisioningServer, error) {
	addOn, error := newAddOn()

	if error != nil {
		return nil, error
	}

	return &mockProvisioningServer{
		addOn:                   addOn,
		config:                  config,
		provisionedEnvironments: map[string]*provisionedEnvironment{},
		reachable:               true,
	}, nil
}

func (mockProvisioningServer *mockProvisioningServer) newProvisionedEnvironment() *provisionedEnvironment {
	return &provisionedEnvironment{
		MaxClusterNodes: mockProvisioningServer.config.MaxClusterNodes,
	}
}

func parsePublicKey(publicKeyBase64 string) (*rsa.PublicKey, error) {
	publicKeyBytes, error := base64.StdEncoding.DecodeString(publicKeyBase64)

	if error != nil {
		return nil, fmt.Errorf("public key: %w", error)
	}

	publicKey, error := x509.ParsePKIXPublicKey(publicKeyBytes)

	if error != nil {
		return nil, fmt.Errorf("public key: %w", error)
	}

	rsaPublicKey, ok := publicKey.(*rsa.PublicKey)

	if !ok {
		return nil, errors.New("public key: not an RSA key")
	}

	return rsaPublicKey, nil
}

func readSignedToken(request *http.Request) (*signedToken, error) {
	body, error := io.ReadAll(request.Body)

	if error != nil {
		return nil, error
	}

	segments := strings.Split(strings.TrimSpace(string(body)), ".")

	if len(segments) != 3 {
		return nil, errors.New("token: not a JWT")
	}

	payload, error := base64.RawURLEncoding.DecodeString(segments[1])

	if error != nil {
		return nil, fmt.Errorf("token: %w", error)
	}

	var claims map[string]any

	if error := json.Unmarshal(payload, &claims); error != nil {
		return nil, fmt.Errorf("token: %w", error)
	}

	return &signedToken{
		Claims:   claims,
		segments: segments,
	}, nil
}

func (mockProvisioningServer *mockProvisioningServer) setMaxClusterNodes(
	responseWriter http.ResponseWriter,
	request *http.Request,
) {
	body, error := io.ReadAll(request.Body)

	if error != nil {
		http.Error(responseWriter, error.Error(), http.StatusBadRequest)

		return
	}

	maxClusterNodes, error := strconv.ParseInt(strings.TrimSpace(string(body)), 10, 32)

	if error != nil {
		http.Error(responseWriter, error.Error(), http.StatusBadRequest)

		return
	}

	environmentID := request.PathValue("environmentID")

	mockProvisioningServer.mutex.Lock()

	defer mockProvisioningServer.mutex.Unlock()

	provisionedEnvironment, ok := mockProvisioningServer.provisionedEnvironments[environmentID]

	if !ok {
		http.NotFound(responseWriter, request)

		return
	}

	provisionedEnvironment.MaxClusterNodes = int32(maxClusterNodes)

	slog.Info(
		"Set the cluster nodes of an environment",
		"environmentID", environmentID,
		"maxClusterNodes", maxClusterNodes,
	)
}

func (mockProvisioningServer *mockProvisioningServer) setReachable(
	responseWriter http.ResponseWriter,
	request *http.Request,
) {
	body, error := io.ReadAll(request.Body)

	if error != nil {
		http.Error(responseWriter, error.Error(), http.StatusBadRequest)

		return
	}

	reachable, error := strconv.ParseBool(strings.TrimSpace(string(body)))

	if error != nil {
		http.Error(responseWriter, error.Error(), http.StatusBadRequest)

		return
	}

	mockProvisioningServer.mutex.Lock()

	mockProvisioningServer.reachable = reachable

	mockProvisioningServer.mutex.Unlock()

	slog.Info("Set reachability", "reachable", reachable)
}

func (signedToken *signedToken) verify(
	environmentID string,
	publicKey *rsa.PublicKey,
) error {
	signature, error := base64.RawURLEncoding.DecodeString(signedToken.segments[2])

	if error != nil {
		return fmt.Errorf("token: %w", error)
	}

	digest := sha256.Sum256(
		[]byte(signedToken.segments[0] + "." + signedToken.segments[1]),
	)

	if error := rsa.VerifyPKCS1v15(
		publicKey, crypto.SHA256, digest[:], signature,
	); error != nil {
		return fmt.Errorf("token: signature: %w", error)
	}

	if signedToken.Claims["environmentID"] != environmentID ||
		signedToken.Claims["iss"] != environmentID {

		return fmt.Errorf(
			"token: issued for environment %v", signedToken.Claims["iss"],
		)
	}

	expiration, ok := signedToken.Claims["exp"].(float64)

	if !ok || time.Now().Unix() >= int64(expiration) {
		return errors.New("token: expired")
	}

	return nil
}

type config struct {
	ActivationCodes []string      `env:"MOCK_ACTIVATION_CODES" envDefault:"demo-activation-code"`
	Address         string        `env:"ADDRESS" envDefault:":8080"`
	BaseURL         string        `env:"MOCK_BASE_URL" envDefault:"http://localhost:8080"`
	LicenseValidity time.Duration `env:"MOCK_LICENSE_VALIDITY" envDefault:"8760h"`
	MaxClusterNodes int32         `env:"MOCK_MAX_CLUSTER_NODES" envDefault:"2"`
}

type mockProvisioningServer struct {
	addOn                   []byte
	config                  config
	mutex                   sync.Mutex
	provisionedEnvironments map[string]*provisionedEnvironment
	reachable               bool
}

type provisionedEnvironment struct {
	EnvironmentName string `json:"environmentName"`
	MaxClusterNodes int32  `json:"maxClusterNodes"`

	publicKey *rsa.PublicKey
}

type signedToken struct {
	Claims map[string]any

	segments []string
}
