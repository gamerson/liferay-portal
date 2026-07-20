package main

import (
	"os"

	"github.com/caarlos0/env/v11"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/runtime"
	utilruntime "k8s.io/apimachinery/pkg/util/runtime"
	clientgoscheme "k8s.io/client-go/kubernetes/scheme"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/cache"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/healthz"
	"sigs.k8s.io/controller-runtime/pkg/log/zap"
	metricsserver "sigs.k8s.io/controller-runtime/pkg/metrics/server"
	"sigs.k8s.io/controller-runtime/pkg/webhook/admission"

	licensingv1alpha1 "github.com/liferay/liferay-portal/cloud/operator/api/licensing/v1alpha1"
	licensingcontroller "github.com/liferay/liferay-portal/cloud/operator/internal/controller/licensing"
	licensingwebhook "github.com/liferay/liferay-portal/cloud/operator/internal/webhook/licensing"
)

func init() {
	utilruntime.Must(clientgoscheme.AddToScheme(scheme))
	utilruntime.Must(licensingv1alpha1.AddToScheme(scheme))
}

func main() {
	cfg, _ := env.ParseAs[config]()

	ctrl.SetLogger(zap.New())

	mgr, err := ctrl.NewManager(
		ctrl.GetConfigOrDie(),
		ctrl.Options{
			Cache: cache.Options{
				// Unlabeled objects are filtered out by default to keep the
				// watch cache small. The types the agent must read but does
				// not own (its CR, the target workload, arbitrary Secrets,
				// the namespace) opt out of that filter explicitly.
				DefaultLabelSelector: labels.SelectorFromSet(
					map[string]string{
						"controller-watched": "yes",
					},
				),
				ByObject: map[client.Object]cache.ByObject{
					&appsv1.StatefulSet{}:                   {},
					&corev1.Namespace{}:                     {},
					&corev1.Secret{}:                        {},
					&licensingv1alpha1.LiferayEnvironment{}: {},
				},
			},
			HealthProbeBindAddress: cfg.ProbeAddress,
			Metrics: metricsserver.Options{
				BindAddress: cfg.MetricsAddress,
			},
			Scheme: scheme,
		},
	)

	if err != nil {
		setupLog.Error(err, "Unable to start manager.")

		os.Exit(1)
	}

	if err := mgr.AddHealthzCheck("healthz", healthz.Ping); err != nil {
		setupLog.Error(err, "Unable to set up health check.")

		os.Exit(1)
	}

	if err := mgr.AddReadyzCheck("readyz", healthz.Ping); err != nil {
		setupLog.Error(err, "Unable to set up ready check.")

		os.Exit(1)
	}

	reconciler := &licensingcontroller.LiferayEnvironmentReconciler{
		Client: mgr.GetClient(),
	}

	if err := reconciler.SetupWithManager(mgr); err != nil {
		setupLog.Error(err, "Unable to create controller.")

		os.Exit(1)
	}

	if cfg.WebhookEnabled {
		mgr.GetWebhookServer().Register(
			licensingwebhook.WebhookPath,
			&admission.Webhook{
				Handler: &licensingwebhook.StatefulSetScaleValidator{
					Client:  mgr.GetClient(),
					Decoder: admission.NewDecoder(mgr.GetScheme()),
				},
			},
		)
	}

	if err := mgr.Start(ctrl.SetupSignalHandler()); err != nil {
		setupLog.Error(err, "Unexpected error while running manager.")

		os.Exit(1)
	}
}

type config struct {
	MetricsAddress string `env:"METRICS_ADDRESS" envDefault:":8080"`
	ProbeAddress   string `env:"PROBE_ADDRESS" envDefault:":8081"`
	WebhookEnabled bool   `env:"WEBHOOK_ENABLED" envDefault:"true"`
}

var (
	scheme   = runtime.NewScheme()
	setupLog = ctrl.Log.WithName("setup")
)
