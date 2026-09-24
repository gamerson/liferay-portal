package cx_test

import (
	"testing"

	cx "github.com/liferay/liferay-portal/cloud/operator/internal/controller/cx"
)

func TestConfigDigestChangesWithContent(testing_ *testing.T) {
	base := map[string]string{"a.client.id": "id-1", "a.jwks.uri": "/o/oauth2/jwks"}
	same := map[string]string{"a.jwks.uri": "/o/oauth2/jwks", "a.client.id": "id-1"}
	rotated := map[string]string{"a.client.id": "id-2", "a.jwks.uri": "/o/oauth2/jwks"}

	if cx.ConfigDigest(base) != cx.ConfigDigest(same) {
		testing_.Error("map iteration order must not change the digest")
	}

	if cx.ConfigDigest(base) == cx.ConfigDigest(rotated) {
		testing_.Error("a reissued client id must change the digest")
	}

	if cx.ConfigDigest(nil, base) == cx.ConfigDigest(base, nil) {
		testing_.Error("payloads must not be interchangeable between positions")
	}

	if cx.ConfigDigest(map[string]string{"ab": "c"}) == cx.ConfigDigest(map[string]string{"a": "bc"}) {
		testing_.Error("length prefixing must keep key and value boundaries distinct")
	}
}
