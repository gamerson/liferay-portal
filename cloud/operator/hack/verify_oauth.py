"""Exercises the OAuth2 handshake a client extension depends on, end to end.

For each target this signs in, mints an access token through the authorization
code + PKCE flow, and calls the client extension's endpoint with it. A failure
here is the whole chain failing: the configuration payload reached Liferay, an
OAuth2 application came back, the operator mirrored the credentials, and the
workload read them at startup.
"""

import os
import sys

import requests

from liferay_client import Portal, PortalError, claims

FAILED = "FAIL"

PASSED = "ok"


def check(portal, target, verbose):
    """Runs one target, given as external-reference-code=url."""

    external_reference_code, _, url = target.partition("=")

    if not url:
        raise PortalError("target %r is not <erc>=<url>" % target)

    client_id, token = portal.token(external_reference_code)

    response = requests.get(
        url,
        headers={"Authorization": "Bearer " + token, "Origin": portal.base_url},
        timeout=60,
    )

    detail = response.text.strip().replace("\n", " ")[:56]

    if response.status_code != 200:
        detail = response.headers.get("www-authenticate", detail)

    print(
        "  %-9s %-52s HTTP %s  %s"
        % (
            PASSED if response.status_code == 200 else FAILED,
            url,
            response.status_code,
            detail,
        )
    )

    if verbose:
        payload = claims(token)

        print("            client_id %s" % client_id)
        print("            aud       %s" % payload.get("aud", "<absent>"))
        print("            scope     %s" % payload.get("scope", "<absent>"))

    return response.status_code == 200


def main():
    if len(sys.argv) < 5:
        print(
            "usage: verify_oauth.py <portal-url> <login> <password> "
            "<erc>=<url> [<erc>=<url> ...]",
            file=sys.stderr,
        )

        return 2

    portal_url, login, password = sys.argv[1:4]
    verbose = bool(os.environ.get("VERBOSE"))

    portal = Portal(portal_url)
    portal.sign_in(login, password)

    print("  signed in to %s as %s" % (portal_url, login))

    failures = 0

    for target in sys.argv[4:]:
        try:
            if not check(portal, target, verbose):
                failures += 1
        except PortalError as error:
            print("  %-9s %s" % (FAILED, error))

            failures += 1

    return 1 if failures else 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except PortalError as error:
        print("  %s" % error, file=sys.stderr)

        sys.exit(1)
