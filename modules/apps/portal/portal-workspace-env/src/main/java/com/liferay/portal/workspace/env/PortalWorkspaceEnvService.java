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

package com.liferay.portal.workspace.env;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringBundler;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.service.command.CommandProcessor;

import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * @author Gregory Amerson
 */
@Component(
	property = {
		CommandProcessor.COMMAND_FUNCTION + "=buildModules",
		CommandProcessor.COMMAND_SCOPE + ":String=portal"
	},
	service = Object.class
)
public class PortalWorkspaceEnvService {

	public File[] buildModules(Object object) throws Exception {
		if (object == null) {
			return null;
		}

		String content;
		File file;

		if (object instanceof File) {
			file = (File)object;
		}
		else {
			try {
				file = new File(object.toString());
			}
			catch (Exception exception) {
				file = null;
			}
		}

		if ((file != null) && file.exists()) {
			content = _read(Files.newInputStream(file.toPath()));
		}
		else {
			content = object.toString();
		}

		return _buildModules(content);
	}

	@Activate
	protected void activate() {
		try {
			ClassLoader classLoader =
				PortalWorkspaceEnvService.class.getClassLoader();

			_portalWorkspaceEnvJarPaths = _extractPortalWorkspaceEnvJars(
				classLoader);
			_projectTemplatesJarPath = _extractProjectTemplatesJar(classLoader);
		}
		catch (IOException ioException) {
			_log.error(ioException, ioException);
		}
	}

	private static String _loadTemplate(String name) {
		try (InputStream inputStream =
				PortalWorkspaceEnvService.class.getResourceAsStream(name)) {

			return _read(inputStream);
		}
		catch (IOException ioException) {
			return MessageFormat.format(
				"Error loading {0}: {1}", name, ioException.getMessage());
		}
	}

	private static String _read(InputStream inputStream) throws IOException {
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

	private File[] _buildModules(String serviceXmlContent) throws Exception {
		if (_log.isDebugEnabled()) {
			_log.debug("Building modules for service.xml");
		}

		Path tempPath = Files.createTempDirectory(
			Paths.get("/tmp"), "portalWorkspaceEnv");

		Path workspacePath = _generateWorkspace(
			tempPath, _projectTemplatesJarPath);

		_writeBuildGradle(
			workspacePath, _portalWorkspaceEnvJarPaths[0].getParent());

		Path modulesPath = workspacePath.resolve("modules");

		String[] params = _getServiceBuilderParams(serviceXmlContent);

		if (params.length == 2) {
			Path serviceModulesPath = _generateServiceBuilderModules(
				_projectTemplatesJarPath, modulesPath, params[0], params[1]);

			_writeServiceXmlFile(serviceXmlContent, serviceModulesPath);

			_removeReleaseApiDependencies(modulesPath);

			_buildServiceJars(workspacePath, params[1]);

			File[] jarFiles = _findJarFiles(modulesPath);

			if (_log.isDebugEnabled()) {
				_log.debug("Generated jar files: ");

				for (File jarFile : jarFiles) {
					_log.debug(jarFile.toString());
				}
			}

			return jarFiles;
		}

		return new File[0];
	}

	private void _buildServiceJars(Path workspacePath, String namespace) {
		GradleConnector gradleConnector = GradleConnector.newConnector();

		gradleConnector.forProjectDirectory(workspacePath.toFile());

		try (ProjectConnection projectConnection = gradleConnector.connect()) {
			BuildLauncher buildLauncher = projectConnection.newBuild();

			buildLauncher.forTasks(
				StringBundler.concat(
					":modules:", namespace, ":", namespace,
					"-service:buildService"));

			if (_log.isDebugEnabled()) {
				_log.debug("Running buildService task");
			}

			buildLauncher.run();

			buildLauncher = projectConnection.newBuild();

			buildLauncher.forTasks("jar");

			if (_log.isDebugEnabled()) {
				_log.debug("Running jar task");
			}

			buildLauncher.run();
		}
	}

	private Path[] _extractPortalWorkspaceEnvJars(ClassLoader classLoader)
		throws IOException {

		List<Path> paths = new ArrayList<>();

		Path libsPath = Files.createTempDirectory("portalWorkspaceEnvLibs");

		try (Scanner scanner = new Scanner(
				classLoader.getResourceAsStream(
					"portal-workspace-env-jars.txt"))) {

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
				classLoader.getResourceAsStream(
					"portal-workspace-env-jars.txt"))) {

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

	private File[] _findJarFiles(Path modulesPath) throws Exception {
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
			Path projectTemplatesJarPath, Path modulesPath, String packageName,
			String namespace)
		throws Exception {

		if (_log.isDebugEnabled()) {
			_log.debug(
				MessageFormat.format(
					"Using params: namespace {0}, package-path: {1}", namespace,
					packageName));
		}

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
		throws Exception {

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

		Matcher matcher = _pattern.matcher(serviceXmlContent);

		if (matcher.matches()) {
			return new String[] {matcher.group(1), matcher.group(2)};
		}

		return new String[0];
	}

	private void _removeReleaseApiDependencies(Path modulesPath)
		throws Exception {

		try (Stream<Path> paths = Files.walk(modulesPath)) {
			Path buildGradlePath = Paths.get("build.gradle");

			paths.filter(
				path -> Objects.equals(buildGradlePath, path.getFileName())
			).forEach(
				path -> {
					try {
						List<String> lines = Files.readAllLines(path);

						Stream<String> stream = lines.stream();

						String content = stream.map(
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
					catch (IOException ioException) {
						_log.error(ioException, ioException);
					}
				}
			);
		}
	}

	private void _writeBuildGradle(
			Path workspacePath, Path portalWorkspaceEnvJarsPath)
		throws Exception {

		String content = _TPL_ROOT_BUILD_GRADLE;

		content = content.replaceAll(
			"%portal_workspace_env_jars_dir%",
			portalWorkspaceEnvJarsPath.toString());

		Path catalinaBasePath = Paths.get(_CATALINA_BASE);

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
		throws Exception {

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
					catch (IOException ioException) {
						_log.error(ioException, ioException);
					}
				}
			);
		}
	}

	private static final String _CATALINA_BASE = System.getProperty(
		"catalina.base");

	private static final String _TPL_ROOT_BUILD_GRADLE;

	private static final Log _log = LogFactoryUtil.getLog(
		PortalWorkspaceEnvService.class);

	private static final Pattern _pattern = Pattern.compile(
		".*package-path=\"(.*)\".*<namespace>(.*)</namespace>.*",
		Pattern.DOTALL | Pattern.MULTILINE);

	static {
		_TPL_ROOT_BUILD_GRADLE = _loadTemplate("root.build.gradle.tpl");
	}

	private Path[] _portalWorkspaceEnvJarPaths;
	private Path _projectTemplatesJarPath;

}