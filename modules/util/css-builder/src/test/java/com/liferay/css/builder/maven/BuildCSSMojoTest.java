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

package com.liferay.css.builder.maven;

import com.liferay.maven.executor.MavenExecutor;
import com.liferay.portal.kernel.util.StringUtil;

import java.io.File;
import java.io.IOException;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Christopher Bryan Boyd
 */
public class BuildCSSMojoTest {

	@ClassRule
	public static final MavenExecutor mavenExecutor = new MavenExecutor();

	@BeforeClass
	public static void setUpClass() throws IOException {
		final String mavenRepoArgument = String.format(
			"-Dmaven.repo.local=%s", _repoPath);
		final String mavenVersionArgument = String.format(
			"-Dcssbuilder.version=%s", _version);
		final String mavenRepoLocationArgument = String.format(
			"-Dcssbuilder.repolocation=%s", _repoPath);

		_fixedMavenCommand = new String[] {
			mavenRepoArgument, mavenRepoLocationArgument, mavenVersionArgument,
			"-f", _pomFixedName, _mavenGoalCssBuilder
		};

		_brokenMavenCommand =
			new String[] {"-f", _pomBrokenName, _mavenGoalCssBuilder};
	}

	@Before
	public void setUp() throws Exception {
		final Path tempCss = Paths.get(
			testCaseTemporaryFolder.getRoot().toString(), "css");
		final Path tempPomFixed = Paths.get(
			testCaseTemporaryFolder.getRoot().toString(), _pomFixedName);
		final Path tempPomBroken = Paths.get(
			testCaseTemporaryFolder.getRoot().toString(), _pomBrokenName);

		final Path tempScssFile = Paths.get(tempCss.toString(), "test.scss");

		Files.createDirectory(tempCss);

		Files.copy(
			Paths.get(_mainPomFolder, "css", "test.scss"), tempScssFile,
			StandardCopyOption.COPY_ATTRIBUTES,
			StandardCopyOption.REPLACE_EXISTING);

		Files.copy(
			_pomFixedPath.toPath(), tempPomFixed,
			StandardCopyOption.COPY_ATTRIBUTES,
			StandardCopyOption.REPLACE_EXISTING);

		Files.copy(
			_pomBrokenPath.toPath(), tempPomBroken,
			StandardCopyOption.COPY_ATTRIBUTES,
			StandardCopyOption.REPLACE_EXISTING);
	}

	@Test(expected = AssertionError.class)
	public void testBrokenPom() throws Exception {
		final boolean result = _runTest(_brokenMavenCommand);

		Assert.assertFalse(result);
	}

	@Test
	public void testFixedPom() throws Exception {
		final boolean result = _runTest(_fixedMavenCommand);

		Assert.assertFalse(result);
	}

	@Rule
	public final TemporaryFolder testCaseTemporaryFolder =
		new TemporaryFolder();

	private static final boolean _anyCssFilesInDirectory(final Path dirPath)
		throws IOException {

		boolean foundCssFile = false;

		try (final DirectoryStream<Path> directoryStream =
				Files.newDirectoryStream(dirPath)) {

			for (final Path path : directoryStream) {
				if (_isCSS(path)) {
					foundCssFile = true;
					break;
				}
			}
		}

		return foundCssFile;
	}

	private static final void _executeMaven(
			final Path projectDir, final String... args)
		throws Exception {

		final String[] completeArgs = new String[args.length + 1];

		completeArgs[0] = "--update-snapshots";

		System.arraycopy(args, 0, completeArgs, 1, args.length);

		final MavenExecutor.Result result = mavenExecutor.execute(
			projectDir.toFile(), args);

		Assert.assertEquals(result.output, 0, result.exitCode);
	}

	private static final boolean _isCSS(final Path path) {
		final String lowerCasePath = StringUtil.toLowerCase(path.toString());

		return lowerCasePath.endsWith(".css");
	}

	private boolean _runTest(final String[] mavenCommand) throws Exception {
		final Path tempPomFolder = testCaseTemporaryFolder.getRoot().toPath();

		_executeMaven(tempPomFolder, mavenCommand);

		final Path tempCss = Paths.get(tempPomFolder.toString(), "css");

		final boolean anyCssFilesInDirectory = _anyCssFilesInDirectory(tempCss);

		return anyCssFilesInDirectory;
	}

	private static String[] _brokenMavenCommand;
	private static String[] _fixedMavenCommand;
	private static final String _mainPomFolder =
		"./src/test/resources/com/liferay/css/builder/maven/dependencies";
	private static final String _mavenGoalCssBuilder = "css-builder:build";
	private static final String _pomBrokenName = "pom-broken.xml";
	private static final File _pomBrokenPath = Paths.get(
		_mainPomFolder, _pomBrokenName).toFile();
	private static final String _pomFixedName = "pom-fixed.xml";
	private static final File _pomFixedPath = Paths.get(
		_mainPomFolder, _pomFixedName).toFile();
	private static final String _repoPath = System.getProperty("repoPath");
	private static final String _version = System.getProperty("version");

}