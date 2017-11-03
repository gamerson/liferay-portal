<%@ include file="/init.jsp" %>
#parse ("definitions.vm")

<div id="${artifactId}-root"></div>

<aui:script require="<%= bootstrapRequire %>">
	${auiScriptRequireVarName}.default();
</aui:script>