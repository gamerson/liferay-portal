// A stand-in for the Liferay provisioning service, for local reproductions.
//
// Activation always succeeds. The manifest response is generated per request,
// so that the license owner defaults to the environment ID of the caller, which
// is the namespace UID the operator compares it against. POST a JSON body to
// /_config to change the ceiling, the owner, or the expiration date at runtime,
// and GET /_calls to read back every request the operator has made.
//
// The signed request bodies the operator sends are not verified, since nothing
// here needs to trust the caller.
package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"sync"
	"time"

	env "github.com/caarlos0/env/v11"
	provisioning "github.com/liferay/liferay-portal/cloud/operator/internal/provisioning"
)

const environmentIDPathValue = "environmentID"

func (mock *mock) handleActivation(
	responseWriter http.ResponseWriter, request *http.Request,
) {
	mock.record(request)

	log.Printf(
		"Activated environment %q", request.PathValue(environmentIDPathValue),
	)

	respond(struct{}{}, responseWriter, http.StatusOK)
}

func (mock *mock) handleCalls(
	responseWriter http.ResponseWriter, request *http.Request,
) {
	mock.mutex.Lock()
	defer mock.mutex.Unlock()

	respond(mock.calls, responseWriter, http.StatusOK)
}

func (mock *mock) handleConfiguration(
	responseWriter http.ResponseWriter, request *http.Request,
) {
	if request.Method == http.MethodPost {
		if error := mock.reconfigure(request); error != nil {
			log.Printf("Unable to reconfigure: %v", error)

			respond(
				map[string]string{"error": error.Error()},
				responseWriter, http.StatusBadRequest,
			)

			return
		}
	}

	mock.mutex.Lock()
	defer mock.mutex.Unlock()

	respond(mock.state(), responseWriter, http.StatusOK)
}

func (mock *mock) handleManifest(
	responseWriter http.ResponseWriter, request *http.Request,
) {
	mock.record(request)

	environmentID := request.PathValue(environmentIDPathValue)

	mock.mutex.Lock()
	defer mock.mutex.Unlock()

	owner := environmentID

	if mock.licenseOwner != nil {
		owner = *mock.licenseOwner
	}

	log.Printf(
		"Issued a license for environment %q owned by %q with a ceiling of %d",
		environmentID, owner, mock.maxClusterNodes,
	)

	respond(
		provisioning.EntitlementsResponse{
			AddOns: []provisioning.AddOn{},
			LicenseXML: base64.StdEncoding.EncodeToString(
				[]byte(mock.licenseXML(owner)),
			),
			MaxClusterNodes: mock.maxClusterNodes,
		},
		responseWriter, http.StatusOK,
	)
}

func (mock *mock) licenseXML(owner string) string {
	return fmt.Sprintf(
		"<licenses><license>"+
			"<owner>%s</owner>"+
			"<expiration-date>%s</expiration-date>"+
			"<license-type>virtual-cluster</license-type>"+
			"<max-cluster-nodes>%d</max-cluster-nodes>"+
			"</license></licenses>",
		owner, mock.expirationDate, mock.maxClusterNodes,
	)
}

func main() {
	config, configError := env.ParseAs[config]()

	if configError != nil {
		log.Printf(
			"Unable to read configuration, falling back to defaults: %v",
			configError,
		)
	}

	mock := &mock{
		calls:           []call{},
		expirationDate:  config.ExpirationDate,
		maxClusterNodes: config.MaxClusterNodes,
	}

	serveMux := http.NewServeMux()

	serveMux.HandleFunc("/_calls", mock.handleCalls)
	serveMux.HandleFunc("/_config", mock.handleConfiguration)
	serveMux.HandleFunc(
		"POST /cloud/v1/environments/{"+environmentIDPathValue+"}/activation",
		mock.handleActivation,
	)
	serveMux.HandleFunc(
		"POST /cloud/v1/environments/{"+environmentIDPathValue+"}/manifest",
		mock.handleManifest,
	)

	log.Printf(
		"Serving the provisioning mock on %s with a ceiling of %d",
		config.Address, config.MaxClusterNodes,
	)

	server := &http.Server{
		Addr:              config.Address,
		Handler:           serveMux,
		ReadHeaderTimeout: 10 * time.Second,
	}

	log.Fatal(server.ListenAndServe())
}

func (mock *mock) reconfigure(request *http.Request) error {
	var fields map[string]json.RawMessage

	if error := json.NewDecoder(request.Body).Decode(&fields); error != nil {
		return fmt.Errorf("provisioning mock: decode configuration: %w", error)
	}

	mock.mutex.Lock()
	defer mock.mutex.Unlock()

	for name, value := range fields {
		var error error

		switch name {
		case "expirationDate":
			error = json.Unmarshal(value, &mock.expirationDate)
		case "licenseOwner":
			mock.licenseOwner = nil

			if string(value) != "null" {
				var owner string

				if error = json.Unmarshal(value, &owner); error == nil {
					mock.licenseOwner = &owner
				}
			}
		case "maxClusterNodes":
			error = json.Unmarshal(value, &mock.maxClusterNodes)
		default:
			error = fmt.Errorf("unrecognized field %q", name)
		}

		if error != nil {
			return fmt.Errorf("provisioning mock: %q: %w", name, error)
		}
	}

	return nil
}

func (mock *mock) record(request *http.Request) {
	mock.mutex.Lock()
	defer mock.mutex.Unlock()

	mock.calls = append(
		mock.calls,
		call{
			Method: request.Method,
			Path:   request.URL.Path,
			Time:   time.Now(),
		},
	)
}

func respond(payload any, responseWriter http.ResponseWriter, status int) {
	body, error := json.Marshal(payload)

	if error != nil {
		http.Error(
			responseWriter, "unable to marshal the response",
			http.StatusInternalServerError,
		)

		return
	}

	responseWriter.Header().Set("Content-Type", "application/json")

	responseWriter.WriteHeader(status)

	if _, error := responseWriter.Write(body); error != nil {
		log.Printf("Unable to write the response: %v", error)
	}
}

func (mock *mock) state() state {
	return state{
		ExpirationDate:  mock.expirationDate,
		LicenseOwner:    mock.licenseOwner,
		MaxClusterNodes: mock.maxClusterNodes,
	}
}

type call struct {
	Method string    `json:"method"`
	Path   string    `json:"path"`
	Time   time.Time `json:"time"`
}

type config struct {
	Address         string `env:"ADDRESS" envDefault:":8080"`
	ExpirationDate  string `env:"EXPIRATION_DATE" envDefault:"Friday, March 2, 2029 12:00:00 AM GMT"`
	MaxClusterNodes int32  `env:"MAX_CLUSTER_NODES" envDefault:"3"`
}

type mock struct {
	calls           []call
	expirationDate  string
	licenseOwner    *string
	maxClusterNodes int32
	mutex           sync.Mutex
}

type state struct {
	ExpirationDate  string  `json:"expirationDate"`
	LicenseOwner    *string `json:"licenseOwner"`
	MaxClusterNodes int32   `json:"maxClusterNodes"`
}
