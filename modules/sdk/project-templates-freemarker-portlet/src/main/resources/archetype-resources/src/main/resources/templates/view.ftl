#set($symbol_pound = "#")
<${symbol_pound}include "init.ftl">

<@liferay_ui["message"] key="${artifactId}.caption" />

≈<@portlet["actionURL"] var="sayHelloUrl">
	<@portlet["param"] name="mvcPath" value="/say_hello.ftl" />
</@>

<${symbol_pound}assign userName = ParamUtil.getString(requet, "name", "") />

<@aui["form"] action=(sayHelloUrl) enctype="multipart/form-data" method="post" name="fm">
	<@aui["input"] name=(Constants.CMD) type="hidden" value="sayHello" />

	<@aui["layout"]>

	<@aui["fieldset-group"] markupView="lexicon">
		<@aui["fieldset"]>
			<@aui["input"] label="name.entry" name="userName" type="text" value=(userName) />
		</@>
	</@>

	<@aui["button-row"]>
		<@aui["button"] cssClass="btn-lg" type="submit" value="say-hello" />
	</@>
</@>
