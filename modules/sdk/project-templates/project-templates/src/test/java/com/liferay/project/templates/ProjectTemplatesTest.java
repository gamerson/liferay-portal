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

package com.liferay.project.templates;

import aQute.bnd.main.bnd;

import com.liferay.maven.executor.MavenExecutor;
import com.liferay.project.templates.internal.Archetyper;
import com.liferay.project.templates.internal.util.FileUtil;
import com.liferay.project.templates.internal.util.Validator;
import com.liferay.project.templates.util.DirectoryComparator;
import com.liferay.project.templates.util.FileTestUtil;
import com.liferay.project.templates.util.StringTestUtil;
import com.liferay.project.templates.util.XMLTestUtil;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;

import java.net.URI;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import net.diibadaaba.zipdiff.DifferenceCalculator;
import net.diibadaaba.zipdiff.Differences;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * @author Lawrence Lee
 * @author Gregory Amerson
 * @author Andrea Di Giorgi
 */
public class ProjectTemplatesTest {

	@ClassRule
	public static final MavenExecutor mavenExecutor = new MavenExecutor();

	@ClassRule
	public static final TemporaryFolder testCaseTemporaryFolder =
		new TemporaryFolder();

	@BeforeClass
	public static void setUpClass() throws Exception {
		String gradleDistribution = System.getProperty("gradle.distribution");

		if (Validator.isNull(gradleDistribution)) {
			Properties properties = FileTestUtil.readProperties(
				"gradle-wrapper/gradle/wrapper/gradle-wrapper.properties");

			gradleDistribution = properties.getProperty("distributionUrl");
		}

		_gradleDistribution = URI.create(gradleDistribution);

		_projectTemplateVersions = FileUtil.readProperties(
			Paths.get("build", "project-template-versions.properties"));

		XPathFactory xPathFactory = XPathFactory.newInstance();

		XPath xPath = xPathFactory.newXPath();

		_pomXmlNpmInstallXPathExpression = xPath.compile(
			"//id[contains(text(),'npm-install')]/parent::*");
	}

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	protected static void addNpmrc(File projectDir) throws IOException {
		File npmrcFile = new File(projectDir, ".npmrc");

		String content = "sass_binary_site=" + _NODEJS_NPM_CI_SASS_BINARY_SITE;

		Files.write(
			npmrcFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
	}

	protected static void buildProjects(
			File gradleProjectDir, File mavenProjectDir)
		throws Exception {

		File gradleOutputDir = new File(gradleProjectDir, "build/libs");
		File mavenOutputDir = new File(mavenProjectDir, "target");

		_buildProjects(
			gradleProjectDir, mavenProjectDir, gradleOutputDir, mavenOutputDir,
			GRADLE_TASK_PATH_BUILD);
	}

	protected static File buildTemplateWithGradle(
			File destinationDir, String template, String name, boolean gradle,
			boolean maven, String... args)
		throws Exception {

		List<String> completeArgs = new ArrayList<>(args.length + 6);

		completeArgs.add("--archetypes-dir");

		File archetypesDir = FileUtil.getJarFile(ProjectTemplatesTest.class);

		completeArgs.add(archetypesDir.getPath());

		completeArgs.add("--destination");
		completeArgs.add(destinationDir.getPath());

		if (!gradle) {
			completeArgs.add("--gradle");
			completeArgs.add(String.valueOf(gradle));
		}

		if (maven) {
			completeArgs.add("--maven");
		}

		if (Validator.isNotNull(name)) {
			completeArgs.add("--name");
			completeArgs.add(name);
		}

		if (Validator.isNotNull(template)) {
			completeArgs.add("--template");
			completeArgs.add(template);
		}

		for (String arg : args) {
			completeArgs.add(arg);
		}

		ProjectTemplates.main(
			completeArgs.toArray(new String[completeArgs.size()]));

		File projectDir = new File(destinationDir, name);

		testExists(projectDir, ".gitignore");

		if (gradle) {
			testExists(projectDir, "build.gradle");
		}
		else {
			testNotExists(projectDir, "build.gradle");
		}

		if (maven) {
			testExists(projectDir, "pom.xml");
		}
		else {
			testNotExists(projectDir, "pom.xml");
		}

		boolean workspace = WorkspaceUtil.isWorkspace(destinationDir);

		if (gradle && !workspace) {
			for (String fileName : _GRADLE_WRAPPER_FILE_NAMES) {
				testExists(projectDir, fileName);
			}

			_testExecutable(projectDir, "gradlew");
		}
		else {
			for (String fileName : _GRADLE_WRAPPER_FILE_NAMES) {
				testNotExists(projectDir, fileName);
			}

			testNotExists(projectDir, "settings.gradle");
		}

		if (maven && !workspace) {
			for (String fileName : _MAVEN_WRAPPER_FILE_NAMES) {
				testExists(projectDir, fileName);
			}

			_testExecutable(projectDir, "mvnw");
		}
		else {
			for (String fileName : _MAVEN_WRAPPER_FILE_NAMES) {
				testNotExists(projectDir, fileName);
			}
		}

		return projectDir;
	}

	protected static File buildTemplateWithGradle(
			File destinationDir, String template, String name, String... args)
		throws Exception {

		return buildTemplateWithGradle(
			destinationDir, template, name, true, false, args);
	}

	protected static File buildTemplateWithMaven(
			File parentDir, File destinationDir, String template, String name,
			String groupId, String... args)
		throws Exception {

		List<String> completeArgs = new ArrayList<>();

		completeArgs.add("archetype:generate");
		completeArgs.add("--batch-mode");

		String archetypeArtifactId =
			"com.liferay.project.templates." + template.replace('-', '.');

		completeArgs.add("-DarchetypeArtifactId=" + archetypeArtifactId);

		String projectTemplateVersion = _projectTemplateVersions.getProperty(
			archetypeArtifactId);

		Assert.assertTrue(
			"Unable to get project template version",
			Validator.isNotNull(projectTemplateVersion));

		completeArgs.add("-DarchetypeGroupId=com.liferay");
		completeArgs.add("-DarchetypeVersion=" + projectTemplateVersion);
		completeArgs.add("-Dauthor=" + System.getProperty("user.name"));
		completeArgs.add("-DgroupId=" + groupId);
		completeArgs.add("-DartifactId=" + name);
		completeArgs.add("-Dversion=1.0.0");

		boolean liferayVersionSet = false;
		boolean projectTypeSet = false;

		for (String arg : args) {
			completeArgs.add(arg);

			if (arg.startsWith("-DliferayVersion=")) {
				liferayVersionSet = true;
			}
			else if (arg.startsWith("-DprojectType=")) {
				projectTypeSet = true;
			}
		}

		if (!liferayVersionSet) {
			completeArgs.add("-DliferayVersion=7.0");
		}

		if (!projectTypeSet) {
			completeArgs.add("-DprojectType=standalone");
		}

		executeMaven(destinationDir, completeArgs.toArray(new String[0]));

		File projectDir = new File(destinationDir, name);

		testExists(projectDir, "pom.xml");
		testNotExists(projectDir, "gradlew");
		testNotExists(projectDir, "gradlew.bat");
		testNotExists(projectDir, "gradle/wrapper/gradle-wrapper.jar");
		testNotExists(projectDir, "gradle/wrapper/gradle-wrapper.properties");

		_testArchetyper(
			parentDir, destinationDir, projectDir, name, groupId, template,
			completeArgs);

		return projectDir;
	}

	protected static void configureExecuteNpmTask(File projectDir)
		throws Exception {

		File buildGradleFile = testContains(
			projectDir, "build.gradle", "com.liferay.gradle.plugins",
			"com.liferay.plugin");

		StringBuilder sb = new StringBuilder();

		String lineSeparator = System.lineSeparator();

		sb.append(lineSeparator);

		sb.append(
			"import com.liferay.gradle.plugins.node.tasks.ExecuteNpmTask");
		sb.append(lineSeparator);

		sb.append("tasks.withType(ExecuteNpmTask) {");
		sb.append(lineSeparator);

		sb.append("\tregistry = '");
		sb.append(_NODEJS_NPM_CI_REGISTRY);
		sb.append('\'');
		sb.append(lineSeparator);

		sb.append('}');

		String executeNpmTaskScript = sb.toString();

		Files.write(
			buildGradleFile.toPath(),
			executeNpmTaskScript.getBytes(StandardCharsets.UTF_8),
			StandardOpenOption.APPEND);
	}

	protected static void configurePomNpmConfiguration(File projectDir)
		throws Exception {

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		File pomXmlFile = new File(projectDir, "pom.xml");

		Document document = documentBuilder.parse(pomXmlFile);

		NodeList nodeList = (NodeList)_pomXmlNpmInstallXPathExpression.evaluate(
			document, XPathConstants.NODESET);

		Node executionNode = nodeList.item(0);

		Element configurationElement = document.createElement("configuration");

		executionNode.appendChild(configurationElement);

		Element argumentsElement = document.createElement("arguments");

		configurationElement.appendChild(argumentsElement);

		Text text = document.createTextNode(
			"install --registry=" + _NODEJS_NPM_CI_REGISTRY);

		argumentsElement.appendChild(text);

		TransformerFactory transformerFactory =
			TransformerFactory.newInstance();

		Transformer transformer = transformerFactory.newTransformer();

		DOMSource domSource = new DOMSource(document);

		StreamResult streamResult = new StreamResult(pomXmlFile);

		transformer.transform(domSource, streamResult);
	}

	protected static void createNewFiles(String fileName, File... dirs)
		throws IOException {

		for (File dir : dirs) {
			File file = new File(dir, fileName);

			File parentDir = file.getParentFile();

			if (!parentDir.isDirectory()) {
				Assert.assertTrue(parentDir.mkdirs());
			}

			Assert.assertTrue(file.createNewFile());
		}
	}

	protected static Optional<String> executeGradle(
			File projectDir, boolean debug, String... taskPaths)
		throws IOException {

		final String repositoryUrl = mavenExecutor.getRepositoryUrl();

		Files.walkFileTree(
			projectDir.toPath(),
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					String fileName = String.valueOf(path.getFileName());

					if (fileName.equals("build.gradle") ||
						fileName.equals("settings.gradle")) {

						String content = FileUtil.read(path);

						if (Validator.isNotNull(repositoryUrl)) {
							content = content.replace(
								"\"" + _REPOSITORY_CDN_URL + "\"",
								"\"" + repositoryUrl + "\"");
						}

						content = content.replace(
							"repositories {", "repositories {\tmavenLocal()\n");

						Files.write(
							path, content.getBytes(StandardCharsets.UTF_8));
					}

					return FileVisitResult.CONTINUE;
				}

			});

		GradleRunner gradleRunner = GradleRunner.create();

		List<String> arguments = new ArrayList<>(taskPaths.length + 5);

		if (debug) {
			arguments.add("--debug");
		}
		else {
			arguments.add("--stacktrace");
		}

		String httpProxyHost = mavenExecutor.getHttpProxyHost();
		int httpProxyPort = mavenExecutor.getHttpProxyPort();

		if (Validator.isNotNull(httpProxyHost) && (httpProxyPort > 0)) {
			arguments.add("-Dhttp.proxyHost=" + httpProxyHost);
			arguments.add("-Dhttp.proxyPort=" + httpProxyPort);
		}

		for (String taskPath : taskPaths) {
			arguments.add(taskPath);
		}

		String stdOutput = null;
		StringWriter stringWriter = new StringWriter();

		if (debug) {
			gradleRunner.forwardStdOutput(stringWriter);
		}

		gradleRunner.withArguments(arguments);

		gradleRunner.withGradleDistribution(_gradleDistribution);
		gradleRunner.withProjectDir(projectDir);

		BuildResult buildResult = gradleRunner.build();

		for (String taskPath : taskPaths) {
			BuildTask buildTask = buildResult.task(taskPath);

			Assert.assertNotNull(
				"Build task \"" + taskPath + "\" not found", buildTask);

			Assert.assertEquals(
				"Unexpected outcome for task \"" + buildTask.getPath() + "\"",
				TaskOutcome.SUCCESS, buildTask.getOutcome());
		}

		if (debug) {
			stdOutput = stringWriter.toString();
			stringWriter.close();
		}

		return Optional.ofNullable(stdOutput);
	}

	protected static void executeGradle(File projectDir, String... taskPaths)
		throws IOException {

		executeGradle(projectDir, false, taskPaths);
	}

	protected static String executeMaven(File projectDir, String... args)
		throws Exception {

		File pomXmlFile = new File(projectDir, "pom.xml");

		if (pomXmlFile.exists()) {
			DocumentBuilderFactory documentBuilderFactory =
				DocumentBuilderFactory.newInstance();

			DocumentBuilder documentBuilder =
				documentBuilderFactory.newDocumentBuilder();

			Document document = documentBuilder.parse(pomXmlFile);

			_addNexusRepositoriesElement(
				document, "repositories", "repository");
			_addNexusRepositoriesElement(
				document, "pluginRepositories", "pluginRepository");

			TransformerFactory transformerFactory =
				TransformerFactory.newInstance();

			Transformer transformer = transformerFactory.newTransformer();

			DOMSource domSource = new DOMSource(document);

			StreamResult streamResult = new StreamResult(pomXmlFile);

			transformer.transform(domSource, streamResult);
		}

		String[] completeArgs = new String[args.length + 1];

		completeArgs[0] = "--update-snapshots";

		System.arraycopy(args, 0, completeArgs, 1, args.length);

		MavenExecutor.Result result = mavenExecutor.execute(projectDir, args);

		Assert.assertEquals(result.output, 0, result.exitCode);

		return result.output;
	}

	protected static File testContains(
			File dir, String fileName, boolean regex, String... strings)
		throws IOException {

		return _testContainsOrNot(dir, fileName, regex, true, strings);
	}

	protected static File testContains(
			File dir, String fileName, String... strings)
		throws IOException {

		return testContains(dir, fileName, false, strings);
	}

	protected static void testContainsJarEntry(File file, String name)
		throws IOException {

		try (JarFile jarFile = new JarFile(file)) {
			Assert.assertNotNull(jarFile.getJarEntry(name));
		}
	}

	protected static File testEquals(
			File dir, String fileName, String expectedContent)
		throws IOException {

		File file = testExists(dir, fileName);

		Assert.assertEquals(
			"Incorrect " + fileName, expectedContent,
			FileUtil.read(file.toPath()));

		return file;
	}

	protected static File testExists(File dir, String fileName) {
		File file = new File(dir, fileName);

		Assert.assertTrue("Missing " + fileName, file.exists());

		return file;
	}

	protected static void testExists(ZipFile zipFile, String name) {
		Assert.assertNotNull("Missing " + name, zipFile.getEntry(name));
	}

	protected static File testNotContains(
			File dir, String fileName, boolean regex, String... strings)
		throws IOException {

		return _testContainsOrNot(dir, fileName, regex, false, strings);
	}

	protected static File testNotContains(
			File dir, String fileName, String... strings)
		throws IOException {

		return testNotContains(dir, fileName, false, strings);
	}

	protected static File testNotExists(File dir, String fileName) {
		File file = new File(dir, fileName);

		Assert.assertFalse("Unexpected " + fileName, file.exists());

		return file;
	}

	protected static File testStartsWith(
			File dir, String fileName, String prefix)
		throws IOException {

		File file = testExists(dir, fileName);

		String content = FileUtil.read(file.toPath());

		Assert.assertTrue(
			fileName + " must start with \"" + prefix + "\"",
			content.startsWith(prefix));

		return file;
	}

	protected static void testWarsDiff(File warFile1, File warFile2)
		throws IOException {

		DifferenceCalculator differenceCalculator = new DifferenceCalculator(
			warFile1, warFile2);

		differenceCalculator.setFilenameRegexToIgnore(
			Collections.singleton(".*META-INF.*"));
		differenceCalculator.setIgnoreTimestamps(true);

		Differences differences = differenceCalculator.getDifferences();

		if (!differences.hasDifferences()) {
			return;
		}

		StringBuilder message = new StringBuilder();

		message.append("WAR ");
		message.append(warFile1);
		message.append(" and ");
		message.append(warFile2);
		message.append(" do not match:");
		message.append(System.lineSeparator());

		boolean realChange;

		Map<String, ZipArchiveEntry> added = differences.getAdded();
		Map<String, ZipArchiveEntry[]> changed = differences.getChanged();
		Map<String, ZipArchiveEntry> removed = differences.getRemoved();

		if (added.isEmpty() && !changed.isEmpty() && removed.isEmpty()) {
			realChange = false;

			ZipFile zipFile1 = null;
			ZipFile zipFile2 = null;

			try {
				zipFile1 = new ZipFile(warFile1);
				zipFile2 = new ZipFile(warFile2);

				for (Map.Entry<String, ZipArchiveEntry[]> entry :
						changed.entrySet()) {

					ZipArchiveEntry[] zipArchiveEntries = entry.getValue();

					ZipArchiveEntry zipArchiveEntry1 = zipArchiveEntries[0];
					ZipArchiveEntry zipArchiveEntry2 = zipArchiveEntries[0];

					if (zipArchiveEntry1.isDirectory() &&
						zipArchiveEntry2.isDirectory() &&
						(zipArchiveEntry1.getSize() ==
							zipArchiveEntry2.getSize()) &&
						(zipArchiveEntry1.getCompressedSize() <= 2) &&
						(zipArchiveEntry2.getCompressedSize() <= 2)) {

						// Skip zipdiff bug

						continue;
					}

					try (InputStream inputStream1 = zipFile1.getInputStream(
							zipFile1.getEntry(zipArchiveEntry1.getName()));
						InputStream inputStream2 = zipFile2.getInputStream(
							zipFile2.getEntry(zipArchiveEntry2.getName()))) {

						List<String> lines1 = StringTestUtil.readLines(
							inputStream1);
						List<String> lines2 = StringTestUtil.readLines(
							inputStream2);

						message.append(System.lineSeparator());

						message.append("--- ");
						message.append(zipArchiveEntry1.getName());
						message.append(System.lineSeparator());

						message.append("+++ ");
						message.append(zipArchiveEntry2.getName());
						message.append(System.lineSeparator());

						Patch<String> diff = DiffUtils.diff(lines1, lines2);

						for (Delta<String> delta : diff.getDeltas()) {
							message.append('\t');
							message.append(delta.getOriginal());
							message.append(System.lineSeparator());

							message.append('\t');
							message.append(delta.getRevised());
							message.append(System.lineSeparator());
						}
					}

					realChange = true;

					break;
				}
			}
			finally {
				ZipFile.closeQuietly(zipFile1);
				ZipFile.closeQuietly(zipFile2);
			}
		}
		else {
			realChange = true;
		}

		Assert.assertFalse(message.toString(), realChange);
	}

	protected static void writeServiceClass(File projectDir)
		throws IOException {

		String importLine =
			"import com.liferay.portal.kernel.events.LifecycleAction;";
		String classLine =
			"public class FooAction implements LifecycleAction {";

		File actionJavaFile = testContains(
			projectDir, "src/main/java/servicepreaction/FooAction.java",
			"package servicepreaction;", importLine,
			"service = LifecycleAction.class", classLine);

		Path actionJavaPath = actionJavaFile.toPath();

		List<String> lines = Files.readAllLines(
			actionJavaPath, StandardCharsets.UTF_8);

		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
				actionJavaPath, StandardCharsets.UTF_8)) {

			for (String line : lines) {
				FileTestUtil.write(bufferedWriter, line);

				if (line.equals(classLine)) {
					FileTestUtil.write(
						bufferedWriter, "@Override",
						"public void processLifecycleEvent(",
						"LifecycleEvent lifecycleEvent)",
						"throws ActionException {", "System.out.println(",
						"\"login.event.pre=\" + lifecycleEvent);", "}");
				}
				else if (line.equals(importLine)) {
					FileTestUtil.write(
						bufferedWriter,
						"import com.liferay.portal.kernel.events." +
							"LifecycleEvent;",
						"import com.liferay.portal.kernel.events." +
							"ActionException;");
				}
			}
		}
	}

	protected File buildTemplateWithGradle(
			String template, String name, String... args)
		throws Exception {

		File destinationDir = temporaryFolder.newFolder("gradle");

		return buildTemplateWithGradle(destinationDir, template, name, args);
	}

	protected File buildTemplateWithMaven(
			String template, String name, String groupId, String... args)
		throws Exception {

		File destinationDir = temporaryFolder.newFolder("maven");

		return buildTemplateWithMaven(
			destinationDir, destinationDir, template, name, groupId, args);
	}

	protected File buildWorkspace() throws Exception {
		File destinationDir = temporaryFolder.newFolder("workspace");

		return buildTemplateWithGradle(
			destinationDir, WorkspaceUtil.WORKSPACE, "test-workspace");
	}

	protected void testBuildTemplateNpm(
			String template, String name, String packageName, String className,
			String bootstrapRequire)
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(template, name);

		testNotContains(
			gradleProjectDir, "src/main/resources/META-INF/resources/init.jsp",
			"<%@ page import=\"" + packageName + ".constants." + className +
				"WebKeys\" %>");
		testNotContains(
			gradleProjectDir, "src/main/resources/META-INF/resources/view.jsp",
			"<aui:script require=\"<%= bootstrapRequire %>\">",
			bootstrapRequire);

		String packagePath = packageName.replaceAll("\\.", "\\/");

		testNotContains(
			gradleProjectDir,
			"src/main/java/" + packagePath + "/portlet/" + className +
				"Portlet.java",
			"import " + packageName + ".constants." + className + "WebKeys;");

		testNotExists(
			gradleProjectDir,
			"src/main/java/" + packagePath + "/constants/" + className +
				"WebKeys.java");

		File mavenProjectDir = buildTemplateWithMaven(
			template, name, "com.test", "-DclassName=" + className,
			"-Dpackage=" + packageName);

		testContains(
			mavenProjectDir, "package.json",
			"target/classes/META-INF/resources");

		testNotContains(
			mavenProjectDir, "package.json",
			"build/resources/main/META-INF/resources");

		testContains(
			mavenProjectDir, ".npmbundlerrc",
			"target/classes/META-INF/resources");

		if (Validator.isNotNull(System.getenv("JENKINS_HOME"))) {
			addNpmrc(gradleProjectDir);
			addNpmrc(mavenProjectDir);
			configureExecuteNpmTask(gradleProjectDir);
			configurePomNpmConfiguration(mavenProjectDir);
		}

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	protected void testBuildTemplateNpm71(
			String template, String name, String packageName, String className,
			String bootstrapRequire)
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(
			template, name, "--liferayVersion", "7.1");

		String packagePath = packageName.replaceAll("\\.", "\\/");

		testContains(
			gradleProjectDir, "build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"3.0.0");

		testContains(
			gradleProjectDir, "package.json",
			"build/resources/main/META-INF/resources");

		testNotContains(
			gradleProjectDir, "package.json",
			"target/classes/META-INF/resources");

		testNotContains(
			gradleProjectDir, ".npmbundlerrc",
			"target/classes/META-INF/resources");

		File mavenProjectDir = buildTemplateWithMaven(
			template, name, "com.test", "-DclassName=" + className,
			"-Dpackage=" + packageName, "-DliferayVersion=7.1");

		testContains(
			mavenProjectDir, "bnd.bnd", "-contract: JavaPortlet,JavaServlet");

		testContains(
			mavenProjectDir, "package.json",
			"target/classes/META-INF/resources");

		testNotContains(
			mavenProjectDir, "package.json",
			"build/resources/main/META-INF/resources");

		testContains(
			mavenProjectDir, ".npmbundlerrc",
			"target/classes/META-INF/resources");

		testContains(
			gradleProjectDir, "src/main/resources/META-INF/resources/init.jsp",
			"<%@ page import=\"" + packageName + ".constants." + className +
				"WebKeys\" %>");
		testContains(
			gradleProjectDir, "src/main/resources/META-INF/resources/view.jsp",
			"<aui:script require=\"<%= bootstrapRequire %>\">",
			bootstrapRequire);

		testContains(
			gradleProjectDir,
			"src/main/java/" + packagePath + "/portlet/" + className +
				"Portlet.java",
			"import " + packageName + ".constants." + className + "WebKeys;");

		testExists(
			gradleProjectDir,
			"src/main/java/" + packagePath + "/constants/" + className +
				"WebKeys.java");

		if (Validator.isNotNull(System.getenv("JENKINS_HOME"))) {
			addNpmrc(gradleProjectDir);
			addNpmrc(mavenProjectDir);
			configureExecuteNpmTask(gradleProjectDir);
			configurePomNpmConfiguration(mavenProjectDir);
		}

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	protected File testBuildTemplatePortlet(
			String template, String portletClassName,
			String... resourceFileNames)
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(template, "foo");

		for (String resourceFileName : resourceFileNames) {
			testExists(
				gradleProjectDir, "src/main/resources/" + resourceFileName);
		}

		testContains(
			gradleProjectDir, "bnd.bnd", "Export-Package: foo.constants");
		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"");
		testContains(
			gradleProjectDir, "src/main/java/foo/constants/FooPortletKeys.java",
			"public class FooPortletKeys");
		testContains(
			gradleProjectDir, "src/main/java/foo/portlet/FooPortlet.java",
			"javax.portlet.name=\" + FooPortletKeys.Foo",
			"public class FooPortlet extends " + portletClassName + " {");

		File mavenProjectDir = buildTemplateWithMaven(
			template, "foo", "com.test", "-DclassName=Foo", "-Dpackage=foo");

		buildProjects(gradleProjectDir, mavenProjectDir);

		return gradleProjectDir;
	}

	protected File testBuildTemplatePortlet71(
			String template, String portletClassName,
			String... resourceFileNames)
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(
			template, "foo", "--liferayVersion", "7.1");

		for (String resourceFileName : resourceFileNames) {
			testExists(
				gradleProjectDir, "src/main/resources/" + resourceFileName);
		}

		testContains(
			gradleProjectDir, "bnd.bnd", "Export-Package: foo.constants");
		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"",
			"name: \"com.liferay.portal.kernel\", version: \"3.0.0");
		testContains(
			gradleProjectDir, "src/main/java/foo/constants/FooPortletKeys.java",
			"public class FooPortletKeys");
		testContains(
			gradleProjectDir, "src/main/java/foo/portlet/FooPortlet.java",
			"javax.portlet.name=\" + FooPortletKeys.Foo",
			"public class FooPortlet extends " + portletClassName + " {");

		File mavenProjectDir = buildTemplateWithMaven(
			template, "foo", "com.test", "-DclassName=Foo", "-Dpackage=foo",
			"-DliferayVersion=7.1");

		testContains(
			mavenProjectDir, "bnd.bnd", "-contract: JavaPortlet,JavaServlet");

		buildProjects(gradleProjectDir, mavenProjectDir);

		return gradleProjectDir;
	}

	protected File testBuildTemplatePortletWithPackage(
			String template, String portletClassName,
			String... resourceFileNames)
		throws Exception, IOException {

		File gradleProjectDir = buildTemplateWithGradle(
			template, "foo", "--package-name", "com.liferay.test");

		testExists(gradleProjectDir, "bnd.bnd");

		for (String resourceFileName : resourceFileNames) {
			testExists(
				gradleProjectDir, "src/main/resources/" + resourceFileName);
		}

		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"");
		testContains(
			gradleProjectDir,
			"src/main/java/com/liferay/test/portlet/FooPortlet.java",
			"public class FooPortlet extends " + portletClassName + " {");

		File mavenProjectDir = buildTemplateWithMaven(
			template, "foo", "com.test", "-DclassName=Foo",
			"-Dpackage=com.liferay.test");

		buildProjects(gradleProjectDir, mavenProjectDir);

		return gradleProjectDir;
	}

	protected File testBuildTemplatePortletWithPortletName(
			String template, String portletClassName,
			String... resourceFileNames)
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(template, "portlet");

		testExists(gradleProjectDir, "bnd.bnd");

		for (String resourceFileName : resourceFileNames) {
			testExists(
				gradleProjectDir, "src/main/resources/" + resourceFileName);
		}

		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"");
		testContains(
			gradleProjectDir,
			"src/main/java/portlet/portlet/PortletPortlet.java",
			"public class PortletPortlet extends " + portletClassName + " {");

		File mavenProjectDir = buildTemplateWithMaven(
			template, "portlet", "com.test", "-DclassName=Portlet",
			"-Dpackage=portlet");

		buildProjects(gradleProjectDir, mavenProjectDir);

		return gradleProjectDir;
	}

	protected File testBuildTemplatePortletWithPortletSuffix(
			String template, String portletClassName,
			String... resourceFileNames)
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(
			template, "portlet-portlet");

		testExists(gradleProjectDir, "bnd.bnd");

		for (String resourceFileName : resourceFileNames) {
			testExists(
				gradleProjectDir, "src/main/resources/" + resourceFileName);
		}

		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"");
		testContains(
			gradleProjectDir,
			"src/main/java/portlet/portlet/portlet/PortletPortlet.java",
			"public class PortletPortlet extends " + portletClassName + " {");

		File mavenProjectDir = buildTemplateWithMaven(
			template, "portlet-portlet", "com.test", "-DclassName=Portlet",
			"-Dpackage=portlet.portlet");

		buildProjects(gradleProjectDir, mavenProjectDir);

		return gradleProjectDir;
	}

	protected void testBuildTemplateProjectWarInWorkspace(
			String template, String name, String warFileName)
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(template, name);

		testContains(
			gradleProjectDir, "build.gradle", "buildscript {",
			"apply plugin: \"war\"", "repositories {");

		File workspaceDir = buildWorkspace();

		File warsDir = new File(workspaceDir, "wars");

		File workspaceProjectDir = buildTemplateWithGradle(
			warsDir, template, name);

		testNotContains(
			workspaceProjectDir, "build.gradle", "apply plugin: \"war\"");
		testNotContains(
			workspaceProjectDir, "build.gradle", true, "^repositories \\{.*");

		executeGradle(gradleProjectDir, GRADLE_TASK_PATH_BUILD);

		File gradleWarFile = testExists(
			gradleProjectDir, "build/libs/" + warFileName + ".war");

		executeGradle(workspaceDir, ":wars:" + name + ":build");

		File workspaceWarFile = testExists(
			workspaceProjectDir, "build/libs/" + warFileName + ".war");

		testWarsDiff(gradleWarFile, workspaceWarFile);
	}

	protected void testBuildTemplateServiceBuilder(
			File gradleProjectDir, File mavenProjectDir, final File rootProject,
			String name, String packageName, final String projectPath)
		throws Exception {

		String apiProjectName = name + "-api";
		final String serviceProjectName = name + "-service";

		boolean workspace = WorkspaceUtil.isWorkspace(gradleProjectDir);

		if (!workspace) {
			testContains(
				gradleProjectDir, "settings.gradle",
				"include \"" + apiProjectName + "\", \"" + serviceProjectName +
					"\"");
		}

		testContains(
			gradleProjectDir, apiProjectName + "/bnd.bnd", "Export-Package:\\",
			packageName + ".exception,\\", packageName + ".model,\\",
			packageName + ".service,\\", packageName + ".service.persistence");

		testContains(
			gradleProjectDir, serviceProjectName + "/bnd.bnd",
			"Liferay-Service: true");

		if (!workspace) {
			testContains(
				gradleProjectDir, serviceProjectName + "/build.gradle",
				"compileOnly project(\":" + apiProjectName + "\")");
		}

		_testChangePortletModelHintsXml(
			gradleProjectDir, serviceProjectName,
			new Callable<Void>() {

				@Override
				public Void call() throws Exception {
					executeGradle(
						rootProject,
						projectPath + ":" + serviceProjectName +
							_GRADLE_TASK_PATH_BUILD_SERVICE);

					return null;
				}

			});

		executeGradle(
			rootProject,
			projectPath + ":" + serviceProjectName + GRADLE_TASK_PATH_BUILD);

		File gradleApiBundleFile = testExists(
			gradleProjectDir,
			apiProjectName + "/build/libs/" + packageName + ".api-1.0.0.jar");

		File gradleServiceBundleFile = testExists(
			gradleProjectDir,
			serviceProjectName + "/build/libs/" + packageName +
				".service-1.0.0.jar");

		_testChangePortletModelHintsXml(
			mavenProjectDir, serviceProjectName,
			new Callable<Void>() {

				@Override
				public Void call() throws Exception {
					executeMaven(
						new File(mavenProjectDir, serviceProjectName),
						MAVEN_GOAL_BUILD_SERVICE);

					return null;
				}

			});

		File gradleServicePropertiesFile = new File(
			gradleProjectDir,
			serviceProjectName + "/src/main/resources/service.properties");

		File mavenServicePropertiesFile = new File(
			mavenProjectDir,
			serviceProjectName + "/src/main/resources/service.properties");

		Files.copy(
			gradleServicePropertiesFile.toPath(),
			mavenServicePropertiesFile.toPath(),
			StandardCopyOption.REPLACE_EXISTING);

		executeMaven(mavenProjectDir, MAVEN_GOAL_PACKAGE);

		File mavenApiBundleFile = testExists(
			mavenProjectDir,
			apiProjectName + "/target/" + name + "-api-1.0.0.jar");
		File mavenServiceBundleFile = testExists(
			mavenProjectDir,
			serviceProjectName + "/target/" + name + "-service-1.0.0.jar");

		_testBundlesDiff(gradleApiBundleFile, mavenApiBundleFile);
		_testBundlesDiff(gradleServiceBundleFile, mavenServiceBundleFile);
	}

	protected void testBuildTemplateWithWorkspace(
			String template, String name, String jarFilePath)
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(template, name);

		testContains(
			gradleProjectDir, "build.gradle", true, ".*^buildscript \\{.*",
			".*^repositories \\{.*");

		File workspaceDir = buildWorkspace();

		File modulesDir = new File(workspaceDir, "modules");

		File workspaceProjectDir = buildTemplateWithGradle(
			modulesDir, template, name);

		testNotContains(
			workspaceProjectDir, "build.gradle", true, "^repositories \\{.*");

		executeGradle(gradleProjectDir, GRADLE_TASK_PATH_BUILD);

		testExists(gradleProjectDir, jarFilePath);

		executeGradle(workspaceDir, ":modules:" + name + ":build");

		testExists(workspaceProjectDir, jarFilePath);
	}

	protected void testPomXmlContainsDependency(
			Path pomXmlPath, String groupId, String artifactId, String version)
		throws Exception {

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		Assert.assertTrue("Missing " + pomXmlPath, Files.exists(pomXmlPath));

		Document document = documentBuilder.parse(pomXmlPath.toFile());

		Element projectElement = document.getDocumentElement();

		Element dependenciesElement = XMLTestUtil.getChildElement(
			projectElement, "dependencies");

		List<Element> dependencyElements;

		if (dependenciesElement != null) {
			dependencyElements = XMLTestUtil.getChildElements(
				dependenciesElement);
		}
		else {
			dependencyElements = Collections.emptyList();
		}

		boolean foundDependency = false;

		for (Element dependencyElement : dependencyElements) {
			String dependencyElementString = XMLTestUtil.toString(
				dependencyElement);

			String artifactIdString = String.format(
				"<artifactId>%s</artifactId>", artifactId);
			String groupIdString = String.format(
				"<groupId>%s</groupId>", groupId);
			String versionString = String.format(
				"<version>%s</version>", version);

			if (dependencyElementString.contains(artifactIdString) &&
				dependencyElementString.contains(groupIdString) &&
				dependencyElementString.contains(versionString)) {

				foundDependency = true;

				break;
			}
		}

		String missingDependencyString = String.format(
			"Missing dependency %s:%s:%s in %s", groupId, artifactId, version,
			pomXmlPath);

		Assert.assertTrue(missingDependencyString, foundDependency);
	}

	protected static final String DEPENDENCY_PORTAL_KERNEL =
		"compileOnly group: \"com.liferay.portal\", name: " +
			"\"com.liferay.portal.kernel\"";

	protected static final String FREEMARKER_PORTLET_VIEW_FTL_PREFIX =
		"<#include \"init.ftl\">";

	protected static final String GRADLE_TASK_PATH_BUILD = ":build";

	protected static final String GRADLE_TASK_PATH_DEPLOY = ":deploy";

	protected static final String MAVEN_GOAL_BUILD_SERVICE =
		"service-builder:build";

	protected static final String MAVEN_GOAL_PACKAGE = "package";

	protected static final String[] SPRING_MVC_PORTLET_JAR_NAMES = {
		"aop", "beans", "context", "core", "expression", "web", "webmvc",
		"webmvc-portlet"
	};

	protected static final String SPRING_MVC_PORTLET_VERSION = "4.1.9.RELEASE";

	protected static final Pattern gradlePluginVersionPattern = Pattern.compile(
		".*com\\.liferay\\.gradle\\.plugins:([0-9]+\\.[0-9]+\\.[0-9]+).*",
		Pattern.DOTALL | Pattern.MULTILINE);
	protected static final Pattern serviceBuilderVersionPattern =
		Pattern.compile(
			".*service\\.builder:([0-9]+\\.[0-9]+\\.[0-9]+).*",
			Pattern.DOTALL | Pattern.MULTILINE);

	private static void _addNexusRepositoriesElement(
		Document document, String parentElementName, String elementName) {

		Element projectElement = document.getDocumentElement();

		Element repositoriesElement = XMLTestUtil.getChildElement(
			projectElement, parentElementName);

		if (repositoriesElement == null) {
			repositoriesElement = document.createElement(parentElementName);

			projectElement.appendChild(repositoriesElement);
		}

		Element repositoryElement = document.createElement(elementName);

		Element idElement = document.createElement("id");

		idElement.appendChild(
			document.createTextNode(System.currentTimeMillis() + ""));

		Element urlElement = document.createElement("url");

		Text urlText = null;

		String repositoryUrl = mavenExecutor.getRepositoryUrl();

		if (Validator.isNotNull(repositoryUrl)) {
			urlText = document.createTextNode(repositoryUrl);
		}
		else {
			urlText = document.createTextNode(_REPOSITORY_CDN_URL);
		}

		urlElement.appendChild(urlText);

		repositoryElement.appendChild(idElement);
		repositoryElement.appendChild(urlElement);

		repositoriesElement.appendChild(repositoryElement);
	}

	private static void _buildProjects(
			File gradleProjectDir, File mavenProjectDir, File gradleOutputDir,
			File mavenOutputDir, String... gradleTaskPath)
		throws Exception {

		executeGradle(gradleProjectDir, gradleTaskPath);

		Path gradleOutputPath = FileTestUtil.getFile(
			gradleOutputDir.toPath(), _OUTPUT_FILENAME_GLOB_REGEX, 1);

		Assert.assertNotNull(gradleOutputPath);

		Assert.assertTrue(Files.exists(gradleOutputPath));

		File gradleOutputFile = gradleOutputPath.toFile();

		String gradleOutputFileName = gradleOutputFile.getName();

		executeMaven(mavenProjectDir, MAVEN_GOAL_PACKAGE);

		Path mavenOutputPath = FileTestUtil.getFile(
			mavenOutputDir.toPath(), _OUTPUT_FILENAME_GLOB_REGEX, 1);

		Assert.assertNotNull(mavenOutputPath);

		Assert.assertTrue(Files.exists(mavenOutputPath));

		File mavenOutputFile = mavenOutputPath.toFile();

		String mavenOutputFileName = mavenOutputFile.getName();

		try {
			if (gradleOutputFileName.endsWith(".jar")) {
				_testBundlesDiff(gradleOutputFile, mavenOutputFile);
			}
			else if (gradleOutputFileName.endsWith(".war")) {
				testWarsDiff(gradleOutputFile, mavenOutputFile);
			}
		}
		catch (Throwable t) {
			if (_TEST_DEBUG_BUNDLE_DIFFS) {
				Path dirPath = Paths.get("build");

				Files.copy(
					gradleOutputFile.toPath(),
					dirPath.resolve(gradleOutputFileName));
				Files.copy(
					mavenOutputFile.toPath(),
					dirPath.resolve(mavenOutputFileName));
			}

			throw t;
		}
	}

	private static void _testArchetyper(
			File parentDir, File destinationDir, File projectDir, String name,
			String groupId, String template, List<String> args)
		throws Exception {

		String author = System.getProperty("user.name");
		String className = name;
		String contributorType = null;
		String hostBundleSymbolicName = null;
		String hostBundleVersion = null;
		String packageName = name.replace('-', '.');
		String service = null;
		String version = "7.0";

		for (String arg : args) {
			int pos = arg.indexOf('=');

			if (pos == -1) {
				continue;
			}

			String key = arg.substring(2, pos);
			String value = arg.substring(pos + 1);

			if (key.equals("author")) {
				author = value;
			}
			else if (key.equals("className")) {
				className = value;
			}
			else if (key.equals("contributorType")) {
				contributorType = value;
			}
			else if (key.equals("hostBundleSymbolicName")) {
				hostBundleSymbolicName = value;
			}
			else if (key.equals("hostBundleVersion")) {
				hostBundleVersion = value;
			}
			else if (key.equals("package")) {
				packageName = value;
			}
			else if (key.equals("serviceClass")) {
				service = value;
			}
			else if (key.equals("serviceWrapperClass")) {
				service = value;
			}
			else if (key.equals("liferayVersion")) {
				version = value;
			}
		}

		File archetypesDir = FileUtil.getJarFile(ProjectTemplatesTest.class);

		File archetyperDestinationDir = null;

		if (parentDir.equals(destinationDir)) {
			archetyperDestinationDir = new File(
				destinationDir.getParentFile(), "archetyper");
		}
		else {
			Path destinationDirPath = destinationDir.toPath();
			Path parentDirPath = parentDir.toPath();

			Path archetyperPath = parentDirPath.resolveSibling("archetyper");
			Path relativePath = parentDirPath.relativize(destinationDirPath);

			Path archetyperDestinationPath = archetyperPath.resolve(
				relativePath);

			archetyperDestinationDir = archetyperDestinationPath.toFile();
		}

		ProjectTemplatesArgs projectTemplatesArgs = new ProjectTemplatesArgs();

		List<File> archetypesDirs = Arrays.asList(archetypesDir);

		projectTemplatesArgs.setArchetypesDirs(archetypesDirs);

		projectTemplatesArgs.setAuthor(author);
		projectTemplatesArgs.setClassName(className);
		projectTemplatesArgs.setContributorType(contributorType);
		projectTemplatesArgs.setDestinationDir(archetyperDestinationDir);
		projectTemplatesArgs.setGradle(false);
		projectTemplatesArgs.setGroupId(groupId);
		projectTemplatesArgs.setHostBundleSymbolicName(hostBundleSymbolicName);
		projectTemplatesArgs.setHostBundleVersion(hostBundleVersion);
		projectTemplatesArgs.setLiferayVersion(version);
		projectTemplatesArgs.setMaven(true);
		projectTemplatesArgs.setName(name);
		projectTemplatesArgs.setPackageName(packageName);
		projectTemplatesArgs.setService(service);
		projectTemplatesArgs.setTemplate(template);

		Archetyper archetyper = new Archetyper();

		archetyper.generateProject(
			projectTemplatesArgs, archetyperDestinationDir);

		File archetyperProjectDir = new File(archetyperDestinationDir, name);

		FileUtil.deleteFiles(archetyperDestinationDir.toPath(), "build.gradle");

		DirectoryComparator directoryComparator = new DirectoryComparator(
			projectDir, archetyperProjectDir);

		List<String> differences = directoryComparator.getDifferences();

		Assert.assertTrue(
			"Found differences " + differences, differences.isEmpty());
	}

	private static void _testBundlesDiff(File bundleFile1, File bundleFile2)
		throws Exception {

		PrintStream originalErrorStream = System.err;
		PrintStream originalOutputStream = System.out;

		originalErrorStream.flush();
		originalOutputStream.flush();

		ByteArrayOutputStream newErrorStream = new ByteArrayOutputStream();
		ByteArrayOutputStream newOutputStream = new ByteArrayOutputStream();

		System.setErr(new PrintStream(newErrorStream, true));
		System.setOut(new PrintStream(newOutputStream, true));

		try (bnd bnd = new bnd()) {
			String[] args = {
				"diff", "--ignore", _BUNDLES_DIFF_IGNORES,
				bundleFile1.getAbsolutePath(), bundleFile2.getAbsolutePath()
			};

			bnd.start(args);
		}
		finally {
			System.setErr(originalErrorStream);
			System.setOut(originalOutputStream);
		}

		String output = newErrorStream.toString();

		if (Validator.isNull(output)) {
			output = newOutputStream.toString();
		}

		Assert.assertEquals(
			"Bundle " + bundleFile1 + " and " + bundleFile2 + " do not match",
			"", output);
	}

	private static void _testChangePortletModelHintsXml(
			File projectDir, String serviceProjectName,
			Callable<Void> buildServiceCallable)
		throws Exception {

		buildServiceCallable.call();

		File file = testExists(
			projectDir,
			serviceProjectName +
				"/src/main/resources/META-INF/portlet-model-hints.xml");

		Path path = file.toPath();

		String content = FileUtil.read(path);

		String newContent = content.replace(
			"<field name=\"field5\" type=\"String\" />",
			"<field name=\"field5\" type=\"String\">\n\t\t\t<hint-collection " +
				"name=\"CLOB\" />\n\t\t</field>");

		Assert.assertNotEquals("Unexpected " + file, content, newContent);

		Files.write(path, newContent.getBytes(StandardCharsets.UTF_8));

		buildServiceCallable.call();

		Assert.assertEquals(
			"Changes in " + file + " incorrectly overridden", newContent,
			FileUtil.read(path));
	}

	private static File _testContainsOrNot(
			File dir, String fileName, boolean regex, boolean contains,
			String... strings)
		throws IOException {

		File file = testExists(dir, fileName);

		String content = FileUtil.read(file.toPath());

		for (String s : strings) {
			boolean found;

			if (regex) {
				Pattern pattern = Pattern.compile(
					s, Pattern.DOTALL | Pattern.MULTILINE);

				Matcher matcher = pattern.matcher(content);

				found = matcher.matches();
			}
			else {
				found = content.contains(s);
			}

			if (contains) {
				Assert.assertTrue("Not found in " + fileName + ": " + s, found);
			}
			else {
				Assert.assertFalse("Found in " + fileName + ": " + s, found);
			}
		}

		return file;
	}

	private static File _testExecutable(File dir, String fileName) {
		File file = testExists(dir, fileName);

		Assert.assertTrue(fileName + " is not executable", file.canExecute());

		return file;
	}

	private static final String _BUNDLES_DIFF_IGNORES = StringTestUtil.merge(
		Arrays.asList(
			"*.js.map", "*pom.properties", "*pom.xml", "*package.json",
			"Archiver-Version", "Build-Jdk", "Built-By", "Javac-Debug",
			"Javac-Deprecation", "Javac-Encoding"),
		',');

	private static final String _GRADLE_TASK_PATH_BUILD_SERVICE =
		":buildService";

	private static final String[] _GRADLE_WRAPPER_FILE_NAMES = {
		"gradlew", "gradlew.bat", "gradle/wrapper/gradle-wrapper.jar",
		"gradle/wrapper/gradle-wrapper.properties"
	};

	private static final String[] _MAVEN_WRAPPER_FILE_NAMES = {
		"mvnw", "mvnw.cmd", ".mvn/wrapper/maven-wrapper.jar",
		".mvn/wrapper/maven-wrapper.properties"
	};

	private static final String _NODEJS_NPM_CI_REGISTRY = System.getProperty(
		"nodejs.npm.ci.registry");

	private static final String _NODEJS_NPM_CI_SASS_BINARY_SITE =
		System.getProperty("nodejs.npm.ci.sass.binary.site");

	private static final String _OUTPUT_FILENAME_GLOB_REGEX = "*.{jar,war}";

	private static final String _REPOSITORY_CDN_URL =
		"https://repository-cdn.liferay.com/nexus/content/groups/public";

	private static final boolean _TEST_DEBUG_BUNDLE_DIFFS = Boolean.getBoolean(
		"test.debug.bundle.diffs");

	private static URI _gradleDistribution;
	private static XPathExpression _pomXmlNpmInstallXPathExpression;
	private static Properties _projectTemplateVersions;

}