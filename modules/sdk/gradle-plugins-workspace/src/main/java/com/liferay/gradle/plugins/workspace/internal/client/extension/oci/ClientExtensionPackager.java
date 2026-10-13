/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.gradle.api.GradleException;

/**
 * Turns a client extension zip into what a cluster runs, without building an
 * image: the zip's files as a single layer OCI artifact that the chart mounts
 * into the unmodified base image, and a chart with the configuration payload,
 * the base image, the mounts and the workload shape from LCP.json baked into
 * its defaults.
 *
 * @author Gregory Amerson
 */
public class ClientExtensionPackager {

	public ClientExtensionPackager(
		Map<String, byte[]> chartFiles, String clusterRegistry,
		String containerRuntime, OCIPublisher ociPublisher, File outputDir,
		String registry, String serviceId, String version, File zipFile) {

		_chartFiles = chartFiles;
		_clusterRegistry = clusterRegistry;
		_containerRuntime = containerRuntime;
		_ociPublisher = ociPublisher;
		_outputDir = outputDir;
		_registry = registry;
		_serviceId = serviceId;
		_version = version;
		_zipFile = zipFile;
	}

	public Map<String, Object> packageClientExtension()
		throws InterruptedException, IOException {

		Map<String, ArtifactFile> artifactFiles = ArtifactFile.readZip(
			_zipFile);

		ArtifactFile dockerfileArtifactFile = artifactFiles.get("Dockerfile");

		if (dockerfileArtifactFile == null) {
			throw new GradleException(_zipFile + " carries no Dockerfile");
		}

		ClientExtensionDockerfile clientExtensionDockerfile =
			ClientExtensionDockerfile.parse(
				new String(
					dockerfileArtifactFile.getData(), StandardCharsets.UTF_8));

		Map<String, Object> configs = ChartValues.mergeConfigs(artifactFiles);

		Map<String, Object> lcp = new LinkedHashMap<>();

		ArtifactFile lcpArtifactFile = artifactFiles.get("LCP.json");

		if (lcpArtifactFile != null) {
			lcp = _objectMapper.readValue(
				lcpArtifactFile.getData(),
				new TypeReference<LinkedHashMap<String, Object>>() {
				});
		}

		List<String> warnings = new ArrayList<>(
			clientExtensionDockerfile.getWarnings());

		if (clientExtensionDockerfile.isReplayable()) {
			artifactFiles = _replayRuns(
				artifactFiles, clientExtensionDockerfile);
		}
		else {
			for (String run : clientExtensionDockerfile.getRuns()) {
				warnings.add(
					"RUN " + run + " is not applied because the zip is not " +
						"copied whole to one destination.");
			}
		}

		ArtifactLayer artifactLayer = ArtifactLayer.plan(
			artifactFiles, clientExtensionDockerfile.getCopies());

		String tag = _version.replace('+', '_');

		Map<String, Object> summary = new LinkedHashMap<>();

		String artifactReference = "";

		List<ArtifactMount> artifactMounts = artifactLayer.getMounts();

		if (!artifactMounts.isEmpty()) {
			String digest = _ociPublisher.publishArtifact(
				_registry + "/cx/" + _serviceId, artifactLayer.toTar(), tag);

			artifactReference =
				_clusterRegistry + "/cx/" + _serviceId + "@" + digest;

			Map<String, Object> artifact = new LinkedHashMap<>();

			artifact.put("digest", digest);

			List<Object> mounts = new ArrayList<>();

			for (ArtifactMount artifactMount : artifactMounts) {
				mounts.add(artifactMount.toMap());
			}

			artifact.put("mounts", mounts);
			artifact.put("reference", artifactReference);

			summary.put("artifact", artifact);
		}

		ChartArchive chartArchive = ChartArchive.create(
			_chartFiles, _serviceId,
			ChartValues.bake(
				artifactReference, configs, clientExtensionDockerfile, lcp,
				artifactMounts, _serviceId),
			_version);

		File chartArchiveFile = new File(
			_outputDir, _serviceId + "-" + _version + ".tgz");

		Files.write(chartArchiveFile.toPath(), chartArchive.getData());

		String chartRepository = _registry + "/charts/" + _serviceId;

		Map<String, Object> chart = new LinkedHashMap<>();

		chart.put("archive", chartArchiveFile.getAbsolutePath());
		chart.put(
			"digest",
			_ociPublisher.publishChart(
				chartArchive.getData(), chartArchive.getChartMetadata(),
				chartRepository, tag));
		chart.put("reference", "oci://" + chartRepository);
		chart.put("version", _version);

		summary.put("chart", chart);

		summary.put("warnings", warnings);

		return summary;
	}

	private void _deleteRecursively(Path path) throws IOException {
		if (!Files.exists(path)) {
			return;
		}

		List<Path> paths = new ArrayList<>();

		try (Stream<Path> stream = Files.walk(path)) {
			stream.forEach(paths::add);
		}

		paths.sort(Comparator.reverseOrder());

		for (Path childPath : paths) {
			Files.delete(childPath);
		}
	}

	private String _getUserId() throws InterruptedException, IOException {
		if (File.separatorChar == '\\') {
			return null;
		}

		return _runCommand("id", "-u") + ":" + _runCommand("id", "-g");
	}

	private String _readOutput(Process process) throws IOException {
		try (InputStream inputStream = process.getInputStream()) {
			return new String(
				inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/**
	 * Runs the Dockerfile's RUN steps inside the zip's own base image against
	 * the unpacked zip, and returns the tree they leave. An image build ran
	 * them, and mounting cannot: the node sample's zip carries one of its six
	 * dependencies, and npm install is where the rest come from. Running them
	 * in the base image installs for the exact runtime the pod uses.
	 */
	private Map<String, ArtifactFile> _replayRuns(
			Map<String, ArtifactFile> artifactFiles,
			ClientExtensionDockerfile clientExtensionDockerfile)
		throws InterruptedException, IOException {

		List<ClientExtensionDockerfile.Copy> copies =
			clientExtensionDockerfile.getCopies();

		ClientExtensionDockerfile.Copy copy = copies.get(0);

		String root = copy.getDestination();

		root = root.replaceAll("/+$", "");

		if (root.isEmpty()) {
			root = "/";
		}

		Path treeDirPath = Files.createTempDirectory("cx-replay-");

		try {
			ArtifactFile.writeDirectory(artifactFiles, treeDirPath);

			String commands = String.join(
				" && ", clientExtensionDockerfile.getRuns());

			List<String> command = new ArrayList<>(
				List.of(
					_containerRuntime, "run", "--entrypoint", "sh", "--env",
					"HOME=/tmp", "--rm"));

			String userId = _getUserId();

			if (userId != null) {
				command.add("--user");
				command.add(userId);
			}

			Collections.addAll(
				command, "--volume", treeDirPath + ":" + root, "--workdir",
				root, clientExtensionDockerfile.getImage(), "-c", commands);

			ProcessBuilder processBuilder = new ProcessBuilder(command);

			processBuilder.redirectErrorStream(true);

			Process process;

			try {
				process = processBuilder.start();
			}
			catch (IOException ioException) {
				throw new GradleException(
					"The Dockerfile has RUN steps, which need " +
						_containerRuntime + " to replay",
					ioException);
			}

			String output = _readOutput(process);

			if (process.waitFor() != 0) {
				throw new GradleException(
					"Unable to replay " + commands + " in " +
						clientExtensionDockerfile.getImage() + ":\n" + output);
			}

			return ArtifactFile.readDirectory(treeDirPath);
		}
		finally {
			_deleteRecursively(treeDirPath);
		}
	}

	private String _runCommand(String... command)
		throws InterruptedException, IOException {

		ProcessBuilder processBuilder = new ProcessBuilder(command);

		Process process = processBuilder.start();

		String output = _readOutput(process);

		process.waitFor();

		return output.trim();
	}

	private static final ObjectMapper _objectMapper = new ObjectMapper();

	private final Map<String, byte[]> _chartFiles;
	private final String _clusterRegistry;
	private final String _containerRuntime;
	private final OCIPublisher _ociPublisher;
	private final File _outputDir;
	private final String _registry;
	private final String _serviceId;
	private final String _version;
	private final File _zipFile;

}