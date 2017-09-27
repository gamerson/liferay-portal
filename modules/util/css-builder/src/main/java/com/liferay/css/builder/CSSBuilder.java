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

package com.liferay.css.builder;

import com.beust.jcommander.IDefaultProvider;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.JCommander.Builder;
import com.beust.jcommander.Parameter;

import com.liferay.portal.kernel.regex.PatternFactory;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.tools.ArgumentsUtil;
import com.liferay.rtl.css.RTLCSSConverter;
import com.liferay.sass.compiler.SassCompiler;
import com.liferay.sass.compiler.SassCompilerException;
import com.liferay.sass.compiler.jni.internal.JniSassCompiler;
import com.liferay.sass.compiler.ruby.internal.RubySassCompiler;

import java.io.File;
import java.io.IOException;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tools.ant.DirectoryScanner;

/**
 * @author Brian Wing Shun Chan
 * @author Raymond Augé
 * @author Eduardo Lundgren
 * @author Shuyang Zhou
 * @author David Truong
 */
public class CSSBuilder implements AutoCloseable, IDefaultProvider {

	public static void main(String[] args) throws Exception {
		Map<String, String> argMap = new HashMap<>();

		for (int x = 0; x < args.length; x++) {
			if (args[x].indexOf('=') != args[x].length() - 1) {
				String[] arg = args[x].replace('=', ' ').split(" ");

				if (arg.length == 2) {
					if (arg[0].startsWith("sass.dir.")) {
						arg[0] = "sass.dir";
					}

					argMap.put(arg[0], arg[1]);
				}
			}
		}

		Collection<String> argsCollection = new ArrayList<>();

		for (Entry<String, String> entry : argMap.entrySet()) {
			argsCollection.add(entry.getKey());
			argsCollection.add(entry.getValue());
		}

		String[] argsArray = argsCollection.toArray(new String[0]);

		try (CSSBuilder cssBuilder = new CSSBuilder()) {
			Builder builder = JCommander.newBuilder();

			JCommander commander = builder.addObject(
				cssBuilder
			).defaultProvider(
				cssBuilder
			).build();

			commander.parse(argsArray);

			cssBuilder._init();
			cssBuilder.execute(cssBuilder._sassDirs);
		}
		catch (Exception e) {
			ArgumentsUtil.processMainException(argMap, e);
		}
	}

	public CSSBuilder() {
	}

	public CSSBuilder(
			boolean appendCssImportTimestamps, String docrootDirName,
			boolean generateSourceMap, String outputDirName,
			String portalCommonPath, int precision,
			String[] rtlExcludedPathRegexps, String sassCompilerClassName)
		throws Exception {

		File portalCommonDir = new File(portalCommonPath);

		if (portalCommonDir.isFile()) {
			portalCommonDir = _unzipPortalCommon(portalCommonDir);

			_cleanPortalCommonDir = true;
		}
		else {
			_cleanPortalCommonDir = false;
		}

		_appendCssImportTimestamps = appendCssImportTimestamps;
		_docrootDirName = docrootDirName;
		_generateSourceMap = generateSourceMap;
		_outputDirName = outputDirName;
		_portalCommonDirName = portalCommonDir.getCanonicalPath();
		_precision = precision;
		_rtlExcludedPathPatterns = PatternFactory.compile(
			rtlExcludedPathRegexps);

		_initSassCompiler(sassCompilerClassName);
	}

	public CSSBuilder(
			String docrootDirName, boolean generateSourceMap,
			String outputDirName, String portalCommonPath, int precision,
			String[] rtlExcludedPathRegexps, String sassCompilerClassName)
		throws Exception {

		this(
			true, docrootDirName, generateSourceMap, outputDirName,
			portalCommonPath, precision, rtlExcludedPathRegexps,
			sassCompilerClassName);
	}

	@Override
	public void close() throws Exception {
		if (_cleanPortalCommonDir) {
			_deltree(_portalCommonDirName);
		}

		_sassCompiler.close();
	}

	public void execute(List<String> dirNames) throws Exception {
		List<String> fileNames = new ArrayList<>();

		for (String dirName : dirNames) {
			_collectSassFiles(fileNames, dirName, _docrootDirName);
		}

		for (String fileName : fileNames) {
			long startTime = System.currentTimeMillis();

			_parseSassFile(fileName);

			System.out.println(
				"Parsed " + fileName + " in " +
					(System.currentTimeMillis() - startTime) + "ms");
		}
	}

	@Override
	public String getDefaultValueFor(String optionName) {
		if ("sass.dir".equals(optionName)) {
			return CSSBuilderArgs.DIR_NAME;
		}

		return null;
	}

	public boolean isRtlExcludedPath(String filePath) {
		for (Pattern pattern : _rtlExcludedPathPatterns) {
			Matcher matcher = pattern.matcher(filePath);

			if (matcher.matches()) {
				return true;
			}
		}

		return false;
	}

	private void _collectSassFiles(
			List<String> fileNames, String dirName, String docrootDirName)
		throws Exception {

		String basedir = docrootDirName.concat(dirName);

		String[] scssFiles = _getScssFiles(basedir);

		if (!_isModified(basedir, scssFiles)) {
			long oldestSassModifiedTime = _getOldestModifiedTime(
				basedir, scssFiles);

			String[] scssFragments = _getScssFragments(basedir);

			long newestFragmentModifiedTime = _getNewestModifiedTime(
				basedir, scssFragments);

			if (oldestSassModifiedTime > newestFragmentModifiedTime) {
				return;
			}
		}

		for (String fileName : scssFiles) {
			if (fileName.contains("_rtl")) {
				continue;
			}

			fileNames.add(_normalizeFileName(dirName, fileName));
		}
	}

	private void _deltree(String dirName) throws IOException {
		Files.walkFileTree(
			Paths.get(dirName),
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult postVisitDirectory(
						Path dirPath, IOException ioe)
					throws IOException {

					Files.delete(dirPath);

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					Files.delete(path);

					return FileVisitResult.CONTINUE;
				}

			});
	}

	private String[] _getFilesFromDirectory(
		String baseDir, String[] includes, String[] excludes) {

		DirectoryScanner directoryScanner = new DirectoryScanner();

		directoryScanner.setBasedir(baseDir);
		directoryScanner.setExcludes(excludes);
		directoryScanner.setIncludes(includes);

		directoryScanner.scan();

		return directoryScanner.getIncludedFiles();
	}

	private long _getLastModifiedTime(Path path) {
		try {
			FileTime fileTime = Files.getLastModifiedTime(path);

			return fileTime.toMillis();
		}
		catch (IOException ioe) {
			return -1;
		}
	}

	private long _getNewestModifiedTime(String baseDir, String[] fileNames) {
		Stream<String> stream = Stream.of(fileNames);

		return stream.map(
			fileName -> Paths.get(baseDir, fileName)
		).map(
			this::_getLastModifiedTime
		).max(
			Comparator.naturalOrder()
		).orElse(
			Long.MIN_VALUE
		);
	}

	private long _getOldestModifiedTime(String baseDir, String[] fileNames) {
		Stream<String> stream = Stream.of(fileNames);

		return stream.map(
			fileName -> Paths.get(baseDir, fileName)
		).map(
			this::_getLastModifiedTime
		).min(
			Comparator.naturalOrder()
		).orElse(
			Long.MIN_VALUE
		);
	}

	private String _getRtlCss(String fileName, String css) throws Exception {
		String rtlCss = css;

		try {
			if (_rtlCSSConverter == null) {
				_rtlCSSConverter = new RTLCSSConverter();
			}

			rtlCss = _rtlCSSConverter.process(rtlCss);
		}
		catch (Exception e) {
			System.out.println(
				"Unable to generate RTL version for " + fileName +
					StringPool.COMMA_AND_SPACE + e.getMessage());
		}

		return rtlCss;
	}

	private String[] _getScssFiles(String baseDir) {
		String[] fragments = {"**\\_*.scss"};
		String[] includes = {"**\\*.scss"};

		Stream<String[]> stream = Stream.of(fragments, _EXCLUDES);

		String[] excludes = stream.flatMap(
			Stream::of
		).toArray(
			String[]::new
		);

		return _getFilesFromDirectory(baseDir, includes, excludes);
	}

	private String[] _getScssFragments(String baseDir) {
		String[] includes = {"**\\\\_*.scss"};

		return _getFilesFromDirectory(baseDir, includes, _EXCLUDES);
	}

	private void _init() throws Exception {
		File portalCommonDir = new File(_portalCommonPath);

		if (portalCommonDir.isFile()) {
			portalCommonDir = _unzipPortalCommon(portalCommonDir);

			_cleanPortalCommonDir = true;
		}
		else {
			_cleanPortalCommonDir = false;
		}

		_portalCommonDirName = portalCommonDir.getCanonicalPath();

		if (_rtlExcludedPathRegexps != null) {
			final String[] rtlExcludedPathRegexpsArray =
				_rtlExcludedPathRegexps.split(",");

			_rtlExcludedPathPatterns = PatternFactory.compile(
				rtlExcludedPathRegexpsArray);
		}

		else
		{
			_rtlExcludedPathPatterns = new Pattern[0];
		}

		_initSassCompiler(_sassCompilerClassName);
	}

	private void _initSassCompiler(String sassCompilerClassName)
		throws Exception {

		if (Validator.isNull(sassCompilerClassName) ||
			sassCompilerClassName.equals("jni")) {

			try {
				System.setProperty("jna.nosys", Boolean.TRUE.toString());

				_sassCompiler = new JniSassCompiler(_precision);

				System.out.println("Using native Sass compiler");
			}
			catch (Throwable t) {
				System.out.println(
					"Unable to load native compiler, falling back to Ruby");

				_sassCompiler = new RubySassCompiler(_precision);
			}
		}
		else {
			try {
				_sassCompiler = new RubySassCompiler(_precision);

				System.out.println("Using Ruby Sass compiler");
			}
			catch (Exception e) {
				System.out.println(
					"Unable to load Ruby compiler, falling back to native");

				System.setProperty("jna.nosys", Boolean.TRUE.toString());

				_sassCompiler = new JniSassCompiler(_precision);
			}
		}
	}

	private boolean _isModified(String dirName, String[] fileNames)
		throws Exception {

		for (String fileName : fileNames) {
			if (fileName.contains("_rtl")) {
				continue;
			}

			fileName = _normalizeFileName(dirName, fileName);

			File file = new File(fileName);
			File cacheFile = CSSBuilderUtil.getOutputFile(
				fileName, _outputDirName);

			if (file.lastModified() != cacheFile.lastModified()) {
				return true;
			}
		}

		return false;
	}

	private String _normalizeFileName(String dirName, String fileName) {
		fileName = StringUtil.replace(
			dirName + StringPool.SLASH + fileName,
			new String[] {StringPool.BACK_SLASH, StringPool.DOUBLE_SLASH},
			new String[] {StringPool.SLASH, StringPool.SLASH});

		return fileName;
	}

	private String _parseSass(String fileName) throws SassCompilerException {
		String filePath = _docrootDirName.concat(fileName);

		String cssBasePath = filePath;

		int pos = filePath.lastIndexOf("/css/");

		if (pos >= 0) {
			cssBasePath = filePath.substring(0, pos + 4);
		}
		else {
			pos = filePath.lastIndexOf("/resources/");

			if (pos >= 0) {
				cssBasePath = filePath.substring(0, pos + 10);
			}
		}

		String css = _sassCompiler.compileFile(
			filePath, _portalCommonDirName + File.pathSeparator + cssBasePath,
			_generateSourceMap, filePath + ".map");

		return CSSBuilderUtil.parseStaticTokens(css);
	}

	private void _parseSassFile(String fileName) throws Exception {
		File file = new File(_docrootDirName, fileName);

		if (!file.exists()) {
			return;
		}

		String ltrContent = _parseSass(fileName);

		_writeOutputFile(fileName, ltrContent, false);

		if (isRtlExcludedPath(fileName)) {
			return;
		}

		String rtlContent = _getRtlCss(fileName, ltrContent);

		String rtlCustomFileName = CSSBuilderUtil.getRtlCustomFileName(
			fileName);

		File rtlCustomFile = new File(_docrootDirName, rtlCustomFileName);

		if (rtlCustomFile.exists()) {
			rtlContent += _parseSass(rtlCustomFileName);
		}

		_writeOutputFile(fileName, rtlContent, true);
	}

	private File _unzipPortalCommon(File portalCommonFile) throws IOException {
		Path portalCommonCssDirPath = Files.createTempDirectory(
			"portalCommonCss");

		try (ZipFile zipFile = new ZipFile(portalCommonFile)) {
			Enumeration<? extends ZipEntry> enumeration = zipFile.entries();

			while (enumeration.hasMoreElements()) {
				ZipEntry zipEntry = enumeration.nextElement();

				String name = zipEntry.getName();

				if (name.endsWith("/") ||
					!name.startsWith("META-INF/resources/")) {

					continue;
				}

				name = name.substring(19);

				Path path = portalCommonCssDirPath.resolve(name);

				Files.createDirectories(path.getParent());

				Files.copy(
					zipFile.getInputStream(zipEntry), path,
					StandardCopyOption.REPLACE_EXISTING);
			}
		}

		return portalCommonCssDirPath.toFile();
	}

	private void _write(File file, String content) throws Exception {
		File parentFile = file.getParentFile();

		if (!parentFile.exists()) {
			parentFile.mkdirs();
		}

		Path path = Paths.get(file.toURI());

		Files.write(path, content.getBytes(StringPool.UTF8));
	}

	private void _writeOutputFile(String fileName, String content, boolean rtl)
		throws Exception {

		if (_appendCssImportTimestamps) {
			content = CSSBuilderUtil.parseCSSImports(content);
		}

		String outputFileName;

		if (rtl) {
			String rtlFileName = CSSBuilderUtil.getRtlCustomFileName(fileName);

			outputFileName = CSSBuilderUtil.getOutputFileName(
				rtlFileName, _outputDirName, StringPool.BLANK);
		}
		else {
			outputFileName = CSSBuilderUtil.getOutputFileName(
				fileName, _outputDirName, StringPool.BLANK);
		}

		File outputFile = new File(_docrootDirName, outputFileName);

		_write(outputFile, content);

		File file = new File(_docrootDirName, fileName);

		outputFile.setLastModified(file.lastModified());
	}

	private static final String[] _EXCLUDES = {
		"**\\_diffs\\**", "**\\.sass-cache*\\**", "**\\.sass_cache_*\\**",
		"**\\_sass_cache_*\\**", "**\\_styled\\**", "**\\_unstyled\\**",
		"**\\css\\aui\\**", "**\\tmp\\**"
	};

	private static RTLCSSConverter _rtlCSSConverter;

	@Parameter(
		description = "Whether to append the current timestamp to the URLs in the @import CSS at-rules.",
		names = "sass.append.css.import.timestamps"
	)
	private boolean _appendCssImportTimestamps =
		CSSBuilderArgs.APPEND_CSS_IMPORT_TIMESTAMPS;

	private boolean _cleanPortalCommonDir;

	@Parameter(
		description = "If the java plugin is applied: The first resources directory of the main source set (by default: src/main/resources).\nIf the war plugin is applied: project.webAppDir.\nOtherwise: null",
		names = "sass.docroot.dir"
	)
	private String _docrootDirName = CSSBuilderArgs.DOCROOT_DIR_NAME;

	@Parameter(
		description = "Whether to generate source maps for easier debugging.",
		names = "sass.generate.source.map"
	)
	private boolean _generateSourceMap;

	@Parameter(
		description = "The name of the sub-directories where the SCSS files are compiled to. " +
			"For each directory that contains SCSS files, a sub-directory with this name is created. ",
		names = "sass.output.dir"
	)
	private String _outputDirName = CSSBuilderArgs.OUTPUT_DIR_NAME;

	@Parameter
	private List<String> _parameters = new ArrayList<>();

	private String _portalCommonDirName;

	@Parameter(
		description = "The value of the portalCommonDir property if set; otherwise portalCommonFile.",
		names = {"sass.portal.common.path", "sass.portal.common.dir"}
	)
	private String _portalCommonPath;

	@Parameter(
		description = "The numeric precision of numbers in Sass.",
		names = "sass.precision"
	)
	private Integer _precision = CSSBuilderArgs.PRECISION;

	private Pattern[] _rtlExcludedPathPatterns;

	@Parameter(
		description = "The SCSS file patterns to exclude when converting for right-to-left (RTL) support.",
		names = "sass.rtl.excluded.path.regexps"
	)
	private String _rtlExcludedPathRegexps;

	private SassCompiler _sassCompiler;

	@Parameter(
		description = "The type of Sass compiler to use. Supported values are \"jni\" and \"ruby\". If not set, defaults to \"jni\".",
		names = "sass.compiler.class.name"
	)
	private String _sassCompilerClassName;

	@Parameter(
		description = "The name of the directories, relative to docrootDir, which contain the SCSS files to compile. All sub-directories are searched for SCSS files as well.",
		names = "sass.dir"
	)
	private List<String> _sassDirs = new ArrayList<>();

}