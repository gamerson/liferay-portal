/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.agent.internal;

import com.liferay.osgi.util.configuration.ConfigurationFactoryUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.persistence.InMemoryOnlyConfigurationThreadLocal;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.URLUtil;
import com.liferay.portal.tools.DBUpgrader;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.felix.configurator.impl.json.BinUtil;
import org.apache.felix.configurator.impl.json.BinaryManager;
import org.apache.felix.configurator.impl.json.JSONUtil;
import org.apache.felix.configurator.impl.model.Config;
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

	private List<String> _addConfigurations(Bundle bundle) {
		List<String> addedPids = new ArrayList<>();

		Enumeration<URL> enumeration = bundle.findEntries(
			"/META-INF/client-extension-config", "*.json", false);

		if (enumeration != null) {
			while (enumeration.hasMoreElements()) {
				URL url = enumeration.nextElement();

				try {
					List<String> processedPids = _processConfigurations(
						bundle, url.getPath(), URLUtil.toString(url));

					if (processedPids != null) {
						addedPids.addAll(processedPids);
					}
				}
				catch (Exception exception) {
					_log.error(
						"Unable to process client extension config " + url,
						exception);
				}
			}
		}

		return addedPids;
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
		Config config, String virtualInstanceId) {

		String pid = config.getPid();

		String factoryPid = ConfigurationFactoryUtil.getFactoryPidFromPid(pid);

		if (factoryPid == null) {
			return pid;
		}

		return StringBundler.concat(pid, "/", virtualInstanceId);
	}

	private String _processConfiguration(Bundle bundle, Config config)
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
						"(.cx.config.key=", virtualInstancePid, ")"));

			if (ArrayUtil.isNotEmpty(configurations)) {
				configuration = configurations[0];

				Dictionary<String, Object> configurationProperties =
					configuration.getProperties();

				if (Objects.equals(
						configurationProperties.get(
							".cx.config.bundle.last.modified"),
						bundle.getLastModified())) {

					if (_log.isInfoEnabled()) {
						_log.info(
							"Configuration and CX Bundle resource versions " +
								"are identical");
					}

					return configuration.getPid();
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

			properties.put(".cx.config.key", virtualInstancePid);
			properties.put(
				".cx.config.bundle.last.modified", bundle.getLastModified());
			properties.put(".cx.config.bundle.id", bundle.getBundleId());

			if (_log.isInfoEnabled()) {
				_log.info("Processed configuration " + properties);
			}

			configuration.updateIfDifferent(properties);

			configuration.addAttributes(
				Configuration.ConfigurationAttribute.READ_ONLY);

			return configuration.getPid();
		}
		finally {
			InMemoryOnlyConfigurationThreadLocal.setInMemoryOnly(false);
		}
	}

	private List<String> _processConfigurations(
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
			return null;
		}

		List<String> addedPids = new ArrayList<>();

		for (Config config : configurationFile.getConfigurations()) {
			try {
				addedPids.add(_processConfiguration(bundle, config));
			}
			catch (Exception exception) {
				_log.error(exception);
			}
		}

		return addedPids;
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

			List<String> addedPids = _addConfigurations(bundle);

			if (addedPids.isEmpty()) {
				return null;
			}

			return bundle;
		}

		@Override
		public void modifiedBundle(
			Bundle bundle, BundleEvent bundleEvent, Bundle unusedBundle) {

			Map<String, Configuration> existingPidConfigurations =
				new HashMap<>();

			try {
				Configuration[] configurations =
					_configurationAdmin.listConfigurations(
						"(.cx.config.bundle.id=" + bundle.getBundleId() + ")");

				if (configurations != null) {
					for (Configuration configuration : configurations) {
						existingPidConfigurations.put(
							configuration.getPid(), configuration);
					}
				}
			}
			catch (Exception exception) {
				_log.error(exception);
			}

			List<String> addedPids = _addConfigurations(bundle);

			for (Map.Entry<String, Configuration> entry :
					existingPidConfigurations.entrySet()) {

				if (!addedPids.contains(entry.getKey())) {
					Configuration existingConfiguration = entry.getValue();

					try {
						if (_log.isInfoEnabled()) {
							_log.info(
								"Deleting configuration " +
									existingConfiguration.getProperties());
						}

						existingConfiguration.delete();
					}
					catch (Exception exception) {
						_log.error(exception);
					}
				}
			}
		}

		@Override
		public void removedBundle(
			Bundle bundle, BundleEvent event, Bundle unusedBundle) {

			Configuration[] configurations = null;

			try {
				configurations = _configurationAdmin.listConfigurations(
					"(.cx.config.bundle.id=" + bundle.getBundleId() + ")");
			}
			catch (Exception exception) {
				_log.error(exception);
			}

			if (configurations == null) {
				return;
			}

			for (Configuration configuration : configurations) {
				try {
					if (_log.isInfoEnabled()) {
						_log.info(
							"Deleting configuration " +
								configuration.getProperties());
					}

					configuration.delete();
				}
				catch (Exception exception) {
					_log.error(exception);
				}
			}
		}

		private static final Log _log = LogFactoryUtil.getLog(
			ClientExtensionConfigBundleTracker.class);

	}

}