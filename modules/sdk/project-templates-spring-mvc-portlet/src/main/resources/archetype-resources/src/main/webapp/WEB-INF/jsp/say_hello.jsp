
<%@ include file="/init.jsp" %>

<%
	String name = ParamUtil.getString(request, "name", "");
%>

<p>
	<b><liferay-ui:message key="${artifactId}.hello" arguments="<%= name %>" /></b>
</p>

