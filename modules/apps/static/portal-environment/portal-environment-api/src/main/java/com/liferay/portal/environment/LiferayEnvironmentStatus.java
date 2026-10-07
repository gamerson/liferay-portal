/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.environment;

import java.util.Date;

/**
 * Holds the license activation outcome that the deployment enforces. Every
 * field may be <code>null</code>, which means the outcome is not known yet.
 *
 * @author Gregory Amerson
 */
public class LiferayEnvironmentStatus {

	public Date getActivatedDate() {
		return _activatedDate;
	}

	public Date getLastVerifiedDate() {
		return _lastVerifiedDate;
	}

	public LicenseState getLicenseState() {
		return _licenseState;
	}

	/**
	 * Returns the number of cluster nodes the environment may run now, after
	 * any grace period the activation module applies. The deployment caps its
	 * nodes at this number.
	 */
	public Integer getMaxClusterNodes() {
		return _maxClusterNodes;
	}

	public String getMessage() {
		return _message;
	}

	public Boolean getProvisioningReachable() {
		return _provisioningReachable;
	}

	public Date getValidUntilDate() {
		return _validUntilDate;
	}

	public void setActivatedDate(Date activatedDate) {
		_activatedDate = activatedDate;
	}

	public void setLastVerifiedDate(Date lastVerifiedDate) {
		_lastVerifiedDate = lastVerifiedDate;
	}

	public void setLicenseState(LicenseState licenseState) {
		_licenseState = licenseState;
	}

	public void setMaxClusterNodes(Integer maxClusterNodes) {
		_maxClusterNodes = maxClusterNodes;
	}

	public void setMessage(String message) {
		_message = message;
	}

	public void setProvisioningReachable(Boolean provisioningReachable) {
		_provisioningReachable = provisioningReachable;
	}

	public void setValidUntilDate(Date validUntilDate) {
		_validUntilDate = validUntilDate;
	}

	private Date _activatedDate;
	private Date _lastVerifiedDate;
	private LicenseState _licenseState;
	private Integer _maxClusterNodes;
	private String _message;
	private Boolean _provisioningReachable;
	private Date _validUntilDate;

}