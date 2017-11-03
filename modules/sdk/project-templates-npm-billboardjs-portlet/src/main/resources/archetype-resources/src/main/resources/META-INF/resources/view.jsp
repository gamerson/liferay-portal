<%@ include file="/init.jsp" %>
#parse ("definitions.vm")

<link href="<%= stylesheetURL %>" rel="stylesheet">

<div id="<portlet:namespace />"></div>

<aui:script require="<%= bootstrapRequire %>">
	${auiScriptRequireVarName}.default('<portlet:namespace />');
</aui:script>