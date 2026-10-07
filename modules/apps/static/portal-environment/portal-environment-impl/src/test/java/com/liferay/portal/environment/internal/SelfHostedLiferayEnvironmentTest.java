/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.environment.internal;

import com.liferay.portal.environment.Entitlements;
import com.liferay.portal.environment.LicenseState;
import com.liferay.portal.environment.LiferayEnvironmentStatus;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.io.ByteArrayInputStream;
import java.io.File;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.security.KeyPair;

import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Gregory Amerson
 */
public class SelfHostedLiferayEnvironmentTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() throws Exception {
		_addOnsFolder = temporaryFolder.newFolder("deploy");
		_environmentFolder = temporaryFolder.newFolder("environment");

		_selfHostedLiferayEnvironment = _createSelfHostedLiferayEnvironment();
	}

	@Test
	public void testGetEntitlementsWhenNoneAreStored() throws Exception {
		Assert.assertNull(_selfHostedLiferayEnvironment.getEntitlements());
	}

	@Test
	public void testGetEnvironmentIdIsSharedByEveryInstance() throws Exception {
		String environmentId = _selfHostedLiferayEnvironment.getEnvironmentId();

		SelfHostedLiferayEnvironment selfHostedLiferayEnvironment =
			_createSelfHostedLiferayEnvironment();

		Assert.assertEquals(
			environmentId, selfHostedLiferayEnvironment.getEnvironmentId());
	}

	@Test
	public void testGetKeyPairIsSharedByEveryInstance() throws Exception {
		KeyPair keyPair = _selfHostedLiferayEnvironment.getKeyPair();

		SelfHostedLiferayEnvironment selfHostedLiferayEnvironment =
			_createSelfHostedLiferayEnvironment();

		KeyPair storedKeyPair = selfHostedLiferayEnvironment.getKeyPair();

		Assert.assertArrayEquals(
			keyPair.getPrivate(
			).getEncoded(),
			storedKeyPair.getPrivate(
			).getEncoded());
		Assert.assertArrayEquals(
			keyPair.getPublic(
			).getEncoded(),
			storedKeyPair.getPublic(
			).getEncoded());
	}

	@Test
	public void testGetStatusWhenNoneIsReported() throws Exception {
		LiferayEnvironmentStatus liferayEnvironmentStatus =
			_selfHostedLiferayEnvironment.getStatus();

		Assert.assertNull(liferayEnvironmentStatus.getActivatedDate());
		Assert.assertNull(liferayEnvironmentStatus.getMaxClusterNodes());
	}

	@Test
	public void testStoreAddOn() throws Exception {
		_selfHostedLiferayEnvironment.storeAddOn(
			"sample.lpkg",
			new ByteArrayInputStream(
				"sample".getBytes(StandardCharsets.UTF_8)));

		Path path = _addOnsFolder.toPath();

		Assert.assertEquals(
			"sample", Files.readString(path.resolve("sample.lpkg")));
	}

	@Test
	public void testStoreEntitlements() throws Exception {
		_selfHostedLiferayEnvironment.storeEntitlements(
			new Entitlements(
				"[]", "<licenses/>".getBytes(StandardCharsets.UTF_8), 3));

		Entitlements entitlements =
			_selfHostedLiferayEnvironment.getEntitlements();

		Assert.assertEquals("[]", entitlements.getAddOnsJSON());
		Assert.assertEquals(
			"<licenses/>",
			new String(entitlements.getLicenseXML(), StandardCharsets.UTF_8));
		Assert.assertEquals(3, entitlements.getMaxClusterNodes());
	}

	@Test
	public void testUpdateStatusReplacesThePreviousStatus() throws Exception {
		LiferayEnvironmentStatus liferayEnvironmentStatus =
			new LiferayEnvironmentStatus();

		Date activatedDate = new Date(1000);

		liferayEnvironmentStatus.setActivatedDate(activatedDate);

		liferayEnvironmentStatus.setLicenseState(LicenseState.VALID);
		liferayEnvironmentStatus.setMaxClusterNodes(3);
		liferayEnvironmentStatus.setMessage("Unreachable");
		liferayEnvironmentStatus.setProvisioningReachable(false);

		_selfHostedLiferayEnvironment.updateStatus(liferayEnvironmentStatus);

		liferayEnvironmentStatus = _selfHostedLiferayEnvironment.getStatus();

		Assert.assertEquals(
			activatedDate, liferayEnvironmentStatus.getActivatedDate());
		Assert.assertEquals(
			LicenseState.VALID, liferayEnvironmentStatus.getLicenseState());
		Assert.assertEquals(
			Integer.valueOf(3), liferayEnvironmentStatus.getMaxClusterNodes());
		Assert.assertEquals(
			"Unreachable", liferayEnvironmentStatus.getMessage());
		Assert.assertEquals(
			Boolean.FALSE, liferayEnvironmentStatus.getProvisioningReachable());

		liferayEnvironmentStatus.setMessage(null);
		liferayEnvironmentStatus.setProvisioningReachable(true);

		_selfHostedLiferayEnvironment.updateStatus(liferayEnvironmentStatus);

		liferayEnvironmentStatus = _selfHostedLiferayEnvironment.getStatus();

		Assert.assertNull(liferayEnvironmentStatus.getMessage());
		Assert.assertEquals(
			Boolean.TRUE, liferayEnvironmentStatus.getProvisioningReachable());
	}

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private SelfHostedLiferayEnvironment _createSelfHostedLiferayEnvironment() {
		SelfHostedLiferayEnvironment selfHostedLiferayEnvironment =
			new SelfHostedLiferayEnvironment();

		ReflectionTestUtil.setFieldValue(
			selfHostedLiferayEnvironment, "_addOnsPath",
			_addOnsFolder.toPath());
		ReflectionTestUtil.setFieldValue(
			selfHostedLiferayEnvironment, "_environmentPath",
			_environmentFolder.toPath());

		return selfHostedLiferayEnvironment;
	}

	private File _addOnsFolder;
	private File _environmentFolder;
	private SelfHostedLiferayEnvironment _selfHostedLiferayEnvironment;

}