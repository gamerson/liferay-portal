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
		final String mavenGoalCssBuilder = "css-builder:build";
		final String mavenRepoArgument = String.format(
			"-Dmaven.repo.local=%s", _repoPath);
		final String mavenVersionArgument = String.format(
			"-Dcssbuilder.version=%s", _version);
		final String mavenRepoLocationArgument = String.format(
			"-Dcssbuilder.repolocation=%s", _repoPath);

		_goodMavenCommand = new String[]
			{
				mavenRepoArgument, mavenRepoLocationArgument,
				mavenVersionArgument, "-f", _pomGoodName, mavenGoalCssBuilder
			};

		_badMavenCommand = new String[]
			{
				"-f", _pomBadName, mavenGoalCssBuilder
			};
	}

	@Before
	public void setUp() throws Exception
	{

		_tempPomFolder = testCaseTemporaryFolder.getRoot();
		_tempCss = testCaseTemporaryFolder.newFolder("css");
		_tempPomGood = testCaseTemporaryFolder.newFile("pom-good.xml");
		_tempPomBad = testCaseTemporaryFolder.newFile("pom-bad.xml");

		_tempScssFile = new File(_tempCss, "test.scss");

		Files.copy(
			Paths.get(_mainPomFolder, "css", "test.scss"),
			_tempScssFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES,
			StandardCopyOption.REPLACE_EXISTING);

		Files.copy(
			_pomGoodPath.toPath(), _tempPomGood.toPath(),
			StandardCopyOption.COPY_ATTRIBUTES,
			StandardCopyOption.REPLACE_EXISTING);

		Files.copy(
			_pomBadPath.toPath(), _tempPomBad.toPath(),
			StandardCopyOption.COPY_ATTRIBUTES,
			StandardCopyOption.REPLACE_EXISTING);
	}

	@Test(expected = AssertionError.class)
	public void testBadPom() throws Exception {
		_executeMaven(_tempPomFolder.toPath(), _badMavenCommand);

		final boolean anyCssFilesInDirectory = _anyCssFilesInDirectory(
			_tempCss.toPath());

		Assert.assertFalse(anyCssFilesInDirectory);
	}

	@Test
	public void testGoodPom() throws Exception {
		System.out.println("Before execute maven");
		_executeMaven(_tempPomFolder.toPath(), _goodMavenCommand);
		System.out.println("After execute maven");
		final boolean anyCssFilesInDirectory = _anyCssFilesInDirectory(
			_tempCss.toPath());

		Assert.assertFalse(anyCssFilesInDirectory);
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

	private static String[] _badMavenCommand;
	private static String[] _goodMavenCommand;
	private static final String _mainPomFolder =
		"./src/test/resources/com/liferay/css/builder/maven/dependencies";
	private static final String _pomBadName = "pom-bad.xml";
	private static final File _pomBadPath = Paths.get(
		_mainPomFolder, _pomBadName).toFile();
	private static final String _pomGoodName = "pom-good.xml";
	private static final File _pomGoodPath = Paths.get(
		_mainPomFolder, _pomGoodName).toFile();
	private static final String _repoPath = System.getProperty("repoPath");
	private static final String _version = System.getProperty("version");

	private File _tempCss;
	private File _tempPomBad;
	private File _tempPomFolder;
	private File _tempPomGood;
	private File _tempScssFile;

}