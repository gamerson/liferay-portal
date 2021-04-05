/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.workspace.env.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.Method;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.apache.felix.service.command.CommandProcessor;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * @author Leon Chi
 */
@RunWith(Arquillian.class)
public class PortalWorkspaceEnvServiceTest {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@BeforeClass
	public static void setUpClass() {
		Bundle bundle = FrameworkUtil.getBundle(
			PortalWorkspaceEnvServiceTest.class);

		_bundleContext = bundle.getBundleContext();

		try {
			String filter = StringBundler.concat(
				"(&(", CommandProcessor.COMMAND_FUNCTION, "=buildModules)(",
				CommandProcessor.COMMAND_SCOPE, "=portal))");

			Collection<ServiceReference<Object>> refs =
				_bundleContext.getServiceReferences(Object.class, filter);

			Assert.assertNotNull(refs);

			Assert.assertFalse(refs.isEmpty());

			Iterator<ServiceReference<Object>> iterator = refs.iterator();

			_serviceReference = iterator.next();
		}
		catch (InvalidSyntaxException invalidSyntaxException) {
		}

		_commandObject = _bundleContext.getService(_serviceReference);
	}

	@AfterClass
	public static void tearDownClass() {
		_bundleContext.ungetService(_serviceReference);
	}

	@Test
	public void testPortalWorkspaceEnvBuildModules() throws Exception {
		Assert.assertNotNull(_commandObject);

		Class<? extends Object> clazz = _commandObject.getClass();

		Method buildModulesMethod = clazz.getMethod(
			"buildModules", Object.class);

		String serviceXmlContent = _read(
			PortalWorkspaceEnvServiceTest.class.getResourceAsStream(
				"test_service.xml"));

		File[] files = (File[])buildModulesMethod.invoke(
			_commandObject, serviceXmlContent);

		Assert.assertNotNull(files);

		Assert.assertEquals(Arrays.toString(files), 2, files.length);
	}

	private String _read(InputStream inputStream) throws Exception {
		byte[] buffer = new byte[8192];
		int offset = 0;

		while (true) {
			int count = inputStream.read(
				buffer, offset, buffer.length - offset);

			if (count == -1) {
				break;
			}

			offset += count;

			if (offset == buffer.length) {
				byte[] newBuffer = new byte[buffer.length << 1];

				System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);

				buffer = newBuffer;
			}
		}

		if (offset == 0) {
			return "";
		}

		return new String(buffer, 0, offset, "UTF-8");
	}

	private static BundleContext _bundleContext;
	private static Object _commandObject;
	private static ServiceReference<Object> _serviceReference;

}