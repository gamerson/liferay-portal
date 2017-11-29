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

import com.liferay.portal.tools.soy.builder.commands.TranspileJsCommand;

import java.util.List;

/**
 * Invoke metal CLI to transpile Soy to JS.
 *
 * @author Christopher Bryan Boyd
 * @goal transpile-js
 */
public class TranspileJsMojo extends BaseSoyNodeMojo<TranspileJsCommand> {

	/**
	 * The name of the final bundle file, for formats
	 * (like "globals") that create one.
	 *
	 * @parameter name="bundleFileName"
	 */
	public void setBundleFileName(String bundleFileName) {
		command.setBundleFileName(bundleFileName);
	}

	/**
	 * The directory where the compiled files will be
	 * stored. If multiple formats are given, multiple
	 * destinations can also be given, one for each
	 * format, in the same order.
	 *
	 * @parameter name="dest"
	 */
	public void setDest(String dest) {
		command.setDest(dest);
	}

	/**
	 * The format(s) that the source files will be built to.
	 *
	 * @parameter name="format"
	 */
	public void setFormat(String format) {
		command.setFormat(format);
	}

	/**
	 * Only used by the "globals" format build. The name
	 * of the global variable that will hold exported modules.
	 *
	 * @parameter name="globalName"
	 */
	public void setGlobalName(String globalName) {
		command.setGlobalName(globalName);
	}

	/**
	 * Only used by the "amd" format build. The name of
	 * the project that is being compiled. All built
	 * modules will be stored in a folder with this name.
	 *
	 * @parameter name="moduleName"
	 */
	public void setModuleName(String moduleName) {
		command.setModuleName(moduleName);
	}

	/**
	 * Soy files that the main source files depend on,
	 * but that shouldn't be compiled as well. The soy
	 * compiler needs these files.
	 *
	 * @parameter name="soyDeps"
	 */
	public void setSoyDeps(List<String> soyDeps) {
		command.setSoyDeps(soyDeps);
	}

	/**
	 * The directory where the compiled files will be stored.
	 *
	 * @parameter name="soyDest"
	 */
	public void setSoyDest(String soyDest) {
		command.setSoyDest(soyDest);
	}

	/**
	 * The path globs to the soy files to be compiled.
	 *
	 * @parameter name="soySrcs"
	 */
	public void setSoySrcs(List<String> soySrc) {
		command.setSoySrcs(soySrc);
	}

	/**
	 * The path globs to the js files to be compiled.
	 *
	 * @parameter name="srcs"
	 */
	public void setSrcs(List<String> src) {
		command.setSrcs(src);
	}

	@Override
	protected TranspileJsCommand createCommand() {
		return new TranspileJsCommand();
	}

}