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
		final String mavenRepoLocalArg = String.format(
			"-Dmaven.repo.local=%s", _testRepoPath);
		final String cssbuilderVersionArg = String.format(
			"-Dcssbuilder.version=%s", _version);
		final String testRepoPathArg = String.format(
			"-Dtest.repo.path=%s", _testRepoPath);

		_fixedMavenArgs = new String[] {
			mavenRepoLocalArg, testRepoPathArg, cssbuilderVersionArg, "-f",
			_pomFixedName, _mavenGoalCssBuilder
		};

		_brokenMavenArgs =
			new String[] {"-f", _pomBrokenName, _mavenGoalCssBuilder};
	}

	@Before
	public void setUp() throws Exception {
		_tempPomFolder = testCaseTemporaryFolder.getRoot();
		_tempCss = testCaseTemporaryFolder.newFolder("css");
		_tempPomFixed = testCaseTemporaryFolder.newFile(_pomFixedName);
		_tempPomBroken = testCaseTemporaryFolder.newFile(_pomBrokenName);

		_tempScssFile = new File(_tempCss, "test.scss");

		Files.copy(
			Paths.get(_mainPomFolder, "css", "test.scss"),
			_tempScssFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES,
			StandardCopyOption.REPLACE_EXISTING);

		Files.copy(
			_pomFixedPath.toPath(), _tempPomFixed.toPath(),
			StandardCopyOption.COPY_ATTRIBUTES,
			StandardCopyOption.REPLACE_EXISTING);

		Files.copy(
			_pomBrokenPath.toPath(), _tempPomBroken.toPath(),
			StandardCopyOption.COPY_ATTRIBUTES,
			StandardCopyOption.REPLACE_EXISTING);
	}

	@Test(expected = AssertionError.class)
	public void testBadPom() throws Exception {
		_executeMaven(_tempPomFolder.toPath(), _brokenMavenArgs);

		final boolean anyCssFilesInDirectory = _hasCssFilesInPath(
			_tempCss.toPath());

		Assert.assertFalse(anyCssFilesInDirectory);
	}

	@Test
	public void testFixedPom() throws Exception {
		_executeMaven(_tempPomFolder.toPath(), _fixedMavenArgs);

		final boolean hasCssFilesInDirectory = _hasCssFilesInPath(
			_tempCss.toPath());

		Assert.assertFalse(hasCssFilesInDirectory);
	}

	@Rule
	public final TemporaryFolder testCaseTemporaryFolder =
		new TemporaryFolder();

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

	private static final boolean _hasCssFilesInPath(final Path dirPath)
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

	private static final boolean _isCSS(final Path path) {
		final String lowerCasePath = StringUtil.toLowerCase(path.toString());

		return lowerCasePath.endsWith(".css");
	}

	private static String[] _brokenMavenArgs;
	private static String[] _fixedMavenArgs;
	private static final String _mainPomFolder =
		"./src/test/resources/com/liferay/css/builder/maven/dependencies";
	private static final String _mavenGoalCssBuilder = "css-builder:build";
	private static final String _pomBrokenName = "pom-broken.xml";
	private static final File _pomBrokenPath = Paths.get(
		_mainPomFolder, _pomBrokenName).toFile();
	private static final String _pomFixedName = "pom-fixed.xml";
	private static final File _pomFixedPath = Paths.get(
		_mainPomFolder, _pomFixedName).toFile();
	private static final String _testRepoPath = System.getProperty(
		"testRepoPath");
	private static final String _version = System.getProperty("version");

	private File _tempCss;
	private File _tempPomBroken;
	private File _tempPomFixed;
	private File _tempPomFolder;
	private File _tempScssFile;

}