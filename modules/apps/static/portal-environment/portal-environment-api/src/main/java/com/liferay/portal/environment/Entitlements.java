/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.environment;

/**
 * Holds what the provisioning service granted the environment. The add-ons
 * stay in the provisioning service's own JSON form, because the activation
 * module owns that schema and an environment only stores it.
 *
 * @author Gregory Amerson
 */
public class Entitlements {

	public Entitlements(
		String addOnsJSON, byte[] licenseXML, int maxClusterNodes) {

		_addOnsJSON = addOnsJSON;
		_licenseXML = licenseXML;
		_maxClusterNodes = maxClusterNodes;
	}

	public String getAddOnsJSON() {
		return _addOnsJSON;
	}

	public byte[] getLicenseXML() {
		return _licenseXML;
	}

	public int getMaxClusterNodes() {
		return _maxClusterNodes;
	}

	private final String _addOnsJSON;
	private final byte[] _licenseXML;
	private final int _maxClusterNodes;

}