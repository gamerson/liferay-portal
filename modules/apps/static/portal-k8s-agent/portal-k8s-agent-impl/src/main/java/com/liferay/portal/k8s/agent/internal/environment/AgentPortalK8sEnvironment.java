/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.agent.internal.environment;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.k8s.agent.configuration.PortalK8sAgentConfiguration;
import com.liferay.portal.k8s.agent.environment.PortalK8sEnvironment;
import com.liferay.portal.k8s.agent.internal.util.KubernetesClientConfigUtil;
import com.liferay.portal.kernel.license.LicenseEnvironmentProvider;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

/**
 * @author Gregory Amerson
 */
@Component(
	configurationPid = "com.liferay.portal.k8s.agent.configuration.PortalK8sAgentConfiguration",
	configurationPolicy = ConfigurationPolicy.REQUIRE,
	service = {LicenseEnvironmentProvider.class, PortalK8sEnvironment.class}
)
public class AgentPortalK8sEnvironment
	implements LicenseEnvironmentProvider, PortalK8sEnvironment {

	@Activate
	public AgentPortalK8sEnvironment(Map<String, Object> properties) {
		_portalK8sAgentConfiguration = ConfigurableUtil.createConfigurable(
			PortalK8sAgentConfiguration.class, properties);
	}

	@Override
	public String getEnvironmentId() {
		String environmentId = _environmentId;

		if (environmentId != null) {
			return environmentId;
		}

		// A namespace UID never changes, so a resolved environment ID is cached
		// for the life of the component. An unresolved one is not cached, so
		// that a revoked service account or an API server outage does not
		// permanently degrade this node.

		_lock.lock();

		try {
			if (_environmentId == null) {
				_environmentId = _getNamespaceUID();
			}

			return _environmentId;
		}
		finally {
			_lock.unlock();
		}
	}

	@Override
	public String getNamespace() {
		return _portalK8sAgentConfiguration.namespace();
	}

	private String _getNamespaceUID() {
		String namespace = _portalK8sAgentConfiguration.namespace();

		try (KubernetesClient kubernetesClient = new DefaultKubernetesClient(
				KubernetesClientConfigUtil.toConfig(
					_portalK8sAgentConfiguration))) {

			Namespace namespaceResource = kubernetesClient.namespaces(
			).withName(
				namespace
			).get();

			if (namespaceResource == null) {
				_log.error(
					StringBundler.concat(
						"Unable to read namespace \"", namespace, "\""));

				return null;
			}

			ObjectMeta objectMeta = namespaceResource.getMetadata();

			String uid = null;

			if (objectMeta != null) {
				uid = objectMeta.getUid();
			}

			if (Validator.isNull(uid)) {
				_log.error(
					StringBundler.concat(
						"Namespace \"", namespace, "\" has no UID"));

				return null;
			}

			if (_log.isInfoEnabled()) {
				_log.info(
					StringBundler.concat(
						"Resolved environment ID \"", uid,
						"\" from namespace \"", namespace, "\""));
			}

			return uid;
		}
		catch (Exception exception) {
			_log.error(
				StringBundler.concat(
					"Unable to read the UID of namespace \"", namespace,
					"\". The service account may not be granted \"get\" on ",
					"the \"namespaces\" resource."),
				exception);

			return null;
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		AgentPortalK8sEnvironment.class);

	private volatile String _environmentId;
	private final Lock _lock = new ReentrantLock();
	private final PortalK8sAgentConfiguration _portalK8sAgentConfiguration;

}