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

package com.liferay.project.templates;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Andrea Di Giorgi
 * @author Gregory Amerson
 */
public class ProjectTemplatesArgs {

	public ProjectTemplatesArgs() {
		setArgument("author", System.getProperty("user.name"));
		setArgument("dependency-injector", "ds");
		setArgument("destination", System.getProperty("user.dir"));
		setArgument("framework-dependencies", "embedded");
		setArgument("liferay-version", "7.2");
		setArgument("template", "mvc-portlet");
	}

	public List<File> getArchetypesDirs() {
		return _archetypesDirs;
	}

	public String getArgument(String key) {
		return _arguments.get(key);
	}

	public Map<String, String> getArguments() {
		return _arguments;
	}

	public String getAuthor() {
		return getArgument("author");
	}

	public String getClassName() {
		return getArgument("class-name");
	}

	public String getContributorType() {
		return getArgument("contributor-type");
	}

	public String getDependencyInjector() {
		return getArgument("dependency-injector");
	}

	public File getDestinationDir() {
		return new File(getArgument("destination"));
	}

	public String getFramework() {
		return getArgument("framework");
	}

	public String getFrameworkDependencies() {
		return getArgument("framework-dependencies");
	}

	public String getGroupId() {
		return getArgument("group-id");
	}

	public String getHostBundleSymbolicName() {
		return getArgument("host-bundle-symbolic-name");
	}

	public String getHostBundleVersion() {
		return getArgument("host-bundle-version");
	}

	public String getLiferayVersion() {
		return getArgument("liferay-version");
	}

	public String getName() {
		return getArgument("name");
	}

	public String getOriginalModuleName() {
		return getArgument("original-module-name");
	}

	public String getOriginalModuleVersion() {
		return getArgument("original-module-version");
	}

	public String getPackageName() {
		return getArgument("package-name");
	}

	public String getService() {
		return getArgument("service");
	}

	public String getTemplate() {
		return getArgument("template");
	}

	public String getTemplateVersion() {
		return getArgument("template-version");
	}

	public String getViewType() {
		return getArgument("view-type");
	}

	public boolean isDependencyManagementEnabled() {
		return _dependencyManagementEnabled;
	}

	public boolean isForce() {
		return _force;
	}

	public boolean isGradle() {
		return _gradle;
	}

	public boolean isMaven() {
		return _maven;
	}

	public void setArchetypesDirs(List<File> archetypesDirs) {
		_archetypesDirs = archetypesDirs;
	}

	public void setArgument(String key, String value) {
		_arguments.put(key, value);
	}

	public void setArguments(Map<String, String> arguments) {
		_arguments = arguments;
	}

	@Parameter(
		description = "The name of the user associated with the code.",
		names = "--author"
	)
	public void setAuthor(String author) {
		setArgument("author", author);
	}

	@Parameter(
		description = "If a class is generated, provide the name of the class to be generated. If not provided, defaults to the project name.",
		names = "--class-name"
	)
	public void setClassName(String className) {
		setArgument("class-name", className);
	}

	@Parameter(
		description = "Used to identify your module as a Theme Contributor. Also, used to add the Liferay-Theme-Contributor-Type and Web-ContextPath bundle headers.",
		names = "--contributor-type"
	)
	public void setContributorType(String contributorType) {
		setArgument("contributor-type", contributorType);
	}

	@Parameter(
		description = "For Service Builder projects, specify the preferred dependency injection method (ds | spring). Default is DS",
		names = "--dependency-injector"
	)
	public void setDependencyInjector(String dependencyInjector) {
		setArgument("dependency-injector", dependencyInjector);
	}

	public void setDependencyManagementEnabled(
		boolean dependencyManagementEnabled) {

		_dependencyManagementEnabled = dependencyManagementEnabled;
	}

	@Parameter(
		description = "The directory where to create the new project.",
		names = "--destination"
	)
	public void setDestinationDir(File destinationDir) {
		setArgument("destination", destinationDir.getAbsolutePath());
	}

	public void setForce(boolean force) {
		_force = force;
	}

	@Parameter(
		description = "The name of the framework to use in the generated project.",
		names = "--framework"
	)
	public void setFramework(String framework) {
		setArgument("framework", framework);
	}

	@Parameter(
		description = "The way that the framework dependencies will be configured.",
		names = "--framework-dependencies"
	)
	public void setFrameworkDependencies(String frameworkDependencies) {
		setArgument("framework-dependencies", frameworkDependencies);
	}

	public void setGradle(boolean gradle) {
		_gradle = gradle;
	}

	@Parameter(
		description = "The group ID to use in the project.",
		names = "--group-id"
	)
	public void setGroupId(String groupId) {
		setArgument("group-id", groupId);
	}

	@Parameter(
		description = "If a new JSP hook fragment is generated, provide the name of the host bundle symbolic name.",
		names = "--host-bundle-symbolic-name"
	)
	public void setHostBundleSymbolicName(String hostBundleSymbolicName) {
		setArgument("host-bundle-symbolic-name", hostBundleSymbolicName);
	}

	@Parameter(
		description = "If a new JSP hook fragment is generated, provide the name of the host bundle version.",
		names = "--host-bundle-version"
	)
	public void setHostBundleVersion(String hostBundleVersion) {
		setArgument("host-bundle-version", hostBundleVersion);
	}

	@Parameter(
		description = "The version of Liferay to target when creating the project.",
		names = "--liferay-version"
	)
	public void setLiferayVersion(String liferayVersion) {
		setArgument("liferay-version", liferayVersion);
	}

	public void setMaven(boolean maven) {
		_maven = maven;
	}

	@Parameter(
		description = "The name of the new project.", names = "--name",
		required = true
	)
	public void setName(String name) {
		setArgument("name", name);
	}

	@Parameter(
		description = "Provide the name of the original module which you want to override.",
		names = "--original-module-name"
	)
	public void setOriginalModuleName(String originalModuleName) {
		setArgument("original-module-name", originalModuleName);
	}

	@Parameter(
		description = "The original module version.",
		names = "--original-module-version"
	)
	public void setOriginalModuleVersion(String originalModuleVersion) {
		setArgument("original-module-version", originalModuleVersion);
	}

	@Parameter(
		description = "The main package name to use in the project.",
		names = "--package-name"
	)
	public void setPackageName(String packageName) {
		setArgument("package-name", packageName);
	}

	@Parameter(
		description = "If a new DS component is generated, provide the name of the service to be implemented.",
		names = "--service"
	)
	public void setService(String service) {
		setArgument("service", service);
	}

	@Parameter(
		description = "The template to use when creating the project.",
		names = "--template"
	)
	public void setTemplate(String template) {
		setArgument("template", template);
	}

	@Parameter(hidden = true, names = "--template-version")
	public void setTemplateVersion(String templateVersion) {
		setArgument("template-version", templateVersion);
	}

	@Parameter(
		description = "Choose the view technology that will be used in the generated project.",
		names = "--view-type"
	)
	public void setViewType(String viewType) {
		setArgument("view-type", viewType);
	}

	protected boolean isHelp() {
		return _help;
	}

	protected boolean isList() {
		return _list;
	}

	@Parameter(hidden = true, names = {"--archetypes-dir", "--archetypes-dirs"})
	private List<File> _archetypesDirs = new ArrayList<>();

	@DynamicParameter(description = "Dynamic arguments", names = "-A")
	private Map<String, String> _arguments = new HashMap<>();

	@Parameter(
		description = "If workspace support target platform, no version number is required for the module.",
		names = "--dependency-management-enabled"
	)
	private boolean _dependencyManagementEnabled;

	@Parameter(
		description = "Forces creation of new project even if target directory contains files.",
		names = "--force"
	)
	private boolean _force;

	@Parameter(
		arity = 1,
		description = "Add the Gradle build script and the Gradle Wrapper to the new project.",
		names = "--gradle"
	)
	private boolean _gradle = true;

	@Parameter(
		description = "Print this message.", help = true,
		names = {"-h", "--help"}
	)
	private boolean _help;

	@Parameter(
		description = "Print the list of available project templates.",
		help = true, names = "--list"
	)
	private boolean _list;

	@Parameter(
		description = "Add the Maven POM file and the Maven Wrapper to the new project.",
		names = "--maven"
	)
	private boolean _maven;

}