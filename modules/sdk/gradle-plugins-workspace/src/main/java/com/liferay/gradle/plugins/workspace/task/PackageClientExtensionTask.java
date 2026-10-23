/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.task;

import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.ArtifactLayer;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.ArtifactMount;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.ClientExtensionPackage;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.HelmChartArchive;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.HelmChartSource;

import java.io.File;

import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Gregory Amerson
 */
public class PackageClientExtensionTask extends BaseClientExtensionPackageTask {

	public PackageClientExtensionTask() {
		super("client-extension-package");
	}

	@TaskAction
	public void packageClientExtension() throws Exception {
		HelmChartSource helmChartSource = resolveHelmChartSource();

		ClientExtensionPackage clientExtensionPackage =
			createClientExtensionPackage();

		Project project = getProject();

		String serviceId = project.getName();

		Property<String> chartVersionProperty = getChartVersion();

		String version = chartVersionProperty.get();

		Logger logger = getLogger();

		ArtifactLayer artifactLayer = clientExtensionPackage.getArtifactLayer();

		List<ArtifactMount> artifactMounts = artifactLayer.getMounts();

		if (!artifactMounts.isEmpty()) {
			File file = writeFile(
				artifactLayer.toTar(),
				serviceId + "-" + version + "-layer.tar");

			logger.lifecycle("Wrote artifact layer {}", file);
		}

		HelmChartArchive helmChartArchive =
			clientExtensionPackage.createHelmChartArchive(
				"", helmChartSource, serviceId, version);

		File file = writeFile(
			helmChartArchive.getData(), serviceId + "-" + version + ".tgz");

		logger.lifecycle(
			"Wrote Helm chart {} from {} ({})", file,
			helmChartSource.getTemplate(), helmChartSource.getOrigin());
	}

}