/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.overlay;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;

/**
 * The files a Liferay Workspace adds to Liferay, laid out the way the Liferay
 * Helm chart mounts them: as a tree under the Liferay home directory.
 *
 * <p>
 * Module JARs land in osgi/modules, WARs in osgi/war, and client extension
 * ZIPs in osgi/client-extensions. The common, docker, and environment
 * configuration directories are merged in that order, so a later one replaces
 * a file of the same path from an earlier one. Their patching directory holds
 * the hotfix, and everything else in them is copied into the Liferay home
 * directory as is.
 * </p>
 *
 * <p>
 * Liferay's file install notices a changed bundle or configuration by its
 * name, size, and modification time, never by its content. A layer that fixes
 * every timestamp would therefore hide a rebuilt JAR of the same size, and one
 * that keeps the build's timestamps would change on every build even when no
 * file did. Each file's modification time is derived from its content instead,
 * so identical files always produce the same layer, and a changed file always
 * looks changed to Liferay.
 * </p>
 *
 * @author Gregory Amerson
 */
public class OverlayLayer {

	public static OverlayLayer collect(
			Iterable<File> clientExtensionFiles, File configsDir,
			Iterable<File> deployFiles, String environment)
		throws IOException {

		Map<String, File> files = new TreeMap<>();
		List<String> warnings = new ArrayList<>();

		for (File clientExtensionFile : clientExtensionFiles) {
			String name = clientExtensionFile.getName();

			if (name.endsWith(".zip")) {
				files.put(
					"osgi/client-extensions/" + name, clientExtensionFile);
			}
			else {
				warnings.add(
					"Skipping " + clientExtensionFile +
						" because it is not a client extension ZIP");
			}
		}

		for (File deployFile : deployFiles) {
			String name = deployFile.getName();

			if (name.endsWith(".jar")) {
				files.put("osgi/modules/" + name, deployFile);
			}
			else if (name.endsWith(".war")) {
				files.put("osgi/war/" + name, deployFile);
			}
			else {
				warnings.add(
					"Skipping " + deployFile +
						" because it is not a JAR or a WAR");
			}
		}

		if (configsDir != null) {
			for (String configDirName :
					new String[] {"common", "docker", environment}) {

				_collectConfigs(
					new File(configsDir, configDirName), files, warnings);
			}
		}

		return new OverlayLayer(files, warnings);
	}

	public Map<String, File> getFiles() {
		return Collections.unmodifiableMap(_files);
	}

	public List<String> getWarnings() {
		return Collections.unmodifiableList(_warnings);
	}

	/**
	 * Writes the layer as an uncompressed tar and returns its SHA-256 digest,
	 * which is the layer's diff ID. Files are streamed rather than held in
	 * memory, since a hotfix alone can be hundreds of megabytes.
	 */
	public String writeTar(Path tarPath) throws IOException {
		MessageDigest messageDigest = _getMessageDigest();

		Set<String> directories = new TreeSet<>();

		for (String path : _files.keySet()) {
			for (int index = path.indexOf('/'); index != -1;
				 index = path.indexOf('/', index + 1)) {

				directories.add(path.substring(0, index));
			}
		}

		try (OutputStream outputStream = Files.newOutputStream(tarPath);

			TarArchiveOutputStream tarArchiveOutputStream =
				new TarArchiveOutputStream(
					new DigestOutputStream(outputStream, messageDigest))) {

			tarArchiveOutputStream.setBigNumberMode(
				TarArchiveOutputStream.BIGNUMBER_POSIX);
			tarArchiveOutputStream.setLongFileMode(
				TarArchiveOutputStream.LONGFILE_POSIX);

			for (String directory : directories) {
				TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(
					directory + "/", TarConstants.LF_DIR);

				tarArchiveEntry.setModTime(0);
				tarArchiveEntry.setMode(TarArchiveEntry.DEFAULT_DIR_MODE);

				tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);

				tarArchiveOutputStream.closeArchiveEntry();
			}

			for (Map.Entry<String, File> entry : _files.entrySet()) {
				File file = entry.getValue();

				TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(
					entry.getKey(), TarConstants.LF_NORMAL);

				if (file.canExecute()) {
					tarArchiveEntry.setMode(0100755);
				}
				else {
					tarArchiveEntry.setMode(0100644);
				}

				tarArchiveEntry.setModTime(getModifiedTime(file) * 1000);
				tarArchiveEntry.setSize(file.length());

				tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);

				Files.copy(file.toPath(), tarArchiveOutputStream);

				tarArchiveOutputStream.closeArchiveEntry();
			}
		}

		HexFormat hexFormat = HexFormat.of();

		return "sha256:" + hexFormat.formatHex(messageDigest.digest());
	}

	/**
	 * Returns a modification time, in seconds since the epoch, that is a
	 * function of the file's content alone. It falls in the twenty years
	 * after 2000, which keeps every tool that reads it from treating it as
	 * implausible.
	 */
	protected static long getModifiedTime(File file) throws IOException {
		MessageDigest messageDigest = _getMessageDigest();

		try (InputStream inputStream = new DigestInputStream(
				Files.newInputStream(file.toPath()), messageDigest)) {

			byte[] buffer = new byte[8192];

			while (inputStream.read(buffer) != -1) {
			}
		}

		byte[] digest = messageDigest.digest();

		long value =
			((digest[0] & 0xffL) << 24) | ((digest[1] & 0xffL) << 16) |
			((digest[2] & 0xffL) << 8) | (digest[3] & 0xffL);

		return _MODIFIED_TIME_EPOCH + (value % _MODIFIED_TIME_RANGE);
	}

	private static void _collectConfigs(
			File configDir, Map<String, File> files, List<String> warnings)
		throws IOException {

		if (!configDir.isDirectory()) {
			return;
		}

		Path configDirPath = configDir.toPath();

		Files.walkFileTree(
			configDirPath,
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(
					Path path, BasicFileAttributes basicFileAttributes) {

					String name = String.valueOf(
						configDirPath.relativize(path));

					name = name.replace(File.separatorChar, '/');

					if (name.equals(".touch") || name.endsWith("/.touch")) {
						return FileVisitResult.CONTINUE;
					}

					if (name.startsWith("scripts/")) {
						warnings.add(
							"Skipping " + path + " because an overlay does " +
								"not run scripts");

						return FileVisitResult.CONTINUE;
					}

					files.put(name, path.toFile());

					return FileVisitResult.CONTINUE;
				}

			});
	}

	private static MessageDigest _getMessageDigest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException noSuchAlgorithmException) {
			throw new IllegalStateException(noSuchAlgorithmException);
		}
	}

	private OverlayLayer(Map<String, File> files, List<String> warnings) {
		_files = files;
		_warnings = warnings;
	}

	private static final long _MODIFIED_TIME_EPOCH = 946684800;

	private static final long _MODIFIED_TIME_RANGE = 20L * 365 * 24 * 60 * 60;

	private final Map<String, File> _files;
	private final List<String> _warnings;

}