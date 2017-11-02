<%@ include file="/init.jsp" %>
#parse ("definitions.vm")

<div id="<portlet:namespace />">{{text}}</div>

<aui:script require="<%= bootstrapRequire %>">
	${auiScriptRequireVarName}.default('<portlet:namespace />');
</aui:script>