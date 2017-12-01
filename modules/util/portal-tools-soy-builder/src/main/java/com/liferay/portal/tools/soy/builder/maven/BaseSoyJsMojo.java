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

import com.liferay.portal.tools.soy.builder.commands.BaseSoyJsCommand;

import java.io.File;

/**
 * @author Andrea Di Giorgi
 * @author Gregory Amerson
 */
public abstract class BaseSoyJsMojo<T extends BaseSoyJsCommand>
	extends BaseSoyMojo<T> {

	public BaseSoyJsMojo() {
	}

	/**
	 * The directory containing the .soy.js files to process.
	 *
	 * @parameter
	 * @required
	 */
	public void setDir(File dir) {
		command.setDir(dir);
	}

}