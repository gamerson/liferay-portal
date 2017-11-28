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

import com.liferay.portal.tools.soy.builder.commands.BaseNodeCommand;

/**
 * @author Christopher Bryan Boyd
 */
public abstract class BaseSoyNodeMojo<T extends BaseNodeCommand>
	extends BaseSoyMojo<T> {

	public BaseSoyNodeMojo() {
	}

	/**
	 * The path to the node module to be executed.
	 *
	 * @parameter name="modulePath"
	 * @required
	 */
	public void setModulePath(String modulePath) {
		command.setModulePath(modulePath);
	}

	/**
	 * The path to the node executable.
	 *
	 * @parameter name="nodePath"
	 * @required
	 */
	public void setNodePath(String nodePath) {
		command.setNodePath(nodePath);
	}

	/**
	 * The working path (directory) of the process to be executed..
	 *
	 * @parameter name="workingPath"
	 * @required
	 */
	public void setWorkingPath(String workingPath) {
		command.setWorkingPath(workingPath);
	}

}