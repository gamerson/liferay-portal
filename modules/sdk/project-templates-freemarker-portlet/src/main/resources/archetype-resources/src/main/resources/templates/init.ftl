<#assign aui = PortletJspTagLibs["/META-INF/liferay-aui.tld"] />
<#assign liferay_portlet = PortletJspTagLibs["/META-INF/liferay-portlet-ext.tld"] />
<#assign liferay_security = PortletJspTagLibs["/META-INF/liferay-security.tld"] />
<#assign liferay_theme = PortletJspTagLibs["/META-INF/liferay-theme.tld"] />
<#assign liferay_ui = PortletJspTagLibs["/META-INF/liferay-ui.tld"] />
<#assign liferay_util = PortletJspTagLibs["/META-INF/liferay-util.tld"] />
<#assign portlet = PortletJspTagLibs["/META-INF/liferay-portlet.tld"] />

<#assign ParamUtil = staticUtil["com.liferay.portal.kernel.util.ParamUtil"] />
<#assign Constants = staticUtil["com.liferay.portal.kernel.util.Constants"] />

<@liferay_theme["defineObjects"] />

<@portlet["defineObjects"] />