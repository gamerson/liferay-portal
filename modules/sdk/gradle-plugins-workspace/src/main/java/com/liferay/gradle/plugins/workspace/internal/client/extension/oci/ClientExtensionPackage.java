/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.liferay.gradle.plugins.workspace.internal.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.gradle.api.GradleException;

/**
 * @author Gregory Amerson
 */
public class ClientExtensionPackage {

	public static ClientExtensionPackage create(
			String containerRuntime, File defaultDockerfile,
			String dockerfileName, File workDir, File zipFile)
		throws InterruptedException, IOException {

		Map<String, ArtifactFile> artifactFiles = ArtifactFile.readZip(zipFile);

		ArtifactFile dockerfileArtifactFile = artifactFiles.get("Dockerfile");

		if (dockerfileArtifactFile == null) {
			throw new GradleException(
				"Client extension zip \"" + zipFile + "\" has no " +
					"\"Dockerfile\"");
		}

		String defaultDockerfileText = Files.readString(
			defaultDockerfile.toPath());

		ClientExtensionDockerfile clientExtensionDockerfile =
			ClientExtensionDockerfile.parse(defaultDockerfileText);

		List<String> defaultInstructions =
			ClientExtensionDockerfile.getInstructions(defaultDockerfileText);

		List<String> instructions = ClientExtensionDockerfile.getInstructions(
			new String(
				dockerfileArtifactFile.getData(), StandardCharsets.UTF_8));

		boolean customDockerfile = !instructions.equals(defaultInstructions);

		List<String> warnings = new ArrayList<>();

		if (customDockerfile) {
			List<String> omittedInstructions = new ArrayList<>(instructions);

			omittedInstructions.removeAll(defaultInstructions);

			warnings.add(
				_getCustomDockerfileWarning(
					clientExtensionDockerfile, dockerfileName,
					omittedInstructions));
		}

		Map<String, Object> configs = HelmChartValues.mergeConfigs(
			artifactFiles);

		Map<String, Object> lcp = new LinkedHashMap<>();

		ArtifactFile lcpArtifactFile = artifactFiles.get("LCP.json");

		if (lcpArtifactFile != null) {
			lcp = _objectMapper.readValue(
				lcpArtifactFile.getData(),
				new TypeReference<LinkedHashMap<String, Object>>() {
				});
		}

		if (clientExtensionDockerfile.isReplayable()) {
			artifactFiles = _replayRuns(
				artifactFiles, clientExtensionDockerfile, containerRuntime,
				workDir);
		}

		ArtifactLayer artifactLayer = null;

		try {
			artifactLayer = ArtifactLayer.plan(
				artifactFiles, clientExtensionDockerfile.getCopies());
		}
		catch (GradleException gradleException) {
			if (!customDockerfile) {
				throw gradleException;
			}

			throw new GradleException(
				StringUtil.concat(
					"\"", dockerfileName, "\" is not supported for packaging, ",
					"and the default \"", clientExtensionDockerfile.getImage(),
					"\" layout does not apply to this client extension"),
				gradleException);
		}

		return new ClientExtensionPackage(
			artifactLayer, clientExtensionDockerfile, configs, lcp, warnings);
	}

	public HelmChartArchive createHelmChartArchive(
			String artifactReference, HelmChartSource helmChartSource,
			String serviceId, String version)
		throws IOException {

		Map<String, String> annotations = new LinkedHashMap<>();

		if (!artifactReference.isEmpty()) {
			annotations.put(
				HelmChartArchive.ANNOTATION_ARTIFACT, artifactReference);
		}

		annotations.put(
			HelmChartArchive.ANNOTATION_CONTENT_DIGEST,
			getContentDigest(helmChartSource, serviceId));

		return HelmChartArchive.create(
			annotations, serviceId, helmChartSource,
			HelmChartValues.derive(
				artifactReference, _configs, _clientExtensionDockerfile, _lcp,
				_artifactLayer.getMounts(), serviceId),
			version);
	}

	public ArtifactLayer getArtifactLayer() {
		return _artifactLayer;
	}

	/**
	 * Returns the SHA-256 digest of what the Helm chart deploys: the template
	 * chart, the values derived from the client extension without the
	 * artifact reference, and the artifact layer's files. It leaves out
	 * buildTimestamp and the generated files that carry it, so rebuilding
	 * unchanged sources keeps the same digest.
	 */
	public String getContentDigest(
			HelmChartSource helmChartSource, String serviceId)
		throws IOException {

		MessageDigest messageDigest = _newMessageDigest();

		Map<String, byte[]> chartFiles = new TreeMap<>(
			helmChartSource.getChartFiles());

		for (Map.Entry<String, byte[]> entry : chartFiles.entrySet()) {
			_update(messageDigest, "chart");
			_update(messageDigest, entry.getKey());
			_update(messageDigest, entry.getValue());
		}

		_update(messageDigest, "values");
		_update(
			messageDigest,
			_objectMapper.writeValueAsBytes(
				HelmChartValues.derive(
					"", _withoutBuildTimestamps(_configs),
					_clientExtensionDockerfile, _lcp,
					_artifactLayer.getMounts(), serviceId)));

		Map<String, ArtifactFile> artifactFiles = new TreeMap<>(
			_artifactLayer.getArtifactFiles());

		for (ArtifactFile artifactFile : artifactFiles.values()) {
			String name = artifactFile.getName();

			if (name.endsWith(".client-extension-config.json") ||
				name.endsWith("WEB-INF/liferay-plugin-package.properties")) {

				continue;
			}

			_update(messageDigest, "file");
			_update(messageDigest, name);
			_update(messageDigest, String.valueOf(artifactFile.getMode()));

			if (artifactFile.isDirectory()) {
				_update(messageDigest, "directory");
			}
			else if (artifactFile.isSymbolicLink()) {
				_update(messageDigest, "link");
				_update(messageDigest, artifactFile.getLinkTarget());
			}
			else {
				_update(messageDigest, "data");
				_update(messageDigest, artifactFile.getData());
			}
		}

		HexFormat hexFormat = HexFormat.of();

		return "sha256:" + hexFormat.formatHex(messageDigest.digest());
	}

	/**
	 * Returns the version of a client extension that declares none: 0.0.0-g
	 * and the first 12 hex digits of its content digest, so the version
	 * changes only when what the chart deploys changes. The "g" keeps the
	 * pre-release identifier valid in SemVer even when the hex digits are all
	 * numbers with a leading zero.
	 */
	public String getContentVersion(
			HelmChartSource helmChartSource, String serviceId)
		throws IOException {

		String contentDigest = getContentDigest(helmChartSource, serviceId);

		int index = contentDigest.indexOf(':') + 1;

		return "0.0.0-g" + contentDigest.substring(index, index + 12);
	}

	public List<String> getWarnings() {
		return _warnings;
	}

	private static String _getCustomDockerfileWarning(
		ClientExtensionDockerfile clientExtensionDockerfile,
		String dockerfileName, List<String> omittedInstructions) {

		StringBuilder sb = new StringBuilder();

		sb.append("\"");
		sb.append(dockerfileName);
		sb.append("\" is not supported for packaging, so the default \"");
		sb.append(clientExtensionDockerfile.getImage());
		sb.append("\" layout is used instead");

		if (!omittedInstructions.isEmpty()) {
			sb.append(", without \"");
			sb.append(String.join("\", \"", omittedInstructions));
			sb.append("\"");
		}

		sb.append(". To deploy a custom image, build it and install the ");
		sb.append("client extension Helm chart directly.");

		return sb.toString();
	}

	private static Map<String, ArtifactFile> _replayRuns(
			Map<String, ArtifactFile> artifactFiles,
			ClientExtensionDockerfile clientExtensionDockerfile,
			String containerRuntime, File workDir)
		throws InterruptedException, IOException {

		Path workDirPath = workDir.toPath();

		ArtifactFile.writeDirectory(artifactFiles, workDirPath);

		List<String> command = new ArrayList<>(
			List.of(
				containerRuntime, "run", "--entrypoint", "sh", "--env",
				"HOME=/tmp", "--rm"));

		FileSystem fileSystem = FileSystems.getDefault();

		Set<String> fileAttributeViews =
			fileSystem.supportedFileAttributeViews();

		if (fileAttributeViews.contains("unix")) {
			command.add("--user");
			command.add(
				Files.getAttribute(workDirPath, "unix:uid") + ":" +
					Files.getAttribute(workDirPath, "unix:gid"));
		}

		List<ClientExtensionDockerfile.Copy> copies =
			clientExtensionDockerfile.getCopies();

		ClientExtensionDockerfile.Copy copy = copies.get(0);

		String destination = copy.getDestination();

		destination = destination.replaceAll("/+$", "");

		String commands = String.join(
			" && ", clientExtensionDockerfile.getRuns());

		Collections.addAll(
			command, "--volume", workDirPath + ":" + destination, "--workdir",
			destination, clientExtensionDockerfile.getImage(), "-c", commands);

		ProcessBuilder processBuilder = new ProcessBuilder(command);

		processBuilder.redirectErrorStream(true);

		Process process = null;

		try {
			process = processBuilder.start();
		}
		catch (IOException ioException) {
			throw new GradleException(
				"Unable to run \"" + containerRuntime + "\", which replays " +
					"the Dockerfile's RUN instructions",
				ioException);
		}

		String output = null;

		try (InputStream inputStream = process.getInputStream()) {
			output = new String(
				inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}

		if (process.waitFor() != 0) {
			throw new GradleException(
				"Unable to replay \"" + commands + "\" in \"" +
					clientExtensionDockerfile.getImage() + "\":\n" + output);
		}

		return ArtifactFile.readDirectory(workDirPath);
	}

	/**
	 * Adds the data to the digest after its length, so that no two sequences
	 * of fields can produce the same input.
	 */
	private static void _update(MessageDigest messageDigest, byte[] data) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);

		byteBuffer.putLong(data.length);

		messageDigest.update(byteBuffer.array());

		messageDigest.update(data);
	}

	private static void _update(MessageDigest messageDigest, String text) {
		_update(messageDigest, text.getBytes(StandardCharsets.UTF_8));
	}

	private ClientExtensionPackage(
		ArtifactLayer artifactLayer,
		ClientExtensionDockerfile clientExtensionDockerfile,
		Map<String, Object> configs, Map<String, Object> lcp,
		List<String> warnings) {

		_artifactLayer = artifactLayer;
		_clientExtensionDockerfile = clientExtensionDockerfile;
		_configs = configs;
		_lcp = lcp;
		_warnings = warnings;
	}

	private MessageDigest _newMessageDigest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException noSuchAlgorithmException) {
			throw new IllegalStateException(noSuchAlgorithmException);
		}
	}

	/**
	 * Returns the configs without the buildTimestamp that every rebuild of
	 * the zip changes.
	 */
	private Map<String, Object> _withoutBuildTimestamps(
		Map<String, Object> configs) {

		Map<String, Object> configsWithoutBuildTimestamps = new TreeMap<>();

		for (Map.Entry<String, Object> entry : configs.entrySet()) {
			Object value = entry.getValue();

			if (value instanceof Map<?, ?> map) {
				Map<Object, Object> configuration = new LinkedHashMap<>(map);

				configuration.remove("buildTimestamp");

				value = configuration;
			}

			configsWithoutBuildTimestamps.put(entry.getKey(), value);
		}

		return configsWithoutBuildTimestamps;
	}

	private static final ObjectMapper _objectMapper = new ObjectMapper(
	).configure(
		SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true
	);

	private final ArtifactLayer _artifactLayer;
	private final ClientExtensionDockerfile _clientExtensionDockerfile;
	private final Map<String, Object> _configs;
	private final Map<String, Object> _lcp;
	private final List<String> _warnings;

}