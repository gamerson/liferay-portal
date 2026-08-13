/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.agent.internal.util;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.k8s.agent.configuration.PortalK8sAgentConfiguration;
import com.liferay.portal.kernel.util.Http;

import io.fabric8.kubernetes.client.Config;

import java.util.Map;

/**
 * @author Raymond Augé
 * @author Gregory Amerson
 */
public class KubernetesClientConfigUtil {

	public static Config toConfig(
		PortalK8sAgentConfiguration portalK8sAgentConfiguration) {

		Config config = Config.empty();

		Map<Integer, String> errorMessages = config.getErrorMessages();

		errorMessages.put(401, _ERROR_MESSAGE);
		errorMessages.put(403, _ERROR_MESSAGE);

		config.setCaCertData(portalK8sAgentConfiguration.caCertData());

		String protocol = Http.HTTP;

		if (portalK8sAgentConfiguration.apiServerSSL()) {
			protocol = Http.HTTPS;
		}

		config.setMasterUrl(
			StringBundler.concat(
				protocol, Http.PROTOCOL_DELIMITER,
				portalK8sAgentConfiguration.apiServerHost(), StringPool.COLON,
				portalK8sAgentConfiguration.apiServerPort(), StringPool.SLASH));

		config.setNamespace(portalK8sAgentConfiguration.namespace());
		config.setOauthToken(portalK8sAgentConfiguration.saToken());

		Config.configFromSysPropsOrEnvVars(config);

		return config;
	}

	private static final String _ERROR_MESSAGE =
		"Configured service account does not have access. Service account " +
			"may have been revoked.";

}