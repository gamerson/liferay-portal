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

package com.liferay.portal.module.builder;

import com.liferay.portal.kernel.security.xml.SecureXMLFactoryProviderUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import org.osgi.service.component.annotations.Component;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Gregory Amerson
 */
@Component(
	property = {
		"osgi.command.function=buildModule", "osgi.command.scope=portal"
	},
	service = ModuleBuilderService.class
)
public class ModuleBuilderService {

	public File[] buildModule(File serviceXmlFile) {
		try {
			Path projectPath = Files.createTempDirectory(
				Paths.get("/tmp"), "moduleBuilder");

			ClassLoader classLoader =
				ModuleBuilderService.class.getClassLoader();

			Path projectTemplatesJarPath = projectPath.resolve(
				"com.liferay.project.templates-5.0.117.jar");

			try (InputStream inputStream = classLoader.getResourceAsStream(
					"lib/com.liferay.project.templates-5.0.117.jar")) {

				Files.copy(
					inputStream, projectTemplatesJarPath,
					StandardCopyOption.REPLACE_EXISTING);
			}

			ProcessBuilder processBuilder = new ProcessBuilder();

			processBuilder.directory(projectPath.toFile());
			processBuilder.inheritIO();
			processBuilder.command(
				"java", "-jar", projectTemplatesJarPath.toString(), "--name",
				"workspace", "--template", "workspace");

			Process process = processBuilder.start();

			process.waitFor();

			Path workspacePath = projectPath.resolve("workspace");

			Files.write(
				workspacePath.resolve("gradle.properties"),
				"liferay.workspace.product=portal-7.3-ga7".getBytes());

			Path modulesPath = workspacePath.resolve("modules");

			DocumentBuilderFactory documentBuilderFactory =
				SecureXMLFactoryProviderUtil.newDocumentBuilderFactory();

			documentBuilderFactory.setFeature(
				"http://apache.org/xml/features/disallow-doctype-decl", true);

			DocumentBuilder documentBuilder =
				documentBuilderFactory.newDocumentBuilder();

			Document document = documentBuilder.parse(
				new FileInputStream(serviceXmlFile));

			XPathFactory xPathFactory = XPathFactory.newInstance();

			XPath xPath = xPathFactory.newXPath();

			String namespace = (String)xPath.evaluate(
				"/service-builder/namespace/text()", document,
				XPathConstants.STRING);

			Node packageNameNode = (Node)xPath.evaluate(
				"/service-builder/@package-path", document,
				XPathConstants.NODE);

			String packageName = packageNameNode.getTextContent();

			processBuilder = new ProcessBuilder();

			processBuilder.directory(modulesPath.toFile());
			processBuilder.inheritIO();
			processBuilder.command(
				"java", "-jar", projectTemplatesJarPath.toString(), "--name",
				namespace, "--template", "service-builder", "--package-name",
				packageName);

			process = processBuilder.start();

			process.waitFor();

			GradleConnector gradleConnector = GradleConnector.newConnector();

			gradleConnector.forProjectDirectory(workspacePath.toFile());

			try (ProjectConnection projectConnection =
					gradleConnector.connect()) {

				BuildLauncher buildLauncher = projectConnection.newBuild();

				buildLauncher.forTasks("buildService");

				buildLauncher.run();

				buildLauncher = projectConnection.newBuild();

				buildLauncher.forTasks("jar");

				buildLauncher.run();

				Stream<Path> paths = Files.walk(modulesPath);

				List<File> jarFiles = paths.filter(
					path -> {
						String pathString = path.toString();

						return pathString.matches(".*/build/libs/.*\\.jar");
					}
				).map(
					Path::toFile
				).collect(
					Collectors.toList()
				);

				paths.close();

				return jarFiles.toArray(new File[0]);
			}
			catch (Throwable throwable) {
				throw throwable;
			}
		}
		catch (Throwable throwable) {
			throwable.printStackTrace(System.err);
		}

		return null;
	}

}