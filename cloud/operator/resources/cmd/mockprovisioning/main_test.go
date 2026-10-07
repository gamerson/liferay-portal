package main

import (
	"context"
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/base64"
	"errors"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	license "github.com/liferay/liferay-portal/cloud/operator/internal/license"
	provisioning "github.com/liferay/liferay-portal/cloud/operator/internal/provisioning"
)

func TestActivateRejectsAnUnknownActivationCode(t *testing.T) {
	provisioningClient, _ := startMockProvisioningServer(t)

	privateKey := generatePrivateKey(t)

	error := activate("unknown-code", privateKey, provisioningClient, t)

	if !errors.Is(error, provisioning.ErrorActivationRejected) {
		t.Errorf("Activate() error = %v, want %v", error, provisioning.ErrorActivationRejected)
	}
}

func TestManifestIssuesALicenseOwnedByTheEnvironment(t *testing.T) {
	provisioningClient, _ := startMockProvisioningServer(t)

	privateKey := generatePrivateKey(t)

	if error := activate("valid-code", privateKey, provisioningClient, t); error != nil {
		t.Fatalf("Activate() error = %v", error)
	}

	entitlements, error := provisioningClient.Manifest(
		context.Background(),
		provisioning.ManifestRequest{EnvironmentID: "environment-uid"},
		privateKey,
	)

	if error != nil {
		t.Fatalf("Manifest() error = %v", error)
	}

	owner, error := license.Owner(entitlements.LicenseXML)

	if error != nil || owner != "environment-uid" {
		t.Errorf("license.Owner() = %q, %v, want %q", owner, error, "environment-uid")
	}

	expirationDate, error := license.ExpirationDate(entitlements.LicenseXML)

	if error != nil || !expirationDate.After(time.Now()) {
		t.Errorf("license.ExpirationDate() = %v, %v, want a future date", expirationDate, error)
	}

	if entitlements.MaxClusterNodes != 2 {
		t.Errorf("MaxClusterNodes = %d, want 2", entitlements.MaxClusterNodes)
	}

	if len(entitlements.AddOns) != 1 {
		t.Fatalf("AddOns = %v, want one add-on", entitlements.AddOns)
	}

	readCloser, error := provisioningClient.DownloadAddOn(
		context.Background(),
		provisioning.DownloadRequest{
			DownloadURL:    entitlements.AddOns[0].DownloadURL,
			EnvironmentID:  "environment-uid",
			VirtualEntryID: entitlements.AddOns[0].VirtualEntryID,
		},
		privateKey,
	)

	if error != nil {
		t.Fatalf("DownloadAddOn() error = %v", error)
	}

	readCloser.Close()
}

func TestManifestRejectsAKeyTheEnvironmentDidNotRegister(t *testing.T) {
	provisioningClient, _ := startMockProvisioningServer(t)

	if error := activate(
		"valid-code", generatePrivateKey(t), provisioningClient, t,
	); error != nil {
		t.Fatalf("Activate() error = %v", error)
	}

	_, error := provisioningClient.Manifest(
		context.Background(),
		provisioning.ManifestRequest{EnvironmentID: "environment-uid"},
		generatePrivateKey(t),
	)

	if error == nil || !strings.Contains(error.Error(), "401") {
		t.Errorf("Manifest() error = %v, want an unauthorized response", error)
	}
}

func TestManifestReportsAnUnreachableService(t *testing.T) {
	provisioningClient, mockProvisioningServer := startMockProvisioningServer(t)

	privateKey := generatePrivateKey(t)

	if error := activate("valid-code", privateKey, provisioningClient, t); error != nil {
		t.Fatalf("Activate() error = %v", error)
	}

	mockProvisioningServer.reachable = false

	_, error := provisioningClient.Manifest(
		context.Background(),
		provisioning.ManifestRequest{EnvironmentID: "environment-uid"},
		privateKey,
	)

	if error == nil || !strings.Contains(error.Error(), "503") {
		t.Errorf("Manifest() error = %v, want an unavailable response", error)
	}
}

func activate(
	activationCode string,
	privateKey *rsa.PrivateKey,
	provisioningClient *provisioning.HTTPClient,
	t *testing.T,
) error {
	t.Helper()

	publicKeyBytes, error := x509.MarshalPKIXPublicKey(&privateKey.PublicKey)

	if error != nil {
		t.Fatalf("Unable to encode the public key: %v", error)
	}

	return provisioningClient.Activate(
		provisioning.ActivationRequest{
			ActivationCode: activationCode,
			EnvironmentID:  "environment-uid",
			PublicKey:      base64.StdEncoding.EncodeToString(publicKeyBytes),
		},
		context.Background(),
		privateKey,
	)
}

func generatePrivateKey(t *testing.T) *rsa.PrivateKey {
	t.Helper()

	privateKey, error := rsa.GenerateKey(rand.Reader, 2048)

	if error != nil {
		t.Fatalf("Unable to generate a key: %v", error)
	}

	return privateKey
}

func startMockProvisioningServer(
	t *testing.T,
) (*provisioning.HTTPClient, *mockProvisioningServer) {
	t.Helper()

	mockProvisioningServer, error := newMockProvisioningServer(
		config{
			ActivationCodes: []string{"valid-code"},
			LicenseValidity: time.Hour,
			MaxClusterNodes: 2,
		},
	)

	if error != nil {
		t.Fatalf("Unable to create the mock: %v", error)
	}

	server := httptest.NewServer(mockProvisioningServer.Handler())

	t.Cleanup(server.Close)

	mockProvisioningServer.config.BaseURL = server.URL

	return provisioning.NewHTTPClient(server.URL), mockProvisioningServer
}
