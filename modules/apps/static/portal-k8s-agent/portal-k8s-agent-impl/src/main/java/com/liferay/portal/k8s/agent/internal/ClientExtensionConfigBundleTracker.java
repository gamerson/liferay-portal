/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.agent.internal;

import com.liferay.osgi.util.configuration.ConfigurationFactoryUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.petra.string.StringUtil;
import com.liferay.portal.configuration.persistence.InMemoryOnlyConfigurationThreadLocal;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.tools.DBUpgrader;

import java.net.URL;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.felix.configurator.impl.json.BinUtil;
import org.apache.felix.configurator.impl.json.BinaryManager;
import org.apache.felix.configurator.impl.json.JSONUtil;
import org.apache.felix.configurator.impl.model.ConfigurationFile;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * @author Gregory Amerson
 */
@Component(service = {})
public class ClientExtensionConfigBundleTracker {

	@Activate
	protected void activate(
		BundleContext bundleContext, Map<String, Object> properties) {

		_bundleTracker = new BundleTracker<>(
			bundleContext, Bundle.ACTIVE,
			new ClientExtensionConfigBundleTrackerCustomizer());

		_bundleTracker.open();
	}

	@Deactivate
	protected void deactivate() {
		_bundleTracker.close();
	}

	private Configuration _getConfiguration(String pid) throws Exception {
		if (pid.endsWith(_FILE_EXTENSION)) {
			pid = pid.substring(0, pid.length() - _FILE_EXTENSION.length());
		}

		int index = pid.indexOf(CharPool.TILDE);

		if (index <= 0) {
			index = pid.indexOf(CharPool.UNDERLINE);

			if (index <= 0) {
				index = pid.indexOf(CharPool.DASH);
			}
		}

		if (index > 0) {
			String name = pid.substring(index + 1);

			pid = pid.substring(0, index);

			return _configurationAdmin.getFactoryConfiguration(
				pid, name, StringPool.QUESTION);
		}

		return _configurationAdmin.getConfiguration(pid, StringPool.QUESTION);
	}

	private String _getVirtualInstancePid(
		org.apache.felix.configurator.impl.model.Config config,
		String virtualInstanceId) {

		String pid = config.getPid();

		String factoryPid = ConfigurationFactoryUtil.getFactoryPidFromPid(pid);

		if (factoryPid == null) {
			return pid;
		}

		return StringBundler.concat(pid, "/", virtualInstanceId);
	}

	private void _processConfiguration(
			Bundle bundle,
			org.apache.felix.configurator.impl.model.Config config)
		throws Exception {

		Dictionary<String, Object> properties = config.getProperties();

		String virtualInstanceId = (String)properties.get(
			"dxp.lxc.liferay.com.virtualInstanceId");

		if (Objects.equals(virtualInstanceId, "default")) {
			virtualInstanceId = PropsValues.COMPANY_DEFAULT_WEB_ID;
		}

		String virtualInstancePid = _getVirtualInstancePid(
			config, virtualInstanceId);

		try {
			InMemoryOnlyConfigurationThreadLocal.setInMemoryOnly(true);

			Configuration configuration = null;

			Configuration[] configurations =
				_configurationAdmin.listConfigurations(
					StringBundler.concat(
						"(.cx.bundle.config.key=", virtualInstancePid, ")"));

			if (ArrayUtil.isNotEmpty(configurations)) {
				configuration = configurations[0];

				Dictionary<String, Object> configurationProperties =
					configuration.getProperties();

				if (Objects.equals(
						configurationProperties.get(
							".cx.config.resource.version"),
						bundle.getLastModified())) {

					if (_log.isInfoEnabled()) {
						_log.info(
							"Configuration and CX Bundle resource versions " +
								"are identical");
					}

					return;
				}
			}
			else {
				configuration = _getConfiguration(virtualInstancePid);
			}

			Set<Configuration.ConfigurationAttribute> configurationAttributes =
				configuration.getAttributes();

			if (configurationAttributes.contains(
					Configuration.ConfigurationAttribute.READ_ONLY)) {

				configuration.removeAttributes(
					Configuration.ConfigurationAttribute.READ_ONLY);
			}

			properties.put(".cx.bundle.config.key", virtualInstancePid);
			properties.put(
				".cx.bundle.config.resource.version", bundle.getLastModified());
			properties.put(".cx.bundle.config.uid", bundle.getBundleId());

			if (_log.isInfoEnabled()) {
				_log.info("Processed configuration " + properties);
			}

			configuration.updateIfDifferent(properties);

			configuration.addAttributes(
				Configuration.ConfigurationAttribute.READ_ONLY);
		}
		finally {
			InMemoryOnlyConfigurationThreadLocal.setInMemoryOnly(false);
		}
	}

	private void _processConfigurations(
			Bundle bundle, String fileName, String json)
		throws Exception {

		if (!fileName.endsWith(_FILE_EXTENSION)) {
			throw new IllegalArgumentException("Invalid file " + fileName);
		}

		JSONUtil.Report report = new JSONUtil.Report();

		BinaryManager binaryManager = new BinaryManager(
			new BinUtil.ResourceProvider() {

				@Override
				public Enumeration<URL> findEntries(
					String path, String pattern) {

					return Collections.emptyEnumeration();
				}

				@Override
				public long getBundleId() {
					return bundle.getBundleId();
				}

				@Override
				public URL getEntry(String path) {
					return null;
				}

				@Override
				public String getIdentifier() {
					return fileName;
				}

			},
			report);

		ConfigurationFile configurationFile = JSONUtil.readJSON(
			binaryManager, fileName, new URL("file", null, fileName),
			bundle.getBundleId(), json, report);

		for (String error : report.errors) {
			_log.error(error);
		}

		for (String warning : report.warnings) {
			if (_log.isWarnEnabled()) {
				_log.warn(warning);
			}
		}

		if (configurationFile == null) {
			return;
		}

		for (org.apache.felix.configurator.impl.model.Config config :
				configurationFile.getConfigurations()) {

			try {
				_processConfiguration(bundle, config);
			}
			catch (Exception exception) {
				_log.error(exception);
			}
		}
	}

	private static final String _FILE_EXTENSION =
		".client-extension-config.json";

	private static final Log _log = LogFactoryUtil.getLog(
		ClientExtensionConfigBundleTracker.class);

	private BundleTracker<Bundle> _bundleTracker;

	@Reference
	private ConfigurationAdmin _configurationAdmin;

	private class ClientExtensionConfigBundleTrackerCustomizer
		implements BundleTrackerCustomizer<Bundle> {

		@Override
		public Bundle addingBundle(Bundle bundle, BundleEvent bundleEvent) {
			if (DBUpgrader.isUpgradeClient()) {
				return null;
			}

			Enumeration<URL> entries = bundle.findEntries(
				"/", "*.client-extension-config.json", false);

			if (entries != null) {
				while (entries.hasMoreElements()) {
					URL url = entries.nextElement();

					try {
						_processConfigurations(
							bundle, url.getPath(),
							StringUtil.read(url.openStream()));
					}
					catch (Exception exception) {
						_log.error(
							"Unable to process client extension config " + url,
							exception);
					}
				}

				return bundle;
			}

			return null;
		}

		@Override
		public void modifiedBundle(
			Bundle bundle, BundleEvent bundleEvent, Bundle unusedBundle) {
		}

		@Override
		public void removedBundle(
			Bundle bundle, BundleEvent event, Bundle unusedBundle) {
		}

		private static final Log _log = LogFactoryUtil.getLog(
			ClientExtensionConfigBundleTracker.class);

	}

}