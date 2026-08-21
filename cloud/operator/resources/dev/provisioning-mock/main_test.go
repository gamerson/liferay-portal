package main

import (
	"encoding/base64"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	license "github.com/liferay/liferay-portal/cloud/operator/internal/license"
	provisioning "github.com/liferay/liferay-portal/cloud/operator/internal/provisioning"
)

func TestHandleConfigurationUpdatesOnlyTheFieldsSent(t *testing.T) {
	owner := "original-owner"

	testCases := map[string]struct {
		body                string
		wantExpirationDate  string
		wantLicenseOwner    *string
		wantMaxClusterNodes int32
	}{
		"a null owner falls back to the caller": {
			body:                `{"licenseOwner": null}`,
			wantExpirationDate:  "Friday, March 2, 2029 12:00:00 AM GMT",
			wantLicenseOwner:    nil,
			wantMaxClusterNodes: 3,
		},
		"an absent field is left alone": {
			body:                `{"maxClusterNodes": 1}`,
			wantExpirationDate:  "Friday, March 2, 2029 12:00:00 AM GMT",
			wantLicenseOwner:    &owner,
			wantMaxClusterNodes: 1,
		},
		"every field at once": {
			body: `{
				"expirationDate": "Sunday, January 5, 2031 12:00:00 AM GMT",
				"licenseOwner": "another-owner",
				"maxClusterNodes": 7
			}`,
			wantExpirationDate:  "Sunday, January 5, 2031 12:00:00 AM GMT",
			wantLicenseOwner:    pointerString("another-owner"),
			wantMaxClusterNodes: 7,
		},
	}

	for name, testCase := range testCases {
		t.Run(name, func(t *testing.T) {
			mock := newMock()
			mock.licenseOwner = &owner

			responseRecorder := configure(mock, testCase.body)

			if responseRecorder.Code != http.StatusOK {
				t.Fatalf(
					"Code = %d, want %d",
					responseRecorder.Code, http.StatusOK,
				)
			}

			if mock.expirationDate != testCase.wantExpirationDate {
				t.Errorf(
					"expirationDate = %q, want %q",
					mock.expirationDate, testCase.wantExpirationDate,
				)
			}

			if mock.maxClusterNodes != testCase.wantMaxClusterNodes {
				t.Errorf(
					"maxClusterNodes = %d, want %d",
					mock.maxClusterNodes, testCase.wantMaxClusterNodes,
				)
			}

			if testCase.wantLicenseOwner == nil {
				if mock.licenseOwner != nil {
					t.Errorf("licenseOwner = %q, want nil", *mock.licenseOwner)
				}

				return
			}

			if mock.licenseOwner == nil {
				t.Fatalf("licenseOwner = nil, want %q", *testCase.wantLicenseOwner)
			}

			if *mock.licenseOwner != *testCase.wantLicenseOwner {
				t.Errorf(
					"licenseOwner = %q, want %q",
					*mock.licenseOwner, *testCase.wantLicenseOwner,
				)
			}
		})
	}
}

func TestHandleConfigurationRejectsAnUnrecognizedField(t *testing.T) {
	responseRecorder := configure(newMock(), `{"ceiling": 3}`)

	if responseRecorder.Code != http.StatusBadRequest {
		t.Fatalf(
			"Code = %d, want %d",
			responseRecorder.Code, http.StatusBadRequest,
		)
	}
}

func TestHandleManifestIssuesALicenseTheOperatorAccepts(t *testing.T) {
	testCases := map[string]struct {
		licenseOwner *string
		wantOwner    string
	}{
		"a configured owner forces a mismatch": {
			licenseOwner: pointerString("another-environment-uid"),
			wantOwner:    "another-environment-uid",
		},
		"no configured owner echoes the caller": {
			licenseOwner: nil,
			wantOwner:    "dev-namespace-uid",
		},
	}

	for name, testCase := range testCases {
		t.Run(name, func(t *testing.T) {
			mock := newMock()
			mock.licenseOwner = testCase.licenseOwner

			request := httptest.NewRequest(
				http.MethodPost,
				"/cloud/v1/environments/dev-namespace-uid/manifest", nil,
			)
			request.SetPathValue(environmentIDPathValue, "dev-namespace-uid")

			responseRecorder := httptest.NewRecorder()

			mock.handleManifest(responseRecorder, request)

			if responseRecorder.Code != http.StatusOK {
				t.Fatalf(
					"Code = %d, want %d",
					responseRecorder.Code, http.StatusOK,
				)
			}

			var entitlementsResponse provisioning.EntitlementsResponse

			if error := json.NewDecoder(
				responseRecorder.Body,
			).Decode(&entitlementsResponse); error != nil {
				t.Fatalf("Unable to decode the response: %v", error)
			}

			entitlements, error := provisioning.EntitlementsFromResponse(
				entitlementsResponse,
			)

			if error != nil {
				t.Fatalf("Unable to read the entitlements: %v", error)
			}

			owner, error := license.Owner(entitlements.LicenseXML)

			if error != nil {
				t.Fatalf("Unable to read the license owner: %v", error)
			}

			if owner != testCase.wantOwner {
				t.Errorf("Owner = %q, want %q", owner, testCase.wantOwner)
			}

			if _, error := license.ExpirationDate(
				entitlements.LicenseXML,
			); error != nil {
				t.Errorf("Unable to read the expiration date: %v", error)
			}

			if entitlements.MaxClusterNodes != 3 {
				t.Errorf(
					"MaxClusterNodes = %d, want 3",
					entitlements.MaxClusterNodes,
				)
			}
		})
	}
}

func TestHandleManifestReturnsBase64EncodedXML(t *testing.T) {
	request := httptest.NewRequest(
		http.MethodPost, "/cloud/v1/environments/dev/manifest", nil,
	)
	request.SetPathValue(environmentIDPathValue, "dev")

	responseRecorder := httptest.NewRecorder()

	newMock().handleManifest(responseRecorder, request)

	var entitlementsResponse provisioning.EntitlementsResponse

	if error := json.NewDecoder(
		responseRecorder.Body,
	).Decode(&entitlementsResponse); error != nil {
		t.Fatalf("Unable to decode the response: %v", error)
	}

	licenseXML, error := base64.StdEncoding.DecodeString(
		entitlementsResponse.LicenseXML,
	)

	if error != nil {
		t.Fatalf("licenseXML is not base64 encoded: %v", error)
	}

	if !strings.Contains(string(licenseXML), "<license-type>virtual-cluster") {
		t.Errorf(
			"licenseXML = %q, want a virtual-cluster license", licenseXML,
		)
	}
}

func configure(mock *mock, body string) *httptest.ResponseRecorder {
	request := httptest.NewRequest(
		http.MethodPost, "/_config", strings.NewReader(body),
	)

	responseRecorder := httptest.NewRecorder()

	mock.handleConfiguration(responseRecorder, request)

	return responseRecorder
}

func newMock() *mock {
	return &mock{
		calls:           []call{},
		expirationDate:  "Friday, March 2, 2029 12:00:00 AM GMT",
		maxClusterNodes: 3,
	}
}

func pointerString(value string) *string {
	return &value
}
