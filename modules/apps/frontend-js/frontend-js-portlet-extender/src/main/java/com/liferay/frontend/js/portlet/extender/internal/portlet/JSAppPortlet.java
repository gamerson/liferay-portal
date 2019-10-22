/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.frontend.js.portlet.extender.internal.portlet;

import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.model.EventDefinition;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.portal.kernel.model.PortletApp;
import com.liferay.portal.kernel.model.PortletFilter;
import com.liferay.portal.kernel.model.PortletURLListener;
import com.liferay.portal.kernel.model.PublicRenderParameter;
import com.liferay.portal.kernel.model.SpriteImage;
import com.liferay.portal.kernel.xml.QName;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * @author Gregory Amerson
 */
@SuppressWarnings("serial")
public class JSAppPortlet extends JSPortlet implements PortletApp {

	public JSAppPortlet(
		JSONFactory jsonFactory, String packageName, String packageVersion,
		Set<String> portletPreferencesFieldNames, PortletApp portletApp,
		Servlet servlet) {

		super(
			jsonFactory, packageName, packageVersion,
			portletPreferencesFieldNames);

		_portletApp = portletApp;
		_servlet = servlet;
	}

	public void addEventDefinition(EventDefinition eventDefinition) {
		_portletApp.addEventDefinition(eventDefinition);
	}

	public void addPortlet(Portlet portlet) {
		_portletApp.addPortlet(portlet);
	}

	public void addPortletFilter(PortletFilter portletFilter) {
		_portletApp.addPortletFilter(portletFilter);
	}

	public void addPortletURLListener(PortletURLListener portletURLListener) {
		_portletApp.addPortletURLListener(portletURLListener);
	}

	public void addPublicRenderParameter(
		PublicRenderParameter publicRenderParameter) {

		_portletApp.addPublicRenderParameter(publicRenderParameter);
	}

	public void addPublicRenderParameter(String identifier, QName qName) {
		_portletApp.addPublicRenderParameter(identifier, qName);
	}

	public void addServletURLPatterns(Set<String> servletURLPatterns) {
		_portletApp.addServletURLPatterns(servletURLPatterns);
	}

	public Map<String, String[]> getContainerRuntimeOptions() {
		return _portletApp.getContainerRuntimeOptions();
	}

	@Override
	public String getContextPath() {
		ServletContext servletContext = getServletContext();

		return servletContext.getContextPath();
	}

	public Map<String, String> getCustomUserAttributes() {
		return _portletApp.getCustomUserAttributes();
	}

	public String getDefaultNamespace() {
		return _portletApp.getDefaultNamespace();
	}

	public Set<EventDefinition> getEventDefinitions() {
		return _portletApp.getEventDefinitions();
	}

	public PortletFilter getPortletFilter(String filterName) {
		return _portletApp.getPortletFilter(filterName);
	}

	public Set<PortletFilter> getPortletFilters() {
		return _portletApp.getPortletFilters();
	}

	public List<Portlet> getPortlets() {
		return _portletApp.getPortlets();
	}

	public PortletURLListener getPortletURLListener(String listenerClass) {
		return _portletApp.getPortletURLListener(listenerClass);
	}

	public Set<PortletURLListener> getPortletURLListeners() {
		return _portletApp.getPortletURLListeners();
	}

	public PublicRenderParameter getPublicRenderParameter(String identifier) {
		return _portletApp.getPublicRenderParameter(identifier);
	}

	public ServletContext getServletContext() {
		ServletConfig servletConfig = _servlet.getServletConfig();

		return servletConfig.getServletContext();
	}

	public String getServletContextName() {
		ServletContext servletContext = getServletContext();

		return servletContext.getServletContextName();
	}

	public Set<String> getServletURLPatterns() {
		return _portletApp.getServletURLPatterns();
	}

	public int getSpecMajorVersion() {
		return _portletApp.getSpecMajorVersion();
	}

	public int getSpecMinorVersion() {
		return _portletApp.getSpecMinorVersion();
	}

	public SpriteImage getSpriteImage(String fileName) {
		return _portletApp.getSpriteImage(fileName);
	}

	public Set<String> getUserAttributes() {
		return _portletApp.getUserAttributes();
	}

	public boolean isWARFile() {
		return true;
	}

	public void removePortlet(Portlet portletModel) {
	}

	public void setDefaultNamespace(String defaultNamespace) {
	}

	public void setServletContext(ServletContext servletContext) {
	}

	public void setSpecMajorVersion(int specMajorVersion) {
	}

	public void setSpecMinorVersion(int specMinorVersion) {
	}

	public void setSpriteImages(String spriteFileName, Properties properties) {
	}

	public void setWARFile(boolean warFile) {
	}

	private final PortletApp _portletApp;
	private final Servlet _servlet;

}