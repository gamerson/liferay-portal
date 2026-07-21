package provisioning

import (
	"bytes"
	"context"
	"crypto"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"
)

// HTTPClient is the concrete provisioning/marketplace client. It targets
// BaseURL for the provisioning endpoints (activation, entitlements) and the
// absolute link from the entitlements response for marketplace downloads.
type HTTPClient struct {
	BaseURL string
	Client  *http.Client
}

// NewHTTPClient builds an HTTPClient for the given provisioning base URL, e.g.
// "https://provisioning.liferay.com" or a local mock.
func NewHTTPClient(baseURL string) *HTTPClient {
	return &HTTPClient{
		BaseURL: strings.TrimRight(baseURL, "/"),
		Client:  &http.Client{Timeout: 30 * time.Second},
	}
}

func (c *HTTPClient) Activate(
	ctx context.Context,
	key *rsa.PrivateKey,
	req ActivationRequest,
) error {

	token, err := signJWT(
		key,
		req.EnvironmentId,
		map[string]any{
			"activationCode":  req.ActivationCode,
			"environmentId":   req.EnvironmentId,
			"environmentName": req.EnvironmentName,
			"publicKey":       req.PublicKey,
		},
	)

	if err != nil {
		return err
	}

	url := fmt.Sprintf(
		"%s/o/provisioning-rest/v1.0/cloud/environment/%s/activation",
		c.BaseURL, req.EnvironmentId,
	)

	resp, err := c.post(ctx, url, token)

	if err != nil {
		return err
	}

	defer resp.Body.Close()

	if resp.StatusCode == http.StatusNotFound {
		return ErrActivationRejected
	}

	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("activation: unexpected status %d", resp.StatusCode)
	}

	return nil
}

func (c *HTTPClient) Entitlements(
	ctx context.Context,
	key *rsa.PrivateKey,
	req EntitlementsRequest,
) (*Entitlements, error) {

	token, err := signJWT(
		key,
		req.EnvironmentId,
		map[string]any{
			"dxpVersion":    req.DxpVersion,
			"environmentId": req.EnvironmentId,
		},
	)

	if err != nil {
		return nil, err
	}

	url := fmt.Sprintf(
		"%s/o/provisioning-rest/v1.0/cloud/environment/%s/entitlements",
		c.BaseURL, req.EnvironmentId,
	)

	resp, err := c.post(ctx, url, token)

	if err != nil {
		return nil, err
	}

	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("entitlements: unexpected status %d", resp.StatusCode)
	}

	var wire struct {
		Apps []struct {
			LpkgDownloadLink string `json:"lpkgDownloadLink"`
			Name             string `json:"name"`
		} `json:"apps"`
		LicenseXML      string `json:"licenseXML"`
		MaxClusterNodes int32  `json:"maxClusterNodes"`
	}

	if err := json.NewDecoder(resp.Body).Decode(&wire); err != nil {
		return nil, fmt.Errorf("entitlements: decode response: %w", err)
	}

	entitlements := &Entitlements{
		LicenseXML:      []byte(wire.LicenseXML),
		MaxClusterNodes: wire.MaxClusterNodes,
	}

	for _, app := range wire.Apps {
		entitlements.Apps = append(
			entitlements.Apps,
			App{LpkgDownloadLink: app.LpkgDownloadLink, Name: app.Name},
		)
	}

	return entitlements, nil
}

func (c *HTTPClient) DownloadApp(
	ctx context.Context,
	key *rsa.PrivateKey,
	link string,
	req DownloadRequest,
) ([]byte, error) {

	token, err := signJWT(
		key,
		req.EnvironmentId,
		map[string]any{
			"environmentId":  req.EnvironmentId,
			"virtualEntryId": req.VirtualEntryId,
		},
	)

	if err != nil {
		return nil, err
	}

	resp, err := c.post(ctx, link, token)

	if err != nil {
		return nil, err
	}

	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("download: unexpected status %d", resp.StatusCode)
	}

	return io.ReadAll(resp.Body)
}

// post sends the signed JWT as the request body.
func (c *HTTPClient) post(
	ctx context.Context,
	url string,
	token string,
) (*http.Response, error) {

	request, err := http.NewRequestWithContext(
		ctx, http.MethodPost, url, bytes.NewReader([]byte(token)),
	)

	if err != nil {
		return nil, err
	}

	request.Header.Set("Content-Type", "application/jwt")

	return c.Client.Do(request)
}

// signJWT builds and signs a short-lived RS256 JWT carrying the given claims
// plus standard iss/iat/exp/jti fields.
func signJWT(
	key *rsa.PrivateKey,
	issuer string,
	claims map[string]any,
) (string, error) {

	now := time.Now()

	payload := map[string]any{}

	for k, v := range claims {
		payload[k] = v
	}

	jti, err := randomID()

	if err != nil {
		return "", err
	}

	payload["exp"] = now.Add(60 * time.Second).Unix()
	payload["iat"] = now.Unix()
	payload["iss"] = issuer
	payload["jti"] = jti

	header, err := json.Marshal(map[string]string{"alg": "RS256", "typ": "JWT"})

	if err != nil {
		return "", err
	}

	body, err := json.Marshal(payload)

	if err != nil {
		return "", err
	}

	signingInput := encodeSegment(header) + "." + encodeSegment(body)

	digest := sha256.Sum256([]byte(signingInput))

	signature, err := rsa.SignPKCS1v15(
		rand.Reader, key, crypto.SHA256, digest[:],
	)

	if err != nil {
		return "", err
	}

	return signingInput + "." + encodeSegment(signature), nil
}

func encodeSegment(b []byte) string {
	return base64.RawURLEncoding.EncodeToString(b)
}

func randomID() (string, error) {
	buf := make([]byte, 16)

	if _, err := rand.Read(buf); err != nil {
		return "", err
	}

	return hex.EncodeToString(buf), nil
}
