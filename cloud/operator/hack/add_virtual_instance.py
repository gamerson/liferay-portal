"""Adds a Liferay virtual instance through the Portal Instances portlet.

There is no headless route to this. CompanyServiceImpl.addCompany carries
@JSONWebService(mode = JSONWebServiceMode.IGNORE), so /api/jsonws/company/add-company
answers 404, and no REST resource exposes company creation. Driving the portlet
form is the supported path that remains.
"""

import sys

from liferay_client import Portal, PortalError

PORTLET = "com_liferay_portal_instances_web_portlet_PortalInstancesPortlet"

NAMESPACE = "_" + PORTLET + "_"


def main():
    if len(sys.argv) != 6:
        print(
            "usage: add_virtual_instance.py <portal-url> <admin-login> "
            "<admin-password> <web-id> <new-admin-password>",
            file=sys.stderr,
        )

        return 2

    portal_url, login, password, web_id, admin_password = sys.argv[1:]

    portal = Portal(portal_url)
    portal.sign_in(login, password)

    # The web ID doubles as the virtual host and the mail domain so that the
    # instance is addressable by the same name the operator labels it with.
    action, fields = portal.form(
        portal.base_url + "/group/control_panel/manage?p_p_id=" + PORTLET +
        "&p_p_lifecycle=0&p_p_state=maximized&" + NAMESPACE +
        "mvcPath=%2Fadd_instance.jsp",
        NAMESPACE + "webId",
    )

    fields.update({
        NAMESPACE + "active": "true",
        NAMESPACE + "defaultAdminEmailAddress": "test@" + web_id,
        NAMESPACE + "defaultAdminFirstName": "Test",
        NAMESPACE + "defaultAdminLastName": "Test",
        NAMESPACE + "defaultAdminMiddleName": "",
        NAMESPACE + "defaultAdminPassword": admin_password,
        NAMESPACE + "defaultAdminScreenName": "test",
        NAMESPACE + "maxUsers": "0",
        NAMESPACE + "mx": web_id,
        NAMESPACE + "siteInitializerKey": "",
        NAMESPACE + "virtualHostname": web_id,
        NAMESPACE + "webId": web_id,
    })

    # Creating an instance takes minutes, and the portal redirects afterwards to
    # a control panel URL scoped to the new company, which answers 404 to this
    # session. The status is therefore not a useful signal -- the caller polls
    # for the company instead.
    portal.session.post(action, data=fields, timeout=1800)

    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except PortalError as error:
        print("  %s" % error, file=sys.stderr)

        sys.exit(1)
