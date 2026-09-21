<%--
/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<%
LicenseActivationAgent.LiferayEnvironment liferayEnvironment = (LicenseActivationAgent.LiferayEnvironment)request.getAttribute("liferayEnvironment");

boolean activationTabVisible = liferayEnvironment != null;

String requestedTab = ParamUtil.getString(renderRequest, "tab", "licenses");

String tab = (activationTabVisible && requestedTab.equals("activation")) ? "activation" : "licenses";
%>

<portlet:renderURL var="licensesTabURL">
	<portlet:param name="tab" value="licenses" />
</portlet:renderURL>

<portlet:renderURL var="activationTabURL">
	<portlet:param name="tab" value="activation" />
</portlet:renderURL>

<%
String activationHref = activationTabURL;
String licensesHref = licensesTabURL;
%>

<clay:navigation-bar
	navigationItems='<%=
		new JSPNavigationItemList(pageContext) {
			{
				add(
					navigationItem -> {
						navigationItem.setActive(tab.equals("licenses"));
						navigationItem.setHref(licensesHref);
						navigationItem.setLabel(LanguageUtil.get(httpServletRequest, "licenses"));
					});

				if (activationTabVisible) {
					add(
						navigationItem -> {
							navigationItem.setActive(tab.equals("activation"));
							navigationItem.setHref(activationHref);
							navigationItem.setLabel("Activation");
						});
				}
			}
		}
	%>'
/>

<c:choose>
	<c:when test='<%= tab.equals("activation") %>'>
		<%@ include file="/activation.jspf" %>
	</c:when>
	<c:otherwise>
		<clay:container-fluid
			cssClass="container-form-lg"
		>
			<clay:sheet
				size="fluid"
			>
				<iframe allowTransparency="true" class="border-0 w-100" frameborder="0" id="<portlet:namespace />iframe" scrolling="no" src="<%= themeDisplay.getPathMain() %>/portal/license?p_l_id=<%= themeDisplay.getPlid() %>&p_p_state=pop_up"></iframe>
			</clay:sheet>
		</clay:container-fluid>

		<aui:script use="aui-autosize-iframe">
			var iframe = A.one('#<portlet:namespace />iframe');

			if (iframe) {
				iframe.plug(A.Plugin.AutosizeIframe);
			}
		</aui:script>
	</c:otherwise>
</c:choose>