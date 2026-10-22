/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Gregory Amerson
 */
public class ArtifactMount {

	public ArtifactMount(String mountPath, String subpath) {
		_mountPath = mountPath;
		_subpath = subpath;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof ArtifactMount)) {
			return false;
		}

		ArtifactMount artifactMount = (ArtifactMount)object;

		if (Objects.equals(_mountPath, artifactMount._mountPath) &&
			Objects.equals(_subpath, artifactMount._subpath)) {

			return true;
		}

		return false;
	}

	public String getMountPath() {
		return _mountPath;
	}

	public String getSubpath() {
		return _subpath;
	}

	@Override
	public int hashCode() {
		return Objects.hash(_mountPath, _subpath);
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();

		map.put("mountPath", _mountPath);

		if (!_subpath.isEmpty()) {
			map.put("subPath", _subpath);
		}

		return map;
	}

	@Override
	public String toString() {
		return _mountPath + "=" + _subpath;
	}

	private final String _mountPath;
	private final String _subpath;

}