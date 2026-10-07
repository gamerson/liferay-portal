/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.environment.internal;

import com.liferay.portal.environment.Entitlements;
import com.liferay.portal.environment.LicenseState;
import com.liferay.portal.environment.LiferayEnvironment;
import com.liferay.portal.environment.LiferayEnvironmentStatus;
import com.liferay.portal.environment.LiferayEnvironmentType;
import com.liferay.portal.environment.util.KeyPairUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;

import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

import java.net.InetAddress;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.security.KeyPair;

import java.util.Date;
import java.util.Properties;
import java.util.UUID;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Keeps the environment's state in files under
 * <code>${liferay.home}/data/environment</code>, and installs add-ons through
 * the auto deploy directory. Each node keeps its own files, so nodes that do
 * not share <code>liferay.home</code> are separate environments.
 *
 * @author Gregory Amerson
 */
@Component(
	property = "service.ranking:Integer=-1", service = LiferayEnvironment.class
)
public class SelfHostedLiferayEnvironment implements LiferayEnvironment {

	@Override
	public Entitlements getEntitlements() throws Exception {
		Path licenseXMLPath = _environmentPath.resolve("license.xml");

		if (Files.notExists(licenseXMLPath)) {
			return null;
		}

		return new Entitlements(
			_read("add-ons.json"), Files.readAllBytes(licenseXMLPath),
			GetterUtil.getInteger(_read("maxClusterNodes")));
	}

	@Override
	public synchronized String getEnvironmentId() throws Exception {
		Path environmentIdPath = _environmentPath.resolve("environment-id");

		if (Files.exists(environmentIdPath)) {
			return _read("environment-id");
		}

		String environmentId = String.valueOf(UUID.randomUUID());

		_write("environment-id", environmentId);

		return environmentId;
	}

	@Override
	public String getEnvironmentName() throws Exception {
		InetAddress inetAddress = InetAddress.getLocalHost();

		return inetAddress.getHostName();
	}

	@Override
	public synchronized KeyPair getKeyPair() throws Exception {
		Path privateKeyPath = _environmentPath.resolve("private.pem");

		if (Files.exists(privateKeyPath)) {
			return KeyPairUtil.toKeyPair(_read("private.pem"));
		}

		KeyPair keyPair = KeyPairUtil.generateKeyPair();

		_write(
			"public.pem", KeyPairUtil.getPublicKeyBase64(keyPair.getPublic()));
		_write(
			"private.pem", KeyPairUtil.getPrivateKeyPEM(keyPair.getPrivate()));

		return keyPair;
	}

	@Override
	public LiferayEnvironmentStatus getStatus() throws Exception {
		LiferayEnvironmentStatus liferayEnvironmentStatus =
			new LiferayEnvironmentStatus();

		Path statusPath = _environmentPath.resolve("status.properties");

		if (Files.notExists(statusPath)) {
			return liferayEnvironmentStatus;
		}

		Properties properties = new Properties();

		try (Reader reader = Files.newBufferedReader(statusPath)) {
			properties.load(reader);
		}

		liferayEnvironmentStatus.setActivatedDate(
			_getDate(properties, "activatedDate"));
		liferayEnvironmentStatus.setLastVerifiedDate(
			_getDate(properties, "lastVerifiedDate"));

		String licenseState = properties.getProperty("licenseState");

		if (licenseState != null) {
			liferayEnvironmentStatus.setLicenseState(
				LicenseState.valueOf(licenseState));
		}

		String maxClusterNodes = properties.getProperty("maxClusterNodes");

		if (maxClusterNodes != null) {
			liferayEnvironmentStatus.setMaxClusterNodes(
				Integer.valueOf(maxClusterNodes));
		}

		liferayEnvironmentStatus.setMessage(properties.getProperty("message"));

		String provisioningReachable = properties.getProperty(
			"provisioningReachable");

		if (provisioningReachable != null) {
			liferayEnvironmentStatus.setProvisioningReachable(
				Boolean.valueOf(provisioningReachable));
		}

		liferayEnvironmentStatus.setValidUntilDate(
			_getDate(properties, "validUntilDate"));

		return liferayEnvironmentStatus;
	}

	@Override
	public LiferayEnvironmentType getType() {
		return LiferayEnvironmentType.SELF_HOSTED;
	}

	@Override
	public void storeAddOn(String fileName, InputStream inputStream)
		throws Exception {

		Files.createDirectories(_addOnsPath);

		Path fileNamePath = Paths.get(fileName);

		Path temporaryPath = _createTemporaryPath();

		Files.copy(
			inputStream, temporaryPath, StandardCopyOption.REPLACE_EXISTING);

		Files.move(
			temporaryPath, _addOnsPath.resolve(fileNamePath.getFileName()),
			StandardCopyOption.REPLACE_EXISTING);
	}

	@Override
	public void storeEntitlements(Entitlements entitlements) throws Exception {
		_write("add-ons.json", entitlements.getAddOnsJSON());
		_write(
			"maxClusterNodes",
			String.valueOf(entitlements.getMaxClusterNodes()));

		_write("license.xml", entitlements.getLicenseXML());
	}

	@Override
	public void updateStatus(LiferayEnvironmentStatus liferayEnvironmentStatus)
		throws Exception {

		Properties properties = new Properties();

		_setDate(
			properties, "activatedDate",
			liferayEnvironmentStatus.getActivatedDate());
		_setDate(
			properties, "lastVerifiedDate",
			liferayEnvironmentStatus.getLastVerifiedDate());
		_setValue(
			properties, "licenseState",
			liferayEnvironmentStatus.getLicenseState());
		_setValue(
			properties, "maxClusterNodes",
			liferayEnvironmentStatus.getMaxClusterNodes());
		_setValue(properties, "message", liferayEnvironmentStatus.getMessage());
		_setValue(
			properties, "provisioningReachable",
			liferayEnvironmentStatus.getProvisioningReachable());
		_setDate(
			properties, "validUntilDate",
			liferayEnvironmentStatus.getValidUntilDate());

		Path temporaryPath = _createTemporaryPath();

		try (Writer writer = Files.newBufferedWriter(temporaryPath)) {
			properties.store(writer, null);
		}

		_move(temporaryPath, "status.properties");
	}

	@Activate
	protected void activate() {
		_addOnsPath = Paths.get(
			PropsUtil.get(PropsKeys.AUTO_DEPLOY_DEPLOY_DIR));
		_environmentPath = Paths.get(
			PropsUtil.get(PropsKeys.LIFERAY_HOME), "data", "environment");
	}

	private Path _createTemporaryPath() throws Exception {
		Files.createDirectories(_environmentPath);

		return Files.createTempFile(_environmentPath, "environment", ".tmp");
	}

	private Date _getDate(Properties properties, String key) {
		String value = properties.getProperty(key);

		if (value == null) {
			return null;
		}

		return new Date(GetterUtil.getLong(value));
	}

	private void _move(Path temporaryPath, String fileName) throws Exception {
		Files.move(
			temporaryPath, _environmentPath.resolve(fileName),
			StandardCopyOption.ATOMIC_MOVE,
			StandardCopyOption.REPLACE_EXISTING);
	}

	private String _read(String fileName) throws Exception {
		String value = Files.readString(_environmentPath.resolve(fileName));

		return value.trim();
	}

	private void _setDate(Properties properties, String key, Date date) {
		if (date != null) {
			properties.setProperty(key, String.valueOf(date.getTime()));
		}
	}

	private void _setValue(Properties properties, String key, Object value) {
		if (value != null) {
			properties.setProperty(key, String.valueOf(value));
		}
	}

	private void _write(String fileName, byte[] bytes) throws Exception {
		Path temporaryPath = _createTemporaryPath();

		Files.write(temporaryPath, bytes);

		_move(temporaryPath, fileName);
	}

	private void _write(String fileName, String value) throws Exception {
		_write(fileName, value.getBytes(StandardCharsets.UTF_8));
	}

	private Path _addOnsPath;
	private Path _environmentPath;

}