/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;

import org.gradle.api.GradleException;

/**
 * @author Gregory Amerson
 */
public class ArtifactLayer {

	/**
	 * Returns the layer for the Dockerfile's COPY instructions. The layer
	 * holds only the files they copy, so files the workload never sees, such
	 * as the Dockerfile and LCP.json beside a frontend's static files, are not
	 * published.
	 */
	public static ArtifactLayer plan(
		Map<String, ArtifactFile> artifactFiles,
		List<ClientExtensionDockerfile.Copy> copies) {

		Set<String> copiedDirectories = new TreeSet<>();
		Set<String> copiedFileNames = new TreeSet<>();
		Map<String, List<String[]>> fileDestinations = new TreeMap<>();
		List<ArtifactMount> mounts = new ArrayList<>();

		for (ClientExtensionDockerfile.Copy copy : copies) {
			String destination = copy.getDestination();

			for (String source : copy.getSources()) {
				source = _trimSlashes(source);

				if (source.equals(".")) {
					source = "";
				}

				if (_isDirectory(artifactFiles, source)) {
					copiedDirectories.add(source);

					mounts.add(
						new ArtifactMount(_getMountPath(destination), source));

					continue;
				}

				List<String> matches = new ArrayList<>();

				PathMatcher pathMatcher = FileSystems.getDefault(
				).getPathMatcher(
					"glob:" + source
				);

				for (ArtifactFile artifactFile : artifactFiles.values()) {
					if (artifactFile.isDirectory() ||
						!pathMatcher.matches(Path.of(artifactFile.getName()))) {

						continue;
					}

					matches.add(artifactFile.getName());
				}

				if (matches.isEmpty()) {
					throw new GradleException(
						"Dockerfile instruction \"COPY " + source +
							"\" matches nothing in the zip");
				}

				if ((matches.size() > 1) && !destination.endsWith("/")) {
					throw new GradleException(
						"Dockerfile instruction \"COPY " + source +
							"\" matches " + matches.size() +
								" files, so its destination must be a " +
									"directory ending in /");
				}

				copiedFileNames.addAll(matches);

				for (String match : matches) {
					String target = destination;

					if (target.endsWith("/")) {
						target = target + _getBaseName(match);
					}

					List<String[]> hardLinks = fileDestinations.computeIfAbsent(
						_getParentPath(target), key -> new ArrayList<>());

					hardLinks.add(new String[] {_getBaseName(target), match});
				}
			}
		}

		List<String[]> hardLinks = new ArrayList<>();
		int index = 0;

		for (Map.Entry<String, List<String[]>> entry :
				fileDestinations.entrySet()) {

			String mountDirectory = _MOUNTS_DIRECTORY + "/" + index++;

			for (String[] hardLink : entry.getValue()) {
				hardLinks.add(
					new String[] {
						mountDirectory + "/" + hardLink[0], hardLink[1]
					});
			}

			mounts.add(new ArtifactMount(entry.getKey(), mountDirectory));
		}

		Map<String, ArtifactFile> copiedArtifactFiles = new TreeMap<>();

		for (Map.Entry<String, ArtifactFile> entry : artifactFiles.entrySet()) {
			String name = entry.getKey();

			if (!copiedFileNames.contains(name) &&
				!_isInDirectory(copiedDirectories, name)) {

				continue;
			}

			copiedArtifactFiles.put(name, entry.getValue());
		}

		return new ArtifactLayer(copiedArtifactFiles, hardLinks, mounts);
	}

	public Map<String, ArtifactFile> getArtifactFiles() {
		return Collections.unmodifiableMap(_artifactFiles);
	}

	public List<ArtifactMount> getMounts() {
		return Collections.unmodifiableList(_mounts);
	}

	public byte[] toTar() throws IOException {
		Set<String> directories = new TreeSet<>();

		for (ArtifactFile artifactFile : _artifactFiles.values()) {
			if (artifactFile.isDirectory()) {
				directories.add(artifactFile.getName());
			}

			directories.addAll(_getParentDirectories(artifactFile.getName()));
		}

		for (String[] hardLink : _hardLinks) {
			directories.addAll(_getParentDirectories(hardLink[0]));
		}

		ByteArrayOutputStream byteArrayOutputStream =
			new ByteArrayOutputStream();

		try (TarArchiveOutputStream tarArchiveOutputStream =
				new TarArchiveOutputStream(byteArrayOutputStream)) {

			tarArchiveOutputStream.setBigNumberMode(
				TarArchiveOutputStream.BIGNUMBER_POSIX);
			tarArchiveOutputStream.setLongFileMode(
				TarArchiveOutputStream.LONGFILE_POSIX);

			for (String directory : directories) {
				_putEntry(
					tarArchiveOutputStream,
					_newTarArchiveEntry(
						0755, directory + "/", TarConstants.LF_DIR),
					null);
			}

			for (ArtifactFile artifactFile : _artifactFiles.values()) {
				if (artifactFile.isDirectory()) {
					continue;
				}

				if (artifactFile.isSymbolicLink()) {
					TarArchiveEntry tarArchiveEntry = _newTarArchiveEntry(
						0777, artifactFile.getName(), TarConstants.LF_SYMLINK);

					tarArchiveEntry.setLinkName(artifactFile.getLinkTarget());

					_putEntry(tarArchiveOutputStream, tarArchiveEntry, null);

					continue;
				}

				_putEntry(
					tarArchiveOutputStream,
					_newTarArchiveEntry(
						artifactFile.getMode(), artifactFile.getName(),
						TarConstants.LF_NORMAL),
					artifactFile.getData());
			}

			for (String[] hardLink : _hardLinks) {
				TarArchiveEntry tarArchiveEntry = _newTarArchiveEntry(
					0644, hardLink[0], TarConstants.LF_LINK);

				tarArchiveEntry.setLinkName(hardLink[1]);

				_putEntry(tarArchiveOutputStream, tarArchiveEntry, null);
			}
		}

		return byteArrayOutputStream.toByteArray();
	}

	private static String _getBaseName(String path) {
		int index = path.lastIndexOf('/');

		return path.substring(index + 1);
	}

	private static String _getMountPath(String destination) {
		destination = destination.replaceAll("/+$", "");

		if (destination.isEmpty()) {
			return "/";
		}

		return destination;
	}

	private static String _getParentPath(String path) {
		int index = path.lastIndexOf('/');

		if (index <= 0) {
			return "/";
		}

		return path.substring(0, index);
	}

	private static boolean _isDirectory(
		Map<String, ArtifactFile> artifactFiles, String name) {

		if (name.isEmpty()) {
			return true;
		}

		ArtifactFile artifactFile = artifactFiles.get(name);

		if (artifactFile != null) {
			return artifactFile.isDirectory();
		}

		for (String fileName : artifactFiles.keySet()) {
			if (fileName.startsWith(name + "/")) {
				return true;
			}
		}

		return false;
	}

	private static boolean _isInDirectory(
		Set<String> directories, String name) {

		for (String directory : directories) {
			if (directory.isEmpty() || name.equals(directory) ||
				name.startsWith(directory + "/")) {

				return true;
			}
		}

		return false;
	}

	private static String _trimSlashes(String path) {
		return path.replaceAll("^/+|/+$", "");
	}

	private ArtifactLayer(
		Map<String, ArtifactFile> artifactFiles, List<String[]> hardLinks,
		List<ArtifactMount> mounts) {

		_artifactFiles = new TreeMap<>(artifactFiles);
		_hardLinks = hardLinks;
		_mounts = mounts;
	}

	private List<String> _getParentDirectories(String name) {
		List<String> parentDirectories = new ArrayList<>();

		int index = name.lastIndexOf('/');

		while (index > 0) {
			name = name.substring(0, index);

			parentDirectories.add(name);

			index = name.lastIndexOf('/');
		}

		return parentDirectories;
	}

	private TarArchiveEntry _newTarArchiveEntry(
		int mode, String name, byte linkFlag) {

		TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(
			name, linkFlag, true);

		tarArchiveEntry.setGroupId(0);
		tarArchiveEntry.setGroupName("");
		tarArchiveEntry.setMode(mode);
		tarArchiveEntry.setModTime(0);
		tarArchiveEntry.setUserId(0);
		tarArchiveEntry.setUserName("");

		return tarArchiveEntry;
	}

	private void _putEntry(
			TarArchiveOutputStream tarArchiveOutputStream,
			TarArchiveEntry tarArchiveEntry, byte[] data)
		throws IOException {

		if (data != null) {
			tarArchiveEntry.setSize(data.length);
		}

		tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);

		if (data != null) {
			tarArchiveOutputStream.write(data);
		}

		tarArchiveOutputStream.closeArchiveEntry();
	}

	private static final String _MOUNTS_DIRECTORY = ".mounts";

	private final Map<String, ArtifactFile> _artifactFiles;
	private final List<String[]> _hardLinks;
	private final List<ArtifactMount> _mounts;

}