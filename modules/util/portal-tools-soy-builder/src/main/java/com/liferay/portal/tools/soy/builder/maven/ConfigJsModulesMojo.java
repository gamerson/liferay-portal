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

package com.liferay.portal.tools.soy.builder.maven;

import com.liferay.portal.tools.soy.builder.commands.ConfigJsModulesCommand;

/**
 * Mojo wrapper for Liferay AMD Module Config Generator.
 *
 * @author Christopher Bryan Boyd
 * @goal config-js-modules
 */
public class ConfigJsModulesMojo
	extends BaseSoyNodeMojo<ConfigJsModulesCommand> {

	/**
	 * The configuration variable to which the modules should be added.
	 *
	 * @parameter name="config" default-value=""
	 */
	public void setConfig(String config) {
		command.setConfig(config);
	}

	/**
	 * Use the provided string as an extension instead to get it automatically from the file name.
	 *
	 * @parameter name="extension" default-value=""
	 */
	public void setExtension(String extension) {
		command.setExtension(extension);
	}

	/**
	 * The pattern to be used in order to find files for processing.
	 *
	 * @parameter name="filePattern" default-value="**\/*.js"
	 */
	public void setFilePattern(String filePattern) {
		command.setFilePattern(filePattern);
	}

	/**
	 * Regex and value which will be applied to the file name when generating the module name.
	 *
	 * @parameter name="format" default-value=""
	 */
	public void setFormat(String format) {
		command.setFormat(format);
	}

	/**
	 * Do not create module path and fullPath properties.
	 *
	 * @parameter name="ignorePath" default-value="false"
	 */
	public void setIgnorePath(boolean ignorePath) {
		command.setIgnorePath(ignorePath);
	}

	/**
	 * The path to the js files to be configured.
	 *
	 * @parameter name="input"
	 */
	public void setInput(String input) {
		command.setInput(input);
	}

	/**
	 * JSON file which contains configuration data for the modules, for example module prefix.
	 *
	 * @parameter name="moduleConfig"
	 * @required
	 */
	public void setModuleConfig(String moduleConfig) {
		command.setModuleConfig(moduleConfig);
	}

	/**
	 * The folder which will be used as starting point from which the module name should be generated.
	 *
	 * @parameter name="moduleRoot"
	 * @required
	 */
	public void setModuleRoot(String moduleRoot) {
		command.setModuleRoot(moduleRoot);
	}

	/**
	 * The namespace that should be used for \"define\" and \"require\" calls.
	 *
	 * @parameter name="namespace" default-value=""
	 */
	public void setNamespace(String namespace) {
		command.setNamespace(namespace);
	}

	/**
	 * Output file to store the generated configuration.
	 *
	 * @parameter name="output"
	 * @required
	 */
	public void setOutput(String output) {
		command.setOutput(output);
	}

	@Override
	protected ConfigJsModulesCommand createCommand() {
		return new ConfigJsModulesCommand();
	}

}