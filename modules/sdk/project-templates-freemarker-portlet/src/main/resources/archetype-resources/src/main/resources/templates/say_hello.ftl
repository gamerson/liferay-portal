#set($symbol_pound = "#")
<${symbol_pound}include "init.ftl">
<${symbol_pound}assign userName = ParamUtil.getString(requet, "name", "") />

<@liferay_ui["message"] key="${artifactId}.hello" arguments=(userName) />