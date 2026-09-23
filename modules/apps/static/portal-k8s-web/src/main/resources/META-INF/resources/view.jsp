<%--
/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<c:choose>
	<c:when test="<%= !clientExtensionStatusDisplayContext.isAvailable() %>">
		<div class="container-fluid container-fluid-max-xxl">
			<clay:alert
				displayType="info"
				message="this-portal-is-not-running-under-the-kubernetes-agent-so-no-client-extension-status-is-available"
			/>
		</div>
	</c:when>
	<c:otherwise>

		<%
		int failedCount = clientExtensionStatusDisplayContext.getFailedCount();
		%>

		<clay:management-toolbar
			managementToolbarDisplayContext="<%= new ClientExtensionStatusManagementToolbarDisplayContext(clientExtensionStatusDisplayContext, request, liferayPortletRequest, liferayPortletResponse) %>"
		/>

		<div class="container-fluid container-fluid-max-xxl">
			<c:if test="<%= failedCount > 0 %>">
				<clay:alert
					displayType="danger"
					message='<%= LanguageUtil.format(request, "x-of-x-client-extensions-did-not-complete-the-deployment-handshake", new Object[] {failedCount, clientExtensionStatusDisplayContext.getTotalCount()}, false) %>'
				/>
			</c:if>

			<liferay-ui:search-container
				id="clientExtensionStatuses"
				searchContainer="<%= clientExtensionStatusDisplayContext.getSearchContainer() %>"
			>
				<liferay-ui:search-container-row
					className="com.liferay.portal.k8s.agent.status.ClientExtensionStatus"
					keyProperty="serviceId"
					modelVar="clientExtensionStatus"
				>
					<liferay-ui:search-container-column-text
						name="service-id"
					>
						<portlet:renderURL var="viewClientExtensionURL">
							<portlet:param name="mvcPath" value="/view_client_extension.jsp" />
							<portlet:param name="serviceId" value="<%= clientExtensionStatus.getServiceId() %>" />
							<portlet:param name="virtualInstanceId" value="<%= clientExtensionStatus.getVirtualInstanceId() %>" />
						</portlet:renderURL>

						<aui:a href="<%= viewClientExtensionURL %>">
							<strong><%= HtmlUtil.escape(clientExtensionStatus.getServiceId()) %></strong>
						</aui:a>

						<c:if test="<%= Validator.isNotNull(clientExtensionStatus.getNamespace()) %>">
							<div class="text-secondary">
								<small><%= HtmlUtil.escape(clientExtensionStatus.getNamespace()) %></small>
							</div>
						</c:if>
					</liferay-ui:search-container-column-text>

					<liferay-ui:search-container-column-text
						name="virtual-instance"
						value="<%= HtmlUtil.escape(clientExtensionStatus.getVirtualInstanceId()) %>"
					/>

					<liferay-ui:search-container-column-text
						name="workload"
					>
						<c:choose>
							<c:when test="<%= Validator.isNull(clientExtensionStatus.getWorkloadKind()) %>">
								<span class="text-secondary"><liferay-ui:message key="configuration-only" /></span>
							</c:when>
							<c:otherwise>
								<%= HtmlUtil.escape(clientExtensionStatus.getWorkloadKind()) %>
							</c:otherwise>
						</c:choose>
					</liferay-ui:search-container-column-text>

					<liferay-ui:search-container-column-text
						name="handshake"
					>

						<%
						for (String conditionType : new String[] {ClientExtensionStatusCondition.DELIVERED, ClientExtensionStatusCondition.CONFIGURATION_ACCEPTED, ClientExtensionStatusCondition.PROVISIONED, ClientExtensionStatusCondition.READY}) {
							ClientExtensionStatusCondition condition = clientExtensionStatus.getCondition(conditionType);
						%>

							<span class="inline-item-before label label-<%= clientExtensionStatusDisplayContext.getConditionStyle(condition) %>">
								<span class="label-item label-item-expand"><%= conditionType %></span>
							</span>

						<%
						}
						%>

					</liferay-ui:search-container-column-text>

					<liferay-ui:search-container-column-text
						name="status"
					>
						<span class="label label-<%= clientExtensionStatusDisplayContext.getPhaseStyle(clientExtensionStatus.getPhase()) %>">
							<span class="label-item label-item-expand"><%= HtmlUtil.escape(clientExtensionStatus.getPhase()) %></span>
						</span>

						<c:if test="<%= clientExtensionStatus.isFailed() %>">
							<div class="text-danger">
								<small>
									<liferay-ui:message key="failed-at" />

									<%= HtmlUtil.escape(clientExtensionStatus.getFailedStage()) %>
								</small>
							</div>
						</c:if>
					</liferay-ui:search-container-column-text>
				</liferay-ui:search-container-row>

				<liferay-ui:search-iterator
					markupView="lexicon"
				/>
			</liferay-ui:search-container>
		</div>
	</c:otherwise>
</c:choose>
