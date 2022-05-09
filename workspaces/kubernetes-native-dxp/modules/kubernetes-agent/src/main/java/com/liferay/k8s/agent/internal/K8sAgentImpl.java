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

package com.liferay.k8s.agent.internal;

import com.liferay.k8s.agent.K8sAgent;
import com.liferay.k8s.agent.configuration.v1.K8sAgentConfiguration;
import com.liferay.k8s.agent.properties.ConfigurationProperties;
import com.liferay.k8s.agent.properties.ConfigurationPropertiesFactory;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.petra.string.StringUtil;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapDictionary;
import com.liferay.portal.util.PropsValues;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerEventListener;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.felix.configurator.impl.json.BinUtil;
import org.apache.felix.configurator.impl.json.BinaryManager;
import org.apache.felix.configurator.impl.json.JSONUtil;
import org.apache.felix.configurator.impl.json.JSONUtil.Report;
import org.apache.felix.configurator.impl.model.ConfigurationFile;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Raymond Augé
 */
@Component(
	configurationPid = "com.liferay.k8s.agent.configuration.v1.K8sAgentConfiguration",
	configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true,
	service = K8sAgent.class
)
public class K8sAgentImpl implements K8sAgent {

	@Activate
	public K8sAgentImpl(
			BundleContext bundleContext,
			@Reference CompanyLocalService companyLocalService,
			@Reference ConfigurationAdmin configurationAdmin,
			Map<String, Object> properties)
		throws Exception {

		_bundle = bundleContext.getBundle();
		_companyLocalService = companyLocalService;
		_configurationAdmin = configurationAdmin;

		_k8sAgentConfiguration = ConfigurableUtil.createConfigurable(
			K8sAgentConfiguration.class, properties);

		_log.info(
			StringBundler.concat(
				"Initializing ", _AGENT_NAME, ": ",
				_k8sAgentConfiguration.namespace(), " ",
				_k8sAgentConfiguration.labelSelector()));

		Config config = Config.autoConfigure(null);

		_kubernetesClient = new DefaultKubernetesClient(config);

		SharedInformerFactory sharedInformerFactory =
			_kubernetesClient.informers();

		sharedInformerFactory.addSharedInformerEventListener(
			new SharedInformerEventListener() {

				@Override
				public void onException(Exception exception) {
					_log.error(exception);
				}

			});

		_sharedIndexInformer = _kubernetesClient.configMaps(
		).inNamespace(
			_k8sAgentConfiguration.namespace()
		).withLabel(
			_k8sAgentConfiguration.labelSelector()
		).inform(new ResourceEventHandler<ConfigMap>() {

			@Override
			public void onAdd(ConfigMap configMap) {
				_add(configMap);
			}

			@Override
			public void onDelete(
				ConfigMap configMap, boolean deletedFinalStateUnknown) {

				_delete(configMap);
			}

			@Override
			public void onUpdate(
				ConfigMap oldConfigMap, ConfigMap newConfigMap) {

				_update(oldConfigMap, newConfigMap);
			}
		});

		if (_log.isDebugEnabled()) {
			_log.debug("Initialized " + _AGENT_NAME);
		}
	}

	@Override
	public void createOrUpdateConfigMap(
		Map<String, String> data, Map<String, String> labels,
		String name) {

		ConfigMap configMap = _kubernetesClient.configMaps(
		).inNamespace(
			_k8sAgentConfiguration.namespace()
		).withName(
			name
		).get();

		if (configMap == null) {
			configMap = new ConfigMapBuilder().withNewMetadata(
			).withNamespace(
				_k8sAgentConfiguration.namespace()
			).withName(
				name
			).withLabels(
				labels
			).endMetadata(
			).addToData(
				data
			).build();

			configMap = _kubernetesClient.configMaps(
			).withName(
				name
			).createOrReplace(
				configMap
			);

			if (_log.isDebugEnabled()) {
				_log.debug(StringBundler.concat("Created ", configMap));
			}
		}
		else {
			Map<String,String> currentData = configMap.getData();

			ObjectMeta metadata = configMap.getMetadata();
			Map<String, String> currentLabels = metadata.getLabels();

			if (!Objects.equals(currentData, data) ||
				!Objects.equals(currentLabels, labels)) {

				currentData.putAll(data);
				currentLabels.putAll(labels);

				configMap = _kubernetesClient.configMaps(
				).withName(
					name
				).createOrReplace(
					configMap
				);

				if (_log.isDebugEnabled()) {
					_log.debug(StringBundler.concat("Updated ", configMap));
				}
			}

		}
	}

	@Deactivate
	public void deactivate() {
		if (_log.isDebugEnabled()) {
			_log.debug("Deactivating " + _AGENT_NAME);
		}

		_sharedIndexInformer.close();
		_kubernetesClient.close();

		if (_log.isDebugEnabled()) {
			_log.debug("Deactivated " + _AGENT_NAME);
		}
	}

	@Override
	public void deleteConfigMap(String name) {
		ConfigMap configMap = _kubernetesClient.configMaps(
		).inNamespace(
			_k8sAgentConfiguration.namespace()
		).withName(
			name
		).get();

		if (configMap != null) {
			_kubernetesClient.configMaps(
			).delete(
				configMap
			);

			if (_log.isDebugEnabled()) {
				_log.debug(StringBundler.concat("Deleted ", name));
			}
		}
	}

	@Override
	public void deleteConfigMapByLabels(
		String name, Predicate<Map<String, String>> predicate) {

		ConfigMap configMap = _kubernetesClient.configMaps(
		).inNamespace(
			_k8sAgentConfiguration.namespace()
		).withName(
			name
		).get();

		if (configMap != null) {
			ObjectMeta objectMeta = configMap.getMetadata();

			if (predicate.test(objectMeta.getLabels())) {
				_kubernetesClient.configMaps(
				).delete(
					configMap
				);

				if (_log.isDebugEnabled()) {
					_log.debug(StringBundler.concat("Delted ", name));
				}
			}
		}
	}

	private void _add(ConfigMap configMap) {
		if (_log.isDebugEnabled()) {
			_log.debug(StringBundler.concat("Adding: ", configMap));
		}

		Map<String, String> data = configMap.getData();

		if (data == null) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					StringBundler.concat(
						"Data is null, skipping: ", configMap));
			}

			return;
		}

		Set<Map.Entry<String, String>> entrySet = data.entrySet();

		for (Map.Entry<String, String> entry : entrySet) {
			String configName = entry.getKey();

			try {
				if (configName.endsWith(_FILE_EXT)) {
					_processConfigMapConfigFileEntry(
						configName, _fromStringContent(
							configName, entry.getValue()),
						configMap.getMetadata());
				}
				else if (configName.endsWith(_FILE_JSON_EXT)) {
					_processConfigMapConfigJSONResource(
						configName, entry.getValue(), configMap);
				}
			}
			catch (Exception exception) {
				_log.error(exception);
			}
		}
	}

	private void _delete(ConfigMap configMap) {
		if (_log.isDebugEnabled()) {
			_log.debug(StringBundler.concat("Deleting: ", configMap));
		}

		Map<String, String> data = configMap.getData();

		if (data == null) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					StringBundler.concat(
						"Data is null, skipping: ", configMap));
			}

			return;
		}

		ObjectMeta metadata = configMap.getMetadata();

		String configurationFilter = StringBundler.concat(
			StringPool.OPEN_PARENTHESIS, _K8S_CONFIG_UID, StringPool.EQUAL,
			metadata.getUid(), StringPool.CLOSE_PARENTHESIS);

		try {
			Configuration[] configurations =
				_configurationAdmin.listConfigurations(configurationFilter);

			if (configurations != null) {
				for (Configuration configuration : configurations) {
					try {
						configuration.delete();
					}
					catch (Exception exception) {
						_log.error(exception);
					}
				}
			}
		}
		catch (Exception exception) {
			_log.error(exception);
		}
	}

	private Dictionary<String, Object> _fromStringContent(
			String configName, String configurationContent)
		throws IOException {

		Dictionary<String, Object> dictionary = new HashMapDictionary<>();

		ConfigurationProperties configurationProperties =
			ConfigurationPropertiesFactory.create(
				configName, configurationContent,
				PropsValues.MODULE_FRAMEWORK_FILE_INSTALL_CONFIG_ENCODING);

		for (String key : configurationProperties.keySet()) {
			dictionary.put(key, configurationProperties.get(key));
		}

		return dictionary;
	}

	private Configuration _findExistingConfiguration(String fileName)
		throws Exception {

		Configuration[] configurations = _configurationAdmin.listConfigurations(
			StringBundler.concat(
				StringPool.OPEN_PARENTHESIS, _K8S_CONFIG_KEY,
				StringPool.EQUAL, fileName, StringPool.CLOSE_PARENTHESIS));

		if ((configurations != null) && (configurations.length > 0)) {
			return configurations[0];
		}

		return null;
	}

	private Company _getCompanyIdByEnvironment(String environment)
		throws PortalException {

		if (Objects.equals("default", environment)) {
			return _companyLocalService.getCompanyByWebId(
				PropsValues.COMPANY_DEFAULT_WEB_ID);
		}

		DynamicQuery dynamicQuery = _companyLocalService.dynamicQuery();

		Property webIdProperty = PropertyFactoryUtil.forName("webId");

		// TODO: rationalize this against the scheme for mapping environments to
		// virtual instances

		String webId = environment;

		dynamicQuery.add(webIdProperty.eq(webId));

		List<Company> companies = _companyLocalService.dynamicQuery(
			dynamicQuery);

		if (!companies.isEmpty()) {
			return companies.get(0);
		}

		if (_log.isDebugEnabled()) {
			_log.debug(
				StringBundler.concat(
					"Could not locate a company by webId: ", webId,
					". Using the default."));
		}

		return _companyLocalService.getCompanyByWebId(
			PropsValues.COMPANY_DEFAULT_WEB_ID);
	}

	private Configuration _getConfiguration(String pid, String name)
		throws Exception {

		if (name != null) {
			return _configurationAdmin.getFactoryConfiguration(
				pid, name, StringPool.QUESTION);
		}

		return _configurationAdmin.getConfiguration(pid, StringPool.QUESTION);
	}

	private void _update(ConfigMap oldConfigMap, ConfigMap newConfigMap) {
		if (_log.isDebugEnabled()) {
			_log.debug(
				StringBundler.concat("Updating:", newConfigMap));
		}

		Map<String, String> data = newConfigMap.getData();
		ObjectMeta metadata = newConfigMap.getMetadata();

		if (data != null) {
			Set<Map.Entry<String, String>> entrySet = data.entrySet();

			for (Map.Entry<String, String> entry : entrySet) {
				String configName = entry.getKey();

				try {
					if (configName.endsWith(_FILE_EXT)) {
						_processConfigMapConfigFileEntry(
							configName, _fromStringContent(
								configName, entry.getValue()), metadata);
					}
					else if (configName.endsWith(_FILE_JSON_EXT)) {
						_processConfigMapConfigJSONResource(
							configName, entry.getValue(), newConfigMap);
					}
				}
				catch (Exception exception) {
					_log.error(exception);
				}
			}
		}

		// Remove left over configurations which were deleted from the ConfigMap

		ObjectMeta oldMetadata = oldConfigMap.getMetadata();

		String configurationFilter = StringBundler.concat(
			StringPool.OPEN_PARENTHESIS, StringPool.AMPERSAND,
			StringPool.OPEN_PARENTHESIS, _K8S_CONFIG_UID, StringPool.EQUAL,
			metadata.getUid(), StringPool.CLOSE_PARENTHESIS,
			StringPool.OPEN_PARENTHESIS, _K8S_CONFIG_RESOURCE_VERSION,
			StringPool.EQUAL, oldMetadata.getResourceVersion(),
			StringPool.CLOSE_PARENTHESIS, StringPool.CLOSE_PARENTHESIS
		);

		try {
			Configuration[] configurations =
				_configurationAdmin.listConfigurations(configurationFilter);

			if (configurations != null) {
				for (Configuration configuration : configurations) {
					try {
						configuration.delete();
					}
					catch (Exception exception) {
						_log.error(exception);
					}
				}
			}
		}
		catch (Exception exception) {
			_log.error(exception);
		}
	}

	private String[] _parsePid(String path) {
		String pid = path;

		if (path.endsWith(_FILE_EXT)) {
			pid = path.substring(0, path.length() - _FILE_EXT.length());
		}
		else if (path.endsWith(_FILE_JSON_EXT)) {
			pid = path.substring(0, path.length() - _FILE_JSON_EXT.length());
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

			return new String[] {pid, name};
		}

		return new String[] {pid, null};
	}

	private void _processConfigMapConfigFileEntry(
			String configName, Dictionary<String, Object> dictionary,
			ObjectMeta metadata)
		throws Exception {

		String resourceVersion = metadata.getResourceVersion();

		String[] pid = _parsePid(configName);

		Configuration configuration = _findExistingConfiguration(configName);

		if (configuration == null) {
			configuration = _getConfiguration(pid[0], pid[1]);
		}
		else {
			Dictionary<String, Object> properties =
				configuration.getProperties();

			String existingResourceVersion = GetterUtil.getString(
				properties.get(_K8S_CONFIG_RESOURCE_VERSION));

			if (Objects.equals(resourceVersion, existingResourceVersion)) {
				if (_log.isDebugEnabled()) {
					_log.debug(
						StringBundler.concat(
							"The resourceVersion of the configuration (",
							existingResourceVersion,
							") is same as that of Kubernetes (",
							resourceVersion,
							") so this action will be ignored"));
				}

				return;
			}
		}

		Set<Configuration.ConfigurationAttribute> configurationAttributes =
			configuration.getAttributes();

		if (configurationAttributes.contains(
				Configuration.ConfigurationAttribute.READ_ONLY)) {

			configuration.removeAttributes(
				Configuration.ConfigurationAttribute.READ_ONLY);
		}

		Map<String, String> labels = metadata.getLabels();

		for (String key : labels.keySet()) {
			String modifiedKey = _K8S_PROPERTY_KEY.concat(key);

			if (key.contains(StringPool.SLASH)) {
				modifiedKey = StringUtil.replace(
					key, CharPool.SLASH , CharPool.PERIOD);
			}

			dictionary.put(modifiedKey, labels.get(key));
		}

		Map<String, String> annotations = metadata.getAnnotations();

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject(
			annotations.get(_CONTEXT_ANNOTATION));

		String environment = jsonObject.getString("environment", "default");

		dictionary.put(
			_K8S_PROPERTY_KEY.concat("lxc.environment"), environment);

		Company company = _getCompanyIdByEnvironment(environment);

		dictionary.put("companyId", company.getCompanyId());

		List<String> serviceDomains = new ArrayList<>();

		JSONArray jsonArray = jsonObject.getJSONArray("domains");

		for (int i = 0; i < jsonArray.length(); i++) {
			serviceDomains.add(jsonArray.getString(i));

			if (i == 0) {
				dictionary.put(
					"host.service.address",
					"https://".concat(jsonArray.getString(i)));
			}
		}

		dictionary.put(
			"host.service.domains", serviceDomains.toArray(new String[0]));

		dictionary.put(_K8S_CONFIG_KEY, configName);
		dictionary.put(_K8S_CONFIG_UID, metadata.getUid());
		dictionary.put(_K8S_CONFIG_RESOURCE_VERSION, resourceVersion);

		if (_log.isDebugEnabled()) {
			_log.debug("Created Configuration " + dictionary);
		}

		configuration.updateIfDifferent(dictionary);

		configuration.addAttributes(
			Configuration.ConfigurationAttribute.READ_ONLY);
	}

	private void _processConfigMapConfigJSONResource(
			final String configName, final String configurationContent,
			final ConfigMap configMap)
		throws Exception {

		final URL url = new URL("file", null, configName);
		final Report report = new JSONUtil.Report();
		final BinaryManager binaryManager = new BinaryManager(new BinUtil.ResourceProvider() {

			@Override
			public long getBundleId() {
				return _bundle.getBundleId();
			}

			@Override
			public URL getEntry(String path) {
				// TODO figure this out...
				return null;
			}

			@Override
			public String getIdentifier() {
				return configName;
			}

			@Override
			public Enumeration<URL> findEntries(String path, String filePattern) {
				// TODO figure this out...
				return Collections.emptyEnumeration();
			}

		},
		report);

		ConfigurationFile configurationFile = JSONUtil.readJSON(
			binaryManager, configName, url, _bundle.getBundleId(),
			configurationContent, report);

		for(final String warning : report.warnings) {
			if (_log.isWarnEnabled()) {
				_log.warn(warning);
			}
		}
		for(final String error : report.errors) {
			if (_log.isErrorEnabled()) {
				_log.error(error);
			}
		}

		if (configurationFile == null) {
			return;
		}

		for (org.apache.felix.configurator.impl.model.Config config :
				configurationFile.getConfigurations()) {

			try {
				_processConfigMapConfigFileEntry(
					config.getPid(), config.getProperties(),
					configMap.getMetadata());
			}
			catch (Exception exception) {
				_log.error(exception);
			}

		}
	}

	private static final String _AGENT_NAME = "Kubernetes Configuration Agent";

	private static final String _CONTEXT_ANNOTATION =
		"cloud.liferay.com/context-data";

	private static final String _FILE_EXT = ".config";

	private static final String _FILE_JSON_EXT = ".config.json";

	private static final String _K8S_CONFIG_KEY =
		".kubernetes.config.key";

	private static final String _K8S_CONFIG_RESOURCE_VERSION =
		".kubernetes.config.resource.version";

	private static final String _K8S_CONFIG_UID =
		".kubernetes.config.uid";

	private static final String _K8S_PROPERTY_KEY = "k8s.";

	private static final Log _log = LogFactoryUtil.getLog(K8sAgentImpl.class);

	private final Bundle _bundle;
	private final CompanyLocalService _companyLocalService;
	private final ConfigurationAdmin _configurationAdmin;
	private final SharedIndexInformer<ConfigMap> _sharedIndexInformer;
	private final K8sAgentConfiguration _k8sAgentConfiguration;
	private final KubernetesClient _kubernetesClient;

}