<%@ include file="/init.jsp" %>

<p>
	<b><liferay-ui:message key="${artifactId}.caption"/></b>
</p>

<portlet:actionURL name="/say_hello" var="sayHelloUrl">
	<portlet:param name="mvcActionCommand" value="/say_hello" />
</portlet:actionURL>

<%
	String name = ParamUtil.getString(request, "userName", "");
%>

<aui:form action="<%= sayHelloUrl %>" method="post" name="fm">
	<aui:input name="<%= Constants.CMD %>" type="hidden" value="sayHello" />

	<aui:fieldset-group markupView="lexicon">
		<aui:fieldset>
			<aui:input label="name.entry" name="userName" type="text" value="<%= name %>" />
		</aui:fieldset>
	</aui:fieldset-group>

	<aui:button-row>
		<aui:button cssClass="btn-lg" type="submit" value="say-hello" />
	</aui:button-row>
</aui:form>
