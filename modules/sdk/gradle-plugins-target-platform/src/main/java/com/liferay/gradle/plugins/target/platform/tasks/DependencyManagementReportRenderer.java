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

package com.liferay.gradle.plugins.target.platform.tasks;

import java.io.PrintWriter;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

/**
 * @author Simon Jiang
 */
public class DependencyManagementReportRenderer {

	public DependencyManagementReportRenderer() {
		this(new PrintWriter(System.out));
	}

	public DependencyManagementReportRenderer(PrintWriter writer) {
		_output = writer;
	}

	public void renderConfigurationManagedVersions(
		Map<String, String> managedVersions,
		final Configuration configuration) {

		_renderDependencyManagementHeader(
			configuration.getName(),
			"Dependency management for the " + configuration.getName() +
				" configuration");

		if ((managedVersions != null) && !managedVersions.isEmpty()) {
			_renderManagedVersions(managedVersions);
		}
		else {
			_output.println("No dependency management");
			_output.println();
		}

		_output.flush();
	}

	public void startProject(final Project project) {
		_output.println();
		_output.println(
			"------------------------------------------------------------");
		String heading;

		if (project.equals(project.getRootProject())) {
			heading = "Root project";
		}
		else {
			heading = "Project " + project.getPath();
		}

		if (project.getDescription() != null) {
			heading += " - " + project.getDescription();
		}

		_output.println(heading);
		_output.println(
			"------------------------------------------------------------");

		_output.println();
	}

	private void _renderDependencyManagementHeader(
		String identifier, String description) {

		_output.println(identifier + " - " + description);
	}

	private void _renderManagedVersions(Map<String, String> managedVersions) {
		Map<String, String> sortedVersions = new TreeMap<String, String>(
			new Comparator<String>() {

				@Override
				public int compare(String one, String two) {
					String[] oneComponents = one.split(":");
					String[] twoComponents = two.split(":");

					int result = oneComponents[0].compareTo(twoComponents[0]);

					if (result == 0) {
						result = oneComponents[1].compareTo(twoComponents[1]);
					}

					return result;
				}

			});

		sortedVersions.putAll(managedVersions);

		for (Map.Entry<String, String> entry : sortedVersions.entrySet()) {
			_output.println("    " + entry.getKey() + " " + entry.getValue());
		}

		_output.println();
	}

	private final PrintWriter _output;

}