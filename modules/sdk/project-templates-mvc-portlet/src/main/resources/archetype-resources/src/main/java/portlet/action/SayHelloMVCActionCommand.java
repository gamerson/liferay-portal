package ${package}.portlet.action;

import ${package}.constants.${className}PortletKeys;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.util.ParamUtil;
import org.osgi.service.component.annotations.Component;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

/**
 * class SayHelloMVCActionCommand: Handles the say hello action.
 *
 * @author ${author}
 */
@Component(
	immediate = true,
	property = {
		"javax.portlet.name=" + ${className}PortletKeys.${className},
		"mvc.command.name=/say_hello"
	},
	service = MVCActionCommand.class
)
public class SayHelloMVCActionCommand extends BaseMVCActionCommand {

	/**
	 * doProcessAction: Invoked when the /say_hello action is submitted.
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@Override
	protected void doProcessAction(
		ActionRequest request, ActionResponse response) throws Exception {

		String name = ParamUtil.getString(request, "userName", "");

		// fix the capitalization on the name
		name = capitalizeFully(name);

		// hide the success message.
		hideDefaultSuccessMessage(request);

		response.setRenderParameter("mvcPath", "/say_hello.jsp");
		response.setRenderParameter("name", name);
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
