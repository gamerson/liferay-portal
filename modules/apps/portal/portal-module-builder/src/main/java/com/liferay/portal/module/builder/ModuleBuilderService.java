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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.text.MessageFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.felix.service.command.CommandProcessor;

import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Gregory Amerson
 */
@Component(
	property = {
		CommandProcessor.COMMAND_FUNCTION + "=buildModule",
		CommandProcessor.COMMAND_FUNCTION + "=buildModuleFile",
		CommandProcessor.COMMAND_SCOPE + ":String=portal"
	},
	service = ModuleBuilderService.class
)
public class ModuleBuilderService {

	public File[] buildModule(String serviceXmlContent) {
		try {
			Path tempPath = Files.createTempDirectory(
				Paths.get("/tmp"), "moduleBuilder");

			Path workspacePath = _generateWorkspace(
				tempPath, _projectTemplatesJarPath);

			_writeBuildGradle(
				workspacePath, _moduleBuilderLibPaths[0].getParent());

			Path modulesPath = workspacePath.resolve("modules");

			String[] params = _getServiceBuilderParams(serviceXmlContent);

			Path serviceModulesPath = _generateServiceBuilderModules(
				_projectTemplatesJarPath, modulesPath, params[0], params[1]);

			_writeServiceXmlFile(serviceXmlContent, serviceModulesPath);

			_removeReleaseApiDependencies(modulesPath);

			_buildServiceJars(workspacePath);

			return _findJarFiles(modulesPath);
		}
		catch (Throwable throwable) {
			throwable.printStackTrace(System.err);
		}

		return null;
	}

	public File[] buildModuleFile(File serviceXmlFile) {
		try {
			return buildModule(
				_read(Files.newInputStream(serviceXmlFile.toPath())));
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Activate
	protected void activate() {
		try {
			ClassLoader classLoader =
				ModuleBuilderService.class.getClassLoader();

			_moduleBuilderLibPaths = _extractModuleBuilderLibs(classLoader);
			_projectTemplatesJarPath = _extractProjectTemplatesJar(classLoader);
		}
		catch (IOException e) {
		}
	}

	private void _buildServiceJars(Path workspacePath) {
		GradleConnector gradleConnector = GradleConnector.newConnector();

		gradleConnector.forProjectDirectory(workspacePath.toFile());

		try (ProjectConnection projectConnection = gradleConnector.connect()) {
			BuildLauncher buildLauncher = projectConnection.newBuild();

			buildLauncher.forTasks("buildService");

			buildLauncher.run();

			buildLauncher = projectConnection.newBuild();

			buildLauncher.forTasks("jar");

			buildLauncher.run();
		}
	}

	private Path[] _extractModuleBuilderLibs(ClassLoader classLoader)
		throws IOException {

		List<Path> paths = new ArrayList<>();

		Path libsPath = Files.createTempDirectory("moduleBuilderLibs");

		try (Scanner scanner = new Scanner(
				classLoader.getResourceAsStream("module-builder-jars.txt"))) {

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();

				if (!line.startsWith("com.liferay.project.templates") &&
					!line.startsWith(
						"com.liferay.portal.tools.service.builder")) {

					try (InputStream inputStream =
							classLoader.getResourceAsStream(line)) {

						Path libPath = libsPath.resolve(line);

						Files.copy(
							inputStream, libPath,
							StandardCopyOption.REPLACE_EXISTING);

						paths.add(libPath);
					}
				}
			}
		}

		return paths.toArray(new Path[0]);
	}

	private Path _extractProjectTemplatesJar(ClassLoader classLoader)
		throws IOException {

		Path tempPath = Files.createTempDirectory("projectTemplatesJar");

		try (Scanner scanner = new Scanner(
				classLoader.getResourceAsStream("module-builder-jars.txt"))) {

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();

				if (line.startsWith("com.liferay.project.templates")) {
					try (InputStream inputStream =
							classLoader.getResourceAsStream(line)) {

						Path projectTemplatesJarPath = tempPath.resolve(line);

						Files.copy(
							inputStream, projectTemplatesJarPath,
							StandardCopyOption.REPLACE_EXISTING);

						return projectTemplatesJarPath;
					}
				}
			}
		}

		return null;
	}

	private File[] _findJarFiles(Path modulesPath) throws IOException {
		try (Stream<Path> paths = Files.walk(modulesPath)) {
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

			return jarFiles.toArray(new File[0]);
		}
	}

	private Path _generateServiceBuilderModules(
			Path projectTemplatesJarPath, Path modulesPath, String namespace,
			String packageName)
		throws Exception {

		ProcessBuilder processBuilder = new ProcessBuilder();

		processBuilder.command(
			"java", "-jar", projectTemplatesJarPath.toString(), "--name",
			namespace, "--template", "service-builder", "--package-name",
			packageName);
		processBuilder.directory(modulesPath.toFile());
		processBuilder.inheritIO();

		Process process = processBuilder.start();

		process.waitFor();

		return modulesPath.resolve(namespace);
	}

	private Path _generateWorkspace(
			Path projectPath, Path projectTemplatesJarPath)
		throws InterruptedException, IOException {

		ProcessBuilder processBuilder = new ProcessBuilder();

		processBuilder.directory(projectPath.toFile());
		processBuilder.inheritIO();
		processBuilder.command(
			"java", "-jar", projectTemplatesJarPath.toString(), "--name",
			"workspace", "--template", "workspace");

		Process process = processBuilder.start();

		process.waitFor();

		return projectPath.resolve("workspace");
	}

	private String[] _getServiceBuilderParams(String serviceXmlContent)
		throws Exception {

		DocumentBuilderFactory documentBuilderFactory =
			SecureXMLFactoryProviderUtil.newDocumentBuilderFactory();

		documentBuilderFactory.setFeature(
			"http://apache.org/xml/features/disallow-doctype-decl", false);

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		Document document = documentBuilder.parse(
			new ByteArrayInputStream(serviceXmlContent.getBytes()));

		XPathFactory xPathFactory = XPathFactory.newInstance();

		XPath xPath = xPathFactory.newXPath();

		String namespace = (String)xPath.evaluate(
			"/service-builder/namespace/text()", document,
			XPathConstants.STRING);

		Node packageNameNode = (Node)xPath.evaluate(
			"/service-builder/@package-path", document, XPathConstants.NODE);

		return new String[] {namespace, packageNameNode.getTextContent()};
	}

	private String _loadTemplate(String name) {
		try (InputStream inputStream =
				ModuleBuilderService.class.getResourceAsStream(name)) {

			return _read(inputStream);
		}
		catch (IOException e) {
			return MessageFormat.format(
				"Error loading {0}: {1}", name, e.getMessage());
		}
	}

	private String _read(InputStream inputStream) throws IOException {
		byte[] buffer = new byte[8192];
		int offset = 0;

		while (true) {
			int count = inputStream.read(
				buffer, offset, buffer.length - offset);

			if (count == -1) {
				break;
			}

			offset += count;

			if (offset == buffer.length) {
				byte[] newBuffer = new byte[buffer.length << 1];

				System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);

				buffer = newBuffer;
			}
		}

		if (offset == 0) {
			return "";
		}

		return new String(buffer, 0, offset, "UTF-8");
	}

	private void _removeReleaseApiDependencies(Path modulesPath)
		throws IOException {

		try (Stream<Path> paths = Files.walk(modulesPath)) {
			Path buildGradlePath = Paths.get("build.gradle");

			paths.filter(
				path -> Objects.equals(buildGradlePath, path.getFileName())
			).forEach(
				path -> {
					try {
						List<String> lines = Files.readAllLines(path);

						String content = lines.stream(
						).map(
							line ->
								line.matches(".*compileOnly.*release.*api.*") ?
									null : line
						).filter(
							Objects::nonNull
						).collect(
							Collectors.joining(System.lineSeparator())
						);

						Files.write(path, content.getBytes());
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			);
		}
	}

	private void _writeBuildGradle(
			Path workspacePath, Path moduleBuilderLibsPath)
		throws IOException {

		String content = _TPL_ROOT_BUILD_GRADLE;

		content = content.replaceAll(
			"%module_builder_jars_dir%", moduleBuilderLibsPath.toString());

		Path catalinaBasePath = Paths.get(_catalinaBase);

		Path extPath = catalinaBasePath.resolve("lib/ext");

		content = content.replaceAll(
			"%tomcat_lib_ext_dir%", extPath.toString());

		Path osgiCorePath = catalinaBasePath.resolve("../osgi/core");

		File osgiCoreDir = osgiCorePath.toFile();

		osgiCoreDir = osgiCoreDir.getCanonicalFile();

		content = content.replaceAll("%osgi_core_dir%", osgiCoreDir.toString());

		Files.write(workspacePath.resolve("build.gradle"), content.getBytes());
	}

	private void _writeServiceXmlFile(
			String serviceXmlContent, Path serviceModulesPath)
		throws IOException {

		try (Stream<Path> paths = Files.walk(serviceModulesPath)) {
			Path serviceXmlPath = Paths.get("service.xml");

			paths.filter(
				path -> Objects.equals(serviceXmlPath, path.getFileName())
			).findFirst(
			).ifPresent(
				path -> {
					try {
						Files.write(path, serviceXmlContent.getBytes());
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			);
		}
	}

	private static final String _TPL_ROOT_BUILD_GRADLE;

	private static final String _catalinaBase = System.getProperty(
		"catalina.base");

	static {
		_TPL_ROOT_BUILD_GRADLE = _loadTemplate("root.build.gradle.tpl");
	}

	private Path[] _moduleBuilderLibPaths;
	private Path _projectTemplatesJarPath;

}