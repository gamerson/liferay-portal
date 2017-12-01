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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Christopher Bryan Boyd
 */
@Parameters(
	commandDescription = "Invoke metal CLI to transpile Soy to JS.",
	commandNames = "transpile-js"
)
public class TranspileJsCommand extends BaseNodeCommand {

	public TranspileJsCommand() {
	}

	@Override
	public void execute() throws Exception {
		Collection<String> arrayList = new ArrayList<>();

		arrayList.add(getNodePath());
		arrayList.add(getModulePath());
		arrayList.add("build");

		if (_bundleFileName.length() > 0) {
			arrayList.add("--bundleFileName");
			arrayList.add(_bundleFileName);
		}
		else {
			arrayList.add("--bundleFileName=");
		}

		if (_dest.length() > 0) {
			arrayList.add("--dest");
			arrayList.add(_dest);
		}

		if (_format.length() > 0) {
			arrayList.add("--format");
			arrayList.add(_format);
		}

		if (_globalName.length() > 0) {
			arrayList.add("--globalName");
			arrayList.add(_globalName);
		}
		else {
			arrayList.add("--globalName=");
		}

		if (_moduleName.length() > 0) {
			arrayList.add("--moduleName");
			arrayList.add(_moduleName);
		}
		else {
			arrayList.add("--moduleName=");
		}

		if (_soyDeps.size() > 0) {
			arrayList.add("--soyDeps");

			for (String string : _soyDeps) {
				arrayList.add(string);
			}
		}

		if (_soyDest.length() > 0) {
			arrayList.add("--soyDest");
			arrayList.add(_soyDest);
		}

		if (_soySrc.size() > 0) {
			arrayList.add("--soySrc");

			for (String string : _soySrc) {
				arrayList.add(string);
			}
		}

		if (_src.size() > 0) {
			arrayList.add("--src");

			for (String string : _src) {
				arrayList.add(string);
			}
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

	public String getBundleFileName() {
		return _bundleFileName;
	}

	public String getDest() {
		return _dest;
	}

	public String getFormat() {
		return _format;
	}

	public String getGlobalName() {
		return _globalName;
	}

	public String getModuleName() {
		return _moduleName;
	}

	public List<String> getSoyDeps() {
		return _soyDeps;
	}

	public String getSoyDest() {
		return _soyDest;
	}

	public List<String> getSoySrc() {
		return _soySrc;
	}

	public List<String> getSrc() {
		return _src;
	}

	public void setBundleFileName(String bundleFileName) {
		_bundleFileName = bundleFileName;
	}

	public void setDest(String dest) {
		_dest = dest;
	}

	public void setFormat(String format) {
		_format = format;
	}

	public void setGlobalName(String globalName) {
		_globalName = globalName;
	}

	public void setModuleName(String moduleName) {
		_moduleName = moduleName;
	}

	public void setSoyDeps(List<String> soyDeps) {
		_soyDeps = soyDeps;
	}

	public void setSoyDest(String soyDest) {
		_soyDest = soyDest;
	}

	public void setSoySrcs(List<String> soySrc) {
		_soySrc = soySrc;
	}

	public void setSrcs(List<String> src) {
		_src = src;
	}

	@Parameter(
		description = "The name of the final bundle file, for formats (like \"globals\") that create one.",
		names = {"-b", "--bundleFileName"}
	)
	private String _bundleFileName = "";

	@Parameter(
		description = "The directory where the compiled files will be stored. If multiple formats are given, multiple destinations can also be given, one for each format, in the same order.",
		names = {"-d", "--dest"}
	)
	private String _dest = "./";

	@Parameter(
		description = "The format(s) that the source files will be built to.",
		names = {"-f", "--format"}
	)
	private String _format = "amd";

	@Parameter(
		description = "Only used by the \"globals\" format build. The name of the global variable that will hold exported modules.",
		names = {"-g", "--globalName"}
	)
	private String _globalName = "";

	@Parameter(
		description = "Only used by the \"amd\" format build. The name of the project that is being compiled. " +
			"All built modules will be stored in a folder with this name.",
		names = {"-m", "--moduleName"}
	)
	private String _moduleName = "";

	@Parameter(
		description = "Soy files that the main source files depend on, but that shouldn't be compiled as well. The soy compiler needs these files.",
		names = {"--soyDeps"}
	)
	private List<String> _soyDeps = new ArrayList<>();

	@Parameter(
		description = "The directory where the compiled files will be stored.",
		names = {"--soyDest"}
	)
	private String _soyDest = "./";

	@Parameter(
		description = "The path globs to the soy files to be compiled.",
		names = {"--soySrc"}
	)
	private List<String> _soySrc = Arrays.asList("**/*.soy");

	@Parameter(
		description = "The path globs to the js files to be compiled.",
		names = {"--src"}
	)
	private List<String> _src = Arrays.asList("**/*.es.js", "*/*.soy.js");

}