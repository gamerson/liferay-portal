#!/usr/bin/env python3
"""A mock of the Liferay provisioning + marketplace APIs for local testing.

It implements the three endpoints from the CNE ↔ provisioning contract and
returns canned responses so the operator's reconcile loop can complete its
activation → entitlements → clamp → app-download path without the real backend.

    POST .../cloud/environment/{environmentId}/activation      -> 200 (no body)
    POST .../cloud/environment/{environmentId}/entitlements    -> 200 (JSON)
    POST .../marketplace/virtual-entry/{virtualEntryId}/download -> 200 (bytes)

The JWT body is NOT verified here — this mock trusts every caller, which is fine
for local testing.

NOT YET CONSUMED: the operator's provisioning client
(internal/provisioning/client.go) is still an interface stub. Wiring this mock
in requires a concrete HTTP client that reads a base URL from configuration.
See ../README.md ("Mocking the provisioning server").

Usage:
    MAX_CLUSTER_NODES=3 PORT=8888 ./server.py
"""

import base64
import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

MAX_CLUSTER_NODES = int(os.environ.get("MAX_CLUSTER_NODES", "3"))
PORT = int(os.environ.get("PORT", "8888"))

# A placeholder license payload. The real API returns aggregated DXP + add-on
# license XML; the operator only needs opaque bytes to write to the Secret.
LICENSE_XML = (
    "<?xml version=\"1.0\"?>\n"
    "<license><type>developer</type>"
    "<maxClusterNodes>{}</maxClusterNodes></license>\n"
).format(MAX_CLUSTER_NODES)

# Set to a non-empty list to exercise the add-on download path, e.g.:
#   {"name": "Acme Connector",
#    "lpkgDownloadLink":
#      "http://localhost:8888/marketplace/virtual-entry/12345/download"}
APPS = []


class Handler(BaseHTTPRequestHandler):

    def _send(self, code, body=b"", content_type="application/json"):
        self.send_response(code)
        if body:
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        if body:
            self.wfile.write(body)

    def do_POST(self):
        path = self.path

        if path.endswith("/activation"):
            self.log_message("activation for %s", path)
            self._send(200)
            return

        if path.endswith("/entitlements"):
            self.log_message("entitlements for %s", path)
            body = json.dumps(
                {
                    "licenseXML": LICENSE_XML,
                    "maxClusterNodes": MAX_CLUSTER_NODES,
                    "apps": APPS,
                }
            ).encode()
            self._send(200, body)
            return

        if path.endswith("/download"):
            self.log_message("download for %s", path)
            lpkg = base64.b64decode("UEsDBBQAAAAA")  # tiny placeholder bytes
            self._send(200, lpkg, content_type="application/octet-stream")
            return

        self._send(404)


def main():
    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(
        "mock provisioning listening on :{} (maxClusterNodes={})".format(
            PORT, MAX_CLUSTER_NODES
        )
    )
    server.serve_forever()


if __name__ == "__main__":
    main()
