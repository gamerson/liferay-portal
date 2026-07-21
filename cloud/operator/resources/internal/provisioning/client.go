// Package provisioning is the client boundary to Liferay's provisioning and
// marketplace REST APIs. Every call is a JWT signed with the environment's
// private key; see the technical design for the trust model.
package provisioning

import (
	"context"
	"crypto/rsa"
	"errors"
)

// ErrActivationRejected is returned when provisioning rejects the activation
// (HTTP 404) — an invalid or already-consumed activation code. It is terminal:
// the reconciler should stop retrying and surface it for operator attention.
var ErrActivationRejected = errors.New("provisioning: activation rejected")

// Client talks to the provisioning and marketplace services. Each call is
// signed as a JWT with the environment's private key (never shared with pods)
// and sent over TLS to the Liferay hosts.
type Client interface {
	// Activate performs the one-time activation (API #1). A rejected or
	// already-consumed code surfaces as ErrActivationRejected.
	Activate(ctx context.Context, key *rsa.PrivateKey, req ActivationRequest) error

	// Entitlements fetches the current license and add-on list (API #2).
	Entitlements(ctx context.Context, key *rsa.PrivateKey, req EntitlementsRequest) (*Entitlements, error)

	// DownloadApp fetches one add-on's lpkg binary (API #3). The link is the
	// full marketplace URL from the entitlements response.
	DownloadApp(ctx context.Context, key *rsa.PrivateKey, link string, req DownloadRequest) ([]byte, error)
}

// ActivationRequest is the payload of the activation JWT.
type ActivationRequest struct {
	ActivationCode  string
	EnvironmentId   string
	EnvironmentName string
	PublicKey       string
}

// EntitlementsRequest is the payload of the entitlements JWT.
type EntitlementsRequest struct {
	DxpVersion    string
	EnvironmentId string
}

// DownloadRequest is the payload of the marketplace download JWT.
type DownloadRequest struct {
	EnvironmentId  string
	VirtualEntryId string
}

// Entitlements is the decoded response of API #2.
type Entitlements struct {
	Apps            []App
	LicenseXML      []byte
	MaxClusterNodes int32
}

// App is one entitled Marketplace add-on.
type App struct {
	LpkgDownloadLink string
	Name             string
}
