package ${package}.portlet;

import ${package}.constants.${className}PortletKeys;

import com.liferay.util.bridges.freemarker.FreeMarkerPortlet;

import javax.portlet.Portlet;

import org.osgi.service.component.annotations.Component;

/**
 * @author ${author}
 */
@Component(
	immediate = true,
	property = {
		"com.liferay.portlet.css-class-wrapper=portlet-freemarker",
		"com.liferay.portlet.display-category=category.sample",
		"com.liferay.portlet.header-portlet-css=/css/main.css",
		"com.liferay.portlet.instanceable=true",
		"javax.portlet.display-name=${artifactId} Portlet",
		"javax.portlet.init-param.template-path=/",
		"javax.portlet.init-param.view-template=/templates/view.ftl",
		"javax.portlet.name=" + ${className}PortletKeys.${className},
		"javax.portlet.resource-bundle=content.Language",
		"javax.portlet.security-role-ref=power-user,user"
	},
	service = Portlet.class
)
public class ${className}Portlet extends FreeMarkerPortlet {

	@Override
	public void processAction(
		ActionRequest actionRequest, ActionResponse actionResponse)
		throws IOException, PortletException {

		try {
			String cmd = ParamUtil.getString(actionRequest, Constants.CMD);

			if (cmd.equals("sayHello")) {
				String name = ParamUtil.getString(actionRequest, "name", "");

				name = capitalizeFully(name);
			}

			sendRedirect(actionRequest, actionResponse);
		}
		catch (Exception e) {
			if ((e instanceof OSGiException) ||
			(e instanceof PrincipalException)) {

				SessionErrors.add(actionRequest, e.getClass().getName());
			}
		}
	}


	/**
	 * capitalizeFully: Capitalizes first letter of all words in given string.
	 * @param str String to capitalize.
	 * @return String The fully capitalized string.
	 */
	public String capitalizeFully(String str) {
		if (str == null || str.length() == 0) {
			return str;
		}
		int strLen = str.length();
		str = str.toLowerCase();
		StringBuffer buffer = new StringBuffer(strLen);
		boolean capitalizeNext = true;
		for (int i = 0; i < strLen; i++) {
			char ch = str.charAt(i);

			if (Character.isWhitespace(ch)) {
				buffer.append(ch);
				capitalizeNext = true;
			} else if (capitalizeNext) {
				buffer.append(Character.toTitleCase(ch));
				capitalizeNext = false;
			} else {
				buffer.append(ch);
			}
		}
		return buffer.toString();
	}
}