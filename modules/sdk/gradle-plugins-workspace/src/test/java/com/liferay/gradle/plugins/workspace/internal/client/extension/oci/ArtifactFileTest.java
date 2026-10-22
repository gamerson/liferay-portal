/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import java.io.File;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Gregory Amerson
 */
public class ArtifactFileTest {

	@Test
	public void testReadZip() throws Exception {
		File file = temporaryFolder.newFile("able.zip");

		try (ZipArchiveOutputStream zipArchiveOutputStream =
				new ZipArchiveOutputStream(file)) {

			_putEntry(zipArchiveOutputStream, "a.txt", null, "a");
			_putEntry(zipArchiveOutputStream, "bin/", null, null);
			_putEntry(
				zipArchiveOutputStream, "bin/run.sh", UnixStat.FILE_FLAG | 0755,
				"#!/bin/sh");
			_putEntry(
				zipArchiveOutputStream, "link", UnixStat.LINK_FLAG | 0777,
				"a.txt");
		}

		Map<String, ArtifactFile> artifactFiles = ArtifactFile.readZip(file);

		Assert.assertEquals(
			List.of("a.txt", "bin", "bin/run.sh", "link"),
			new ArrayList<>(artifactFiles.keySet()));

		ArtifactFile artifactFile = artifactFiles.get("a.txt");

		Assert.assertEquals(0644, artifactFile.getMode());

		artifactFile = artifactFiles.get("bin");

		Assert.assertTrue(artifactFile.isDirectory());

		artifactFile = artifactFiles.get("bin/run.sh");

		Assert.assertEquals(0755, artifactFile.getMode());

		artifactFile = artifactFiles.get("link");

		Assert.assertEquals("a.txt", artifactFile.getLinkTarget());
	}

	@Test
	public void testReadZipWhenEntryPathIsInvalid() throws Exception {
		for (String name : List.of("../a.txt", "/a.txt", "a/../../b.txt")) {
			File file = temporaryFolder.newFile();

			try (ZipArchiveOutputStream zipArchiveOutputStream =
					new ZipArchiveOutputStream(file)) {

				_putEntry(zipArchiveOutputStream, name, null, "a");
			}

			try {
				ArtifactFile.readZip(file);

				Assert.fail(name);
			}
			catch (IOException ioException) {
				String message = ioException.getMessage();

				Assert.assertTrue(
					message, message.contains("is not a valid path"));
			}
		}
	}

	@Test
	public void testWriteDirectory() throws Exception {
		Assume.assumeTrue(
			FileSystems.getDefault(
			).supportedFileAttributeViews(
			).contains(
				"posix"
			));

		Map<String, ArtifactFile> artifactFiles = new TreeMap<>();

		for (ArtifactFile artifactFile :
				List.of(
					ArtifactFile.newFile(
						"a".getBytes(StandardCharsets.UTF_8), 0644, "a.txt"),
					ArtifactFile.newDirectory("bin"),
					ArtifactFile.newFile(
						"#!/bin/sh".getBytes(StandardCharsets.UTF_8), 0755,
						"bin/run.sh"),
					ArtifactFile.newSymbolicLink("a.txt", "link"))) {

			artifactFiles.put(artifactFile.getName(), artifactFile);
		}

		Path rootPath = temporaryFolder.newFolder(
		).toPath();

		ArtifactFile.writeDirectory(artifactFiles, rootPath);

		Map<String, ArtifactFile> readArtifactFiles =
			ArtifactFile.readDirectory(rootPath);

		Assert.assertEquals(artifactFiles.keySet(), readArtifactFiles.keySet());

		ArtifactFile artifactFile = readArtifactFiles.get("a.txt");

		Assert.assertEquals(0644, artifactFile.getMode());

		artifactFile = readArtifactFiles.get("bin/run.sh");

		Assert.assertEquals(0755, artifactFile.getMode());

		artifactFile = readArtifactFiles.get("link");

		Assert.assertEquals("a.txt", artifactFile.getLinkTarget());
	}

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	private void _putEntry(
			ZipArchiveOutputStream zipArchiveOutputStream, String name,
			Integer unixMode, String data)
		throws IOException {

		ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(name);

		if (unixMode != null) {
			zipArchiveEntry.setUnixMode(unixMode);
		}

		zipArchiveOutputStream.putArchiveEntry(zipArchiveEntry);

		if (data != null) {
			zipArchiveOutputStream.write(data.getBytes(StandardCharsets.UTF_8));
		}

		zipArchiveOutputStream.closeArchiveEntry();
	}

}