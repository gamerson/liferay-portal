"""A small Liferay HTTP client for the end to end client extension checks.

Everything here drives the portal the way a browser does, because the flows
these scripts exercise have no headless equivalent: CompanyServiceImpl.addCompany
is annotated JSONWebServiceMode.IGNORE, and an OAuth2 authorization code is only
issued to an authenticated session.
"""

import base64
import hashlib
import json
import os
import re

import requests

LOGIN_NAMESPACE = "_com_liferay_login_web_portlet_LoginPortlet_"


class PortalError(Exception):
    pass


class Portal:
    """An authenticated session against one virtual instance."""

    def __init__(self, base_url, timeout=60):
        self.base_url = base_url.rstrip("/")
        self.session = requests.Session()
        self.session.headers["User-Agent"] = "Mozilla/5.0"
        self.timeout = timeout

    def application(self, external_reference_code):
        """Resolves an OAuth2 application by external reference code."""

        response = self.session.get(
            self.base_url + "/o/oauth2/application",
            params={"externalReferenceCode": external_reference_code},
            timeout=self.timeout,
        )

        if response.status_code != 200:
            raise PortalError(
                "no OAuth2 application %r on %s (HTTP %s). The client extension "
                "that declares it may not be deployed to this virtual instance."
                % (external_reference_code, self.base_url, response.status_code)
            )

        return response.json()

    def form(self, url, marker):
        """Fetches a page and returns (action, fields) for the form containing
        marker. Hidden inputs are preserved so the portal's auth token and
        redirect survive the round trip."""

        response = self.session.get(url, timeout=self.timeout)

        if response.status_code != 200:
            raise PortalError("GET %s returned HTTP %s" % (url, response.status_code))

        for element in re.findall(r"<form[^>]*>.*?</form>", response.text, re.S):
            if marker not in element:
                continue

            action = re.search(r'action="([^"]+)"', element)

            if action is None:
                raise PortalError("form containing %r has no action" % marker)

            return (
                action.group(1).replace("&amp;", "&"),
                dict(
                    re.findall(
                        r'<input[^>]*name="([^"]+)"[^>]*value="([^"]*)"', element
                    )
                ),
            )

        raise PortalError("no form containing %r at %s" % (marker, url))

    def sign_in(self, login, password):
        action, fields = self.form(
            self.base_url + "/c/portal/login", LOGIN_NAMESPACE + "password"
        )

        fields[LOGIN_NAMESPACE + "login"] = login
        fields[LOGIN_NAMESPACE + "password"] = password

        self.session.post(action, data=fields, timeout=self.timeout)

        # The portal answers an unauthenticated /o/oauth2/authorize with a login
        # redirect rather than an error, so a failed sign in would otherwise only
        # surface much later as a missing authorization code.
        if not self.signed_in():
            raise PortalError(
                "sign in as %r failed on %s" % (login, self.base_url)
            )

    def signed_in(self):
        """True when the session holds an authenticated user.

        Asking the login page is what distinguishes the two states cheaply: it
        renders the form for a guest and redirects an authenticated session away
        from it. The headless APIs are no use here -- they answer 403 to a
        request carrying only a session cookie.
        """

        response = self.session.get(
            self.base_url + "/c/portal/login", timeout=self.timeout
        )

        return LOGIN_NAMESPACE + "password" not in response.text

    def token(self, external_reference_code):
        """Runs the authorization code + PKCE exchange and returns the access
        token. This is the same flow the browser widgets use."""

        client_id = self.application(external_reference_code)["client_id"]
        redirect_uri = self.base_url + "/o/oauth2/redirect"

        verifier = base64.urlsafe_b64encode(os.urandom(40)).decode().rstrip("=")
        challenge = (
            base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest())
            .decode()
            .rstrip("=")
        )

        response = self.session.get(
            self.base_url + "/o/oauth2/authorize",
            allow_redirects=False,
            params={
                "client_id": client_id,
                "code_challenge": challenge,
                "code_challenge_method": "S256",
                "redirect_uri": redirect_uri,
                "response_type": "code",
            },
            timeout=self.timeout,
        )

        location = response.headers.get("Location", "")
        code = re.search(r"[?&]code=([^&]+)", location)

        if code is None:
            raise PortalError(
                "authorize returned HTTP %s with no code (Location: %s)"
                % (response.status_code, location[:200])
            )

        response = self.session.post(
            self.base_url + "/o/oauth2/token",
            data={
                "client_id": client_id,
                "code": code.group(1),
                "code_verifier": verifier,
                "grant_type": "authorization_code",
                "redirect_uri": redirect_uri,
            },
            timeout=self.timeout,
        )

        if response.status_code != 200:
            raise PortalError(
                "token exchange returned HTTP %s: %s"
                % (response.status_code, response.text[:200])
            )

        return client_id, response.json()["access_token"]


def claims(token):
    """Decodes a JWT payload without verifying it. For reporting only."""

    payload = token.split(".")[1]

    return json.loads(base64.urlsafe_b64decode(payload + "=" * (-len(payload) % 4)))
