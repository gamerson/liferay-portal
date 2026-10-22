/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;

import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

/**
 * @author Gregory Amerson
 */
public class ArtifactFile {

	public static ArtifactFile newDirectory(String name) {
		return new ArtifactFile(null, true, null, 0755, name);
	}

	public static ArtifactFile newFile(byte[] data, int mode, String name) {
		return new ArtifactFile(data, false, null, mode, name);
	}

	public static ArtifactFile newSymbolicLink(String linkTarget, String name) {
		return new ArtifactFile(null, false, linkTarget, 0777, name);
	}

	public static Map<String, ArtifactFile> readDirectory(Path rootPath)
		throws IOException {

		Map<String, ArtifactFile> artifactFiles = new TreeMap<>();

		Files.walkFileTree(
			rootPath,
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(
					Path dirPath, BasicFileAttributes basicFileAttributes) {

					String name = _getName(dirPath, rootPath);

					if (!name.isEmpty()) {
						artifactFiles.put(name, newDirectory(name));
					}

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					String name = _getName(path, rootPath);

					if (basicFileAttributes.isSymbolicLink()) {
						Path linkTargetPath = Files.readSymbolicLink(path);

						artifactFiles.put(
							name,
							newSymbolicLink(linkTargetPath.toString(), name));

						return FileVisitResult.CONTINUE;
					}

					artifactFiles.put(
						name,
						newFile(
							Files.readAllBytes(path), _getMode(path), name));

					return FileVisitResult.CONTINUE;
				}

			});

		return artifactFiles;
	}

	public static Map<String, ArtifactFile> readZip(File file)
		throws IOException {

		Map<String, ArtifactFile> artifactFiles = new TreeMap<>();

		try (ZipFile zipFile = new ZipFile(file)) {
			Enumeration<ZipArchiveEntry> enumeration = zipFile.getEntries();

			while (enumeration.hasMoreElements()) {
				ZipArchiveEntry zipArchiveEntry = enumeration.nextElement();

				String name = _trimTrailingSlash(zipArchiveEntry.getName());

				List<String> segments = List.of(name.split("/"));

				if (name.isEmpty() || name.startsWith("/") ||
					segments.contains("..")) {

					throw new IOException(
						"Zip entry " + zipArchiveEntry.getName() +
							" is not a valid path");
				}

				if (zipArchiveEntry.isDirectory()) {
					artifactFiles.put(name, newDirectory(name));

					continue;
				}

				byte[] data;

				try (InputStream inputStream = zipFile.getInputStream(
						zipArchiveEntry)) {

					data = inputStream.readAllBytes();
				}

				if (zipArchiveEntry.isUnixSymlink()) {
					artifactFiles.put(
						name,
						newSymbolicLink(
							new String(data, StandardCharsets.UTF_8), name));

					continue;
				}

				int mode = zipArchiveEntry.getUnixMode() & 0777;

				if (mode == 0) {
					mode = 0644;
				}

				artifactFiles.put(name, newFile(data, mode, name));
			}
		}

		return artifactFiles;
	}

	public static void writeDirectory(
			Map<String, ArtifactFile> artifactFiles, Path rootPath)
		throws IOException {

		for (ArtifactFile artifactFile : artifactFiles.values()) {
			if (artifactFile.isSymbolicLink()) {
				continue;
			}

			Path path = rootPath.resolve(artifactFile.getName());

			if (artifactFile.isDirectory()) {
				Files.createDirectories(path);

				continue;
			}

			Files.createDirectories(path.getParent());

			Files.write(path, artifactFile.getData());

			_setMode(artifactFile.getMode(), path);
		}

		for (ArtifactFile artifactFile : artifactFiles.values()) {
			if (!artifactFile.isSymbolicLink()) {
				continue;
			}

			Path path = rootPath.resolve(artifactFile.getName());

			Files.createDirectories(path.getParent());

			Files.createSymbolicLink(
				path, Path.of(artifactFile.getLinkTarget()));
		}
	}

	public byte[] getData() {
		return _data;
	}

	public String getLinkTarget() {
		return _linkTarget;
	}

	public int getMode() {
		return _mode;
	}

	public String getName() {
		return _name;
	}

	public boolean isDirectory() {
		return _directory;
	}

	public boolean isSymbolicLink() {
		if (_linkTarget != null) {
			return true;
		}

		return false;
	}

	private static int _getMode(Path path) throws IOException {
		try {
			Set<PosixFilePermission> posixFilePermissions =
				Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS);

			int mode = 0;

			for (Map.Entry<PosixFilePermission, Integer> entry :
					_posixFilePermissionModes.entrySet()) {

				if (!posixFilePermissions.contains(entry.getKey())) {
					continue;
				}

				mode |= entry.getValue();
			}

			return mode;
		}
		catch (UnsupportedOperationException unsupportedOperationException) {
			if (Files.isExecutable(path)) {
				return 0755;
			}

			return 0644;
		}
	}

	private static String _getName(Path path, Path rootPath) {
		String name = String.valueOf(rootPath.relativize(path));

		return name.replace(File.separatorChar, '/');
	}

	private static void _setMode(int mode, Path path) throws IOException {
		Set<PosixFilePermission> posixFilePermissions = EnumSet.noneOf(
			PosixFilePermission.class);

		for (Map.Entry<PosixFilePermission, Integer> entry :
				_posixFilePermissionModes.entrySet()) {

			if ((mode & entry.getValue()) == 0) {
				continue;
			}

			posixFilePermissions.add(entry.getKey());
		}

		try {
			Files.setPosixFilePermissions(path, posixFilePermissions);
		}
		catch (UnsupportedOperationException unsupportedOperationException) {
			File file = path.toFile();

			file.setExecutable((mode & 0111) != 0, false);
		}
	}

	private static String _trimTrailingSlash(String name) {
		while (name.endsWith("/")) {
			name = name.substring(0, name.length() - 1);
		}

		return name;
	}

	private ArtifactFile(
		byte[] data, boolean directory, String linkTarget, int mode,
		String name) {

		_data = data;
		_directory = directory;
		_linkTarget = linkTarget;
		_mode = mode;
		_name = name;
	}

	private static final Map<PosixFilePermission, Integer>
		_posixFilePermissionModes = Map.of(
			PosixFilePermission.OWNER_READ, 0400,
			PosixFilePermission.OWNER_WRITE, 0200,
			PosixFilePermission.OWNER_EXECUTE, 0100,
			PosixFilePermission.GROUP_READ, 0040,
			PosixFilePermission.GROUP_WRITE, 0020,
			PosixFilePermission.GROUP_EXECUTE, 0010,
			PosixFilePermission.OTHERS_READ, 0004,
			PosixFilePermission.OTHERS_WRITE, 0002,
			PosixFilePermission.OTHERS_EXECUTE, 0001);

	private final byte[] _data;
	private final boolean _directory;
	private final String _linkTarget;
	private final int _mode;
	private final String _name;

}