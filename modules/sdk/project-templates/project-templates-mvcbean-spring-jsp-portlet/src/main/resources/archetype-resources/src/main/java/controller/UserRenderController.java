package ${package}.controller;

import ${package}.constants.${className}PortletKeys;

import ${package}.dto.User;

import com.liferay.bean.portlet.LiferayPortletConfiguration;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import javax.mvc.Controller;
import javax.mvc.Models;
import javax.mvc.engine.ViewEngineContext;

import javax.portlet.ActionRequest;
import javax.portlet.ActionURL;
import javax.portlet.MutableActionParameters;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.annotations.PortletConfiguration;
import javax.portlet.annotations.RenderMethod;

import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ${author}
 */
@ApplicationScoped
@Controller
@LiferayPortletConfiguration(
	portletName = ${className}PortletKeys.${className},
	properties = {
		"com.liferay.portlet.display-category=category.sample",
		"com.liferay.portlet.header-portlet-css=/css/main.css",
		"com.liferay.portlet.icon=/icon.png"
	}
)
@PortletConfiguration(
	portletName = ${className}PortletKeys.${className},
	resourceBundle = "content.Language"
)
public class UserRenderController {

	private static final Logger logger = LoggerFactory.getLogger(UserRenderController.class);

	@Inject
	private Models models;

	@Inject
	private ViewEngineContext viewEngineContext;

	@RenderMethod(portletNames = {${className}PortletKeys.${className}})
	@ValidateOnExecution(type = ExecutableType.NONE)
	public String prepareView(RenderRequest renderRequest, RenderResponse renderResponse) {

		String viewName = viewEngineContext.getView();

		if (viewName == null) {

			viewName = "user.jspx";

			User user = (User) models.get("user");

			if (user == null) {
				user = new User();
				models.put("user", user);
			}

			ActionURL actionURL = renderResponse.createActionURL();
			MutableActionParameters actionParameters = actionURL.getActionParameters();
			actionParameters.setValue(ActionRequest.ACTION_NAME, "submitUser");

			models.put("submitUserActionURL", actionURL.toString());
		}

		logger.debug("[RENDER_PHASE] prepared model for viewName: {}", viewName);

		return viewName;
	}
}