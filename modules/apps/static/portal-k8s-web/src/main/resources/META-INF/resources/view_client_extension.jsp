<%--
/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<%
String serviceId = ParamUtil.getString(request, "serviceId");
String virtualInstanceId = ParamUtil.getString(request, "virtualInstanceId");

ClientExtensionStatus clientExtensionStatus = clientExtensionStatusDisplayContext.getClientExtensionStatus(serviceId, virtualInstanceId);

renderResponse.setTitle(serviceId);
%>

<clay:container-fluid>
	<c:choose>
		<c:when test="<%= clientExtensionStatus == null %>">
			<clay:alert
				displayType="warning"
				message="the-client-extension-could-not-be-found"
			/>
		</c:when>
		<c:otherwise>
			<clay:sheet>
				<clay:sheet-section>
					<h3 class="sheet-subtitle"><liferay-ui:message key="deployment-handshake" /></h3>

					<p class="text-secondary">
						<liferay-ui:message key="each-stage-must-succeed-in-order-the-first-failing-stage-is-where-to-look" />
					</p>

					<table class="table table-autofit table-list">
						<thead>
							<tr>
								<th><liferay-ui:message key="stage" /></th>
								<th><liferay-ui:message key="status" /></th>
								<th><liferay-ui:message key="reason" /></th>
								<th><liferay-ui:message key="message" /></th>
							</tr>
						</thead>
						<tbody>

							<%
							for (String conditionType : new String[] {ClientExtensionStatusCondition.DELIVERED, ClientExtensionStatusCondition.CONFIGURATION_ACCEPTED, ClientExtensionStatusCondition.PROVISIONED, ClientExtensionStatusCondition.READY}) {
								ClientExtensionStatusCondition condition = clientExtensionStatus.getCondition(conditionType);
							%>

								<tr>
									<td>
										<strong><%= conditionType %></strong>
									</td>
									<td>
										<span class="label label-<%= clientExtensionStatusDisplayContext.getConditionStyle(condition) %>">
											<span class="label-item label-item-expand">
												<c:choose>
													<c:when test="<%= condition == null %>">
														<liferay-ui:message key="unknown" />
													</c:when>
													<c:otherwise>
														<%= HtmlUtil.escape(condition.getStatus()) %>
													</c:otherwise>
												</c:choose>
											</span>
										</span>
									</td>
									<td>
										<%= (condition == null) ? "" : HtmlUtil.escape(condition.getReason()) %>
									</td>
									<td>
										<%= (condition == null) ? "" : HtmlUtil.escape(condition.getMessage()) %>
									</td>
								</tr>

							<%
							}
							%>

						</tbody>
					</table>
				</clay:sheet-section>

				<%
				List<ClientExtensionConfigurationError> configurationErrors = clientExtensionStatus.getConfigurationErrors();
				%>

				<c:if test="<%= !configurationErrors.isEmpty() %>">
					<clay:sheet-section>
						<h3 class="sheet-subtitle text-danger"><liferay-ui:message key="configuration-errors" /></h3>

						<p class="text-secondary">
							<liferay-ui:message key="these-entries-were-rejected-by-this-virtual-instance-while-the-payload-was-injected" />
						</p>

						<table class="table table-autofit table-list">
							<thead>
								<tr>
									<th><liferay-ui:message key="phase" /></th>
									<th><liferay-ui:message key="configuration" /></th>
									<th><liferay-ui:message key="message" /></th>
								</tr>
							</thead>
							<tbody>

								<%
								for (ClientExtensionConfigurationError configurationError : configurationErrors) {
								%>

									<tr>
										<td>
											<span class="label label-danger">
												<span class="label-item label-item-expand"><%= HtmlUtil.escape(configurationError.getPhase()) %></span>
											</span>
										</td>
										<td>
											<code><%= HtmlUtil.escape(configurationError.getPid()) %></code>
										</td>
										<td>
											<%= HtmlUtil.escape(configurationError.getMessage()) %>
										</td>
									</tr>

								<%
								}
								%>

							</tbody>
						</table>
					</clay:sheet-section>
				</c:if>

				<%
				List<String> configurationPids = clientExtensionStatus.getConfigurationPids();
				%>

				<c:if test="<%= !configurationPids.isEmpty() %>">
					<clay:sheet-section>
						<h3 class="sheet-subtitle"><liferay-ui:message key="applied-configurations" /></h3>

						<ul class="list-unstyled">

							<%
							for (String configurationPid : configurationPids) {
							%>

								<li><code><%= HtmlUtil.escape(configurationPid) %></code></li>

							<%
							}
							%>

						</ul>
					</clay:sheet-section>
				</c:if>

				<clay:sheet-section>
					<h3 class="sheet-subtitle"><liferay-ui:message key="details" /></h3>

					<table class="table table-autofit table-list">
						<tbody>
							<tr>
								<td><liferay-ui:message key="virtual-instance" /></td>
								<td><%= HtmlUtil.escape(clientExtensionStatus.getVirtualInstanceId()) %></td>
							</tr>
							<tr>
								<td><liferay-ui:message key="namespace" /></td>
								<td><%= HtmlUtil.escape(clientExtensionStatus.getNamespace()) %></td>
							</tr>
							<tr>
								<td><liferay-ui:message key="project-name" /></td>
								<td><%= HtmlUtil.escape(clientExtensionStatus.getProjectName()) %></td>
							</tr>
							<tr>
								<td><liferay-ui:message key="workload" /></td>
								<td>
									<%= HtmlUtil.escape(clientExtensionStatus.getWorkloadKind()) %>

									<c:if test="<%= Validator.isNotNull(clientExtensionStatus.getWorkloadName()) %>">
										<code><%= HtmlUtil.escape(clientExtensionStatus.getWorkloadName()) %></code>
									</c:if>
								</td>
							</tr>
						</tbody>
					</table>
				</clay:sheet-section>
			</clay:sheet>
		</c:otherwise>
	</c:choose>
</clay:container-fluid>
