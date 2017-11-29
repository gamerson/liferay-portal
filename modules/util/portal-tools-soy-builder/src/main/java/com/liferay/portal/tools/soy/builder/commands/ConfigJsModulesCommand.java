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

package com.liferay.portal.tools.soy.builder.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Christopher Bryan Boyd
 */
@Parameters(
	commandDescription = "Mojo wrapper for Liferay AMD Module Config Generator.",
	commandNames = "config-js-modules"
)
public class ConfigJsModulesCommand extends BaseNodeCommand {

	public ConfigJsModulesCommand() {
	}

	@Override
	public void execute() throws Exception {
		Collection<String> arrayList = new ArrayList<>();

		arrayList.add(getNodePath());
		arrayList.add(getModulePath());

		if (_config.length() > 0) {
			arrayList.add("--config");
			arrayList.add(_config);
		}
		else {
			arrayList.add("--config=");
		}

		if (_extension.length() > 0) {
			arrayList.add("--extension");
			arrayList.add(_extension);
		}
		else {
			arrayList.add("--extension=");
		}

		if (_filePattern.length() > 0) {
			arrayList.add("--filePattern");
			arrayList.add(_filePattern);
		}
		else {
			arrayList.add("--filePattern=");
		}

		if (_format.length() > 0) {
			arrayList.add("--format");
			arrayList.add(_format);
		}
		else {
			arrayList.add("--format=");
		}

		arrayList.add("--ignorePath");

		if (_ignorePath) {
			arrayList.add("true");
		}
		else {
			arrayList.add("false");
		}

		if (_moduleConfig.length() > 0) {
			arrayList.add("--moduleConfig");
			arrayList.add(_moduleConfig);
		}
		else {
			arrayList.add("--moduleConfig=");
		}

		if (_namespace.length() > 0) {
			arrayList.add("--namespace");
			arrayList.add(_namespace);
		}
		else {
			arrayList.add("--namespace=");
		}

		if (_output.length() > 0) {
			arrayList.add("--output");
			arrayList.add(_output);
		}
		else {
			arrayList.add("--output=");
		}

		if (_moduleRoot.length() > 0) {
			arrayList.add("--moduleRoot");
			arrayList.add(_moduleRoot);
		}
		else {
			arrayList.add("--moduleRoot");
			arrayList.add("./");
		}

		if (_input.length() > 0) {
			Path inputPath = Paths.get(_input).toAbsolutePath();

			arrayList.add(inputPath.toString());
		}

		StringBuilder stringBuilder = new StringBuilder();

		for (String string : arrayList) {
			stringBuilder.append(string + " ");
		}

		String string = stringBuilder.toString();

		string = string.trim();

		final int returnCode = executeCommand(string, getWorkingPath());

		if (returnCode != 0) {
			throw new Exception("Abormal return code " + returnCode);
		}
	}

	public String getBase() {
		return _base;
	}

	public String getConfig() {
		return _config;
	}

	public String getExtension() {
		return _extension;
	}

	public String getFilePattern() {
		return _filePattern;
	}

	public String getFormat() {
		return _format;
	}

	public String getInput() {
		return _input;
	}

	public String getModuleConfig() {
		return _moduleConfig;
	}

	public String getModuleRoot() {
		return _moduleRoot;
	}

	public String getNamespace() {
		return _namespace;
	}

	public String getOutput() {
		return _output;
	}

	public boolean isIgnorePath() {
		return _ignorePath;
	}

	public void setBase(String base) {
		_base = base;
	}

	public void setConfig(String config) {
		_config = config;
	}

	public void setExtension(String extension) {
		_extension = extension;
	}

	public void setFilePattern(String filePattern) {
		_filePattern = filePattern;
	}

	public void setFormat(String format) {
		_format = format;
	}

	public void setIgnorePath(boolean ignorePath) {
		_ignorePath = ignorePath;
	}

	public void setInput(String input) {
		_input = input;
	}

	public void setModuleConfig(String moduleConfig) {
		_moduleConfig = moduleConfig;
	}

	public void setModuleRoot(String moduleRoot) {
		_moduleRoot = moduleRoot;
	}

	public void setNamespace(String namespace) {
		_namespace = namespace;
	}

	public void setOutput(String output) {
		_output = output;
	}

	@Parameter(
		description = "Already existing template to be used as base for the parsed configuration.",
		names = {"-b", "--base"}
	)
	private String _base;

	@Parameter(
		description = "The configuration variable to which the modules should be added.",
		names = {"-c", "--config"}
	)
	private String _config = "";

	@Parameter(
		description = "Use the provided string as an extension instead to get it automatically from the file name.",
		names = {"-e", "--extension"}
	)
	private String _extension = "";

	@Parameter(
		description = "The pattern to be used in order to find files for processing.",
		names = {"-p", "--filePattern"}
	)
	private String _filePattern = "**/resources/*.js";

	@Parameter(
		description = "Regex and value which will be applied to the file name when generating the module name.",
		names = {"-f", "--format"}
	)
	private String _format = "/_/g,-";

	@Parameter(
		description = "Do not create module path and fullPath properties.",
		names = {"-i", "--ignorePath"}
	)
	private boolean _ignorePath = true;

	@Parameter(
		description = "The path to the js files to be configured.",
		names = {"--input"}, required = true
	)
	private String _input;

	@Parameter(
		description = "JSON file which contains configuration data for the modules, for example module prefix.",
		names = {"-m", "--moduleConfig"}
	)
	private String _moduleConfig;

	@Parameter(
		description = "The folder which will be used as starting point from which the module name should be generated.",
		names = {"-r", "--moduleRoot"}
	)
	private String _moduleRoot = "./";

	@Parameter(
		description = "The namespace that should be used for \"define\" and \"require\" calls.",
		names = {"-n", "--namespace"}
	)
	private String _namespace = "Liferay.Loader";

	@Parameter(
		description = "Output file to store the generated configuration.",
		names = {"-o", "--output"}
	)
	private String _output = "../config.json";

}