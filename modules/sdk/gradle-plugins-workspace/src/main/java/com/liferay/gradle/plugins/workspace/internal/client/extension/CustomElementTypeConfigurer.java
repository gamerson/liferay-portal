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

package com.liferay.gradle.plugins.workspace.internal.client.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.liferay.gradle.plugins.node.NodeExtension;
import com.liferay.gradle.plugins.node.NodePlugin;
import com.liferay.gradle.plugins.workspace.configurator.ClientExtensionProjectConfigurator;
import com.liferay.gradle.plugins.workspace.internal.util.GradleUtil;
import com.liferay.gradle.plugins.workspace.task.CreateClientExtensionConfigTask;

import groovy.json.JsonSlurper;

import groovy.lang.Closure;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;

import org.osgi.framework.Version;
import org.yaml.snakeyaml.Yaml;

/**
 * @author Gregory Amerson
 */
public class CustomElementTypeConfigurer
	implements ClientExtensionTypeConfigurer {
	private static final String _CLIENT_EXTENSION_YAML =
			"client-extension.yaml";
	@Override
	public void apply(
		Project project, ClientExtension clientExtension,
		TaskProvider<Zip> zipTaskProvider) {

		if (!_hasFrontendBuild(project)) {
			return;
		}

		GradleUtil.applyPlugin(project, NodePlugin.class);

		_configureNodeAndNpmVersion(project);

		TaskProvider<CreateClientExtensionConfigTask>
			createClientExtensionConfigTaskProvider =
				GradleUtil.getTaskProvider(
					project,
					ClientExtensionProjectConfigurator.
						CREATE_CLIENT_EXTENSION_CONFIG_TASK_NAME,
					CreateClientExtensionConfigTask.class);

		createClientExtensionConfigTaskProvider.configure(
			createClientExtensionConfigTask ->
				createClientExtensionConfigTask.dependsOn(
					NodePlugin.PACKAGE_RUN_BUILD_TASK_NAME));

		zipTaskProvider.configure(
			new Action<Zip>() {

				@Override
				@SuppressWarnings("serial")
				public void execute(Zip zip) {
					zip.into(
						new Callable<String>() {

							@Override
							public String call() throws Exception {
								return "static";
							}

						},
						new Closure<Void>(zip) {

							@SuppressWarnings("unused")
							public void doCall(CopySpec copySpec) {
								copySpec.from(project.getBuildDir());
								copySpec.include("static/**/*");
								copySpec.into("static");
							}

						});
				}

			});
	}

	private void _configureNodeAndNpmVersion(Project project) {
		NodeExtension nodeExtension = GradleUtil.getExtension(
			project, NodeExtension.class);

		String nodeVersion = nodeExtension.getNodeVersion();

		try {
			Version version = Version.parseVersion(nodeVersion);

			if (version.compareTo(_MINIMUM_NODE_VERSION) < 0) {
				nodeVersion = _MINIMUM_NODE_VERSION.toString();

				nodeExtension.setNodeVersion(nodeVersion);
			}
		}
		catch (Exception exception) {
			throw new GradleException(
				"Unable to parse node version", exception);
		}

		String npmVersion = nodeExtension.getNpmVersion();

		try {
			Version version = Version.parseVersion(nodeVersion);

			if (version.compareTo(_MINIMUM_NPM_VERSION) < 0) {
				npmVersion = _MINIMUM_NPM_VERSION.toString();

				nodeExtension.setNpmVersion(npmVersion);
			}
		}
		catch (Exception exception) {
			throw new GradleException("Unable to parse npm version", exception);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> _getPackageJsonMap(File packageJsonFile) {
		if (!packageJsonFile.exists()) {
			return Collections.emptyMap();
		}

		JsonSlurper jsonSlurper = new JsonSlurper();

		return (Map<String, Object>)jsonSlurper.parse(packageJsonFile);
	}

	@SuppressWarnings("unchecked")
	private boolean _hasBuildScript(Path packageJsonPath) {
		Map<String, Object> packageJsonMap = _getPackageJsonMap(
			packageJsonPath.toFile());

		Map<String, Object> liferayTheme =
			(Map<String, Object>)packageJsonMap.get("liferayTheme");
		Map<String, Object> scripts = (Map<String, Object>)packageJsonMap.get(
			"scripts");

		if ((liferayTheme == null) && (scripts != null) &&
			(scripts.get("build") != null)) {

			return true;
		}

		return false;
	}

	private boolean _hasFrontendBuild(Project project) {
		File packageJsonFile = project.file("package.json");

		if (packageJsonFile.exists() &&
			_hasBuildScript(packageJsonFile.toPath())) {

			return true;
		}

		return false;
	}

	private static final Version _MINIMUM_NODE_VERSION = Version.parseVersion(
		"10.15.3");

	private static final Version _MINIMUM_NPM_VERSION = Version.parseVersion(
		"6.4.1");

}