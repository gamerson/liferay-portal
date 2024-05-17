/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.client.extension.type.internal.configuration.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.client.extension.type.CET;
import com.liferay.client.extension.type.manager.CETManager;
import com.liferay.portal.configuration.test.util.ConfigurationTestUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.util.HashMapDictionary;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Gregory Amerson
 */
@RunWith(Arquillian.class)
public class CETConfigurationFactoryVirtualInstanceTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@BeforeClass
	public static void setUpClass() throws Exception {
		Company company = CompanyLocalServiceUtil.addCompany(
			null, _VIRTUAL_HOSTNAME, _VIRTUAL_HOSTNAME, _VIRTUAL_HOSTNAME,
			0, true, null, null, null, null, null, null);

		_autoCloseables.add(
			() -> CompanyLocalServiceUtil.deleteCompany(company));
	}

	@AfterClass
	public static void tearDownClass() {
		ListIterator<AutoCloseable> listIterator =
			_autoCloseables.listIterator(_autoCloseables.size());

		while (listIterator.hasPrevious()) {
			AutoCloseable previousAutoCloseable = listIterator.previous();

			try {
				previousAutoCloseable.close();
			}
			catch (Exception exception) {
				_log.error(exception);
			}
		}
	}

	@Test
	public void testDefault() throws Exception {
		Dictionary<String, Object> properties =
			HashMapDictionaryBuilder.<String, Object>put(
				"baseURL", "${portalURL}/o/liferay-sample-iframe-2"
			).put(
				"description", ""
			).put(
				"dxp.lxc.liferay.com.virtualInstanceId", "default"
			).put(
				"name", "Counter App"
			).put(
				"projectId", "liferaysampleiframe2"
			).put(
				"projectName", "liferay-sample-iframe-2"
			).put(
				"properties", new String[]{""}
			).put(
				"sourceCodeURL", ""
			).put(
				"type", "iframe"
			).put(
				"typeSettings",
				new String[]{
					"portletCategoryName=category.client-extensions",
					"url=https://arnab-datta.github.io/counter-app"
				}
			).put(
				"webContextPath", "/liferay-sample-iframe-2"
			).build();

		String defaultFactoryConfigurationPid = ConfigurationTestUtil.getFactoryConfiguration(
			"com.liferay.client.extension.type.configuration." +
			"CETConfiguration~liferay-sample-iframe-2",
			properties);

		ConfigurationTestUtil.saveConfiguration(defaultFactoryConfigurationPid, properties);

		_autoCloseables.add(
			() -> ConfigurationTestUtil.deleteConfiguration(defaultFactoryConfigurationPid));

		Company company = CompanyLocalServiceUtil.fetchCompanyById(
			PortalUtil.getDefaultCompanyId());

		Assert.assertNotNull(company);

		CET cet = _cetManager.getCET(
			company.getCompanyId(), "liferay-sample-iframe-1");

		Assert.assertNotNull(cet);

		Assert.assertEquals("Liferay Sample", cet.getName());
	}

	@Test
	public void testVirtualHost() throws Exception {
		Dictionary<String, Object> properties =
			HashMapDictionaryBuilder.<String, Object>put(
				"baseURL",
				"${portalURL}/o/liferay-sample-iframe-3_vi.localtest.me"
			).put(
				"description", ""
			).put(
				"dxp.lxc.liferay.com.virtualInstanceId", "vi.localtest.me"
			).put(
				"name", "Counter App vi.localtest.me"
			).put(
				"projectId", "liferaysampleiframe3"
			).put(
				"projectName", "liferay-sample-iframe-3"
			).put(
				"properties", new String[]{""}
			).put(
				"sourceCodeURL", ""
			).put(
				"type", "iframe"
			).put(
				"typeSettings",
				new String[]{
					"portletCategoryName=category.client-extensions",
					"url=https://arnab-datta.github.io/counter-app"
				}
			).put(
				"webContextPath",
				"/liferay-sample-iframe-3_vi.localtest.me"
			).build();

		String virtualInstanceConfigurationPid =
			ConfigurationTestUtil.getFactoryConfiguration(
				"com.liferay.client.extension.type.configuration." +
				"CETConfiguration~liferay-sample-iframe-3" +
				"/vi.localtest.me",
				properties);

		ConfigurationTestUtil.saveConfiguration(virtualInstanceConfigurationPid, properties);

		_autoCloseables.add(
			() -> ConfigurationTestUtil.deleteConfiguration(virtualInstanceConfigurationPid));

		Company company = CompanyLocalServiceUtil.fetchCompanyByVirtualHost(
			_VIRTUAL_HOSTNAME);

		Assert.assertNotNull(company);

		CET cet = _cetManager.getCET(
			company.getCompanyId(), "liferay-sample-iframe-1");

		Assert.assertNotNull(cet);

		Assert.assertEquals("Liferay Sample", cet.getName());
	}

	private static final String _VIRTUAL_HOSTNAME = "vi.localtest.me";

	private static final Log _log = LogFactoryUtil.getLog(
		CETConfigurationFactoryVirtualInstanceTest.class);

	@Inject
	private CETManager _cetManager;

	private static final List<AutoCloseable> _autoCloseables = new ArrayList<>();
}