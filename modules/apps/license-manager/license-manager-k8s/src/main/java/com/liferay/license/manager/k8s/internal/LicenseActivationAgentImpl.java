/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.license.manager.k8s.internal;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.k8s.agent.licensing.LicenseActivationAgent;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.Validator;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Talks to the API server as the pod's own service account. Nothing here is
 * configured: <code>Config.autoConfigure</code> reads the token, the CA
 * certificate, and the namespace the kubelet mounts into every pod, so the
 * component is inert outside a cluster and needs no setup inside one.
 *
 * @author Gregory Amerson
 */
@Component(service = LicenseActivationAgent.class)
public class LicenseActivationAgentImpl implements LicenseActivationAgent {

	@Override
	public ActivationWorkflow getActivationWorkflow(String workflowName) {
		if (!_isAvailable() || Validator.isNull(workflowName)) {
			return null;
		}

		try {
			Map<String, Object> workflow = _kubernetesClient.customResource(
				_workflowContext()
			).get(
				_namespace, workflowName
			);

			return _toActivationWorkflow(workflow);
		}
		catch (Exception exception) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Unable to read the workflow " + workflowName, exception);
			}

			return null;
		}
	}

	@Override
	public LiferayEnvironment getLiferayEnvironment() {
		if (!_isAvailable()) {
			return null;
		}

		try {
			Map<String, Object> liferayEnvironments =
				_kubernetesClient.customResource(
					_liferayEnvironmentContext()
				).list(
					_namespace
				);

			List<Map<String, Object>> items =
				(List<Map<String, Object>>)liferayEnvironments.get("items");

			if (ListUtil.isEmpty(items)) {
				return null;
			}

			return _toLiferayEnvironment(items.get(0));
		}
		catch (Exception exception) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Unable to list the Liferay environments in " + _namespace,
					exception);
			}

			return null;
		}
	}

	@Override
	public String getOfflineActivationRequest() {
		LiferayEnvironment liferayEnvironment = getLiferayEnvironment();

		if (liferayEnvironment == null) {
			return null;
		}

		try {
			Secret secret = _kubernetesClient.secrets(
			).inNamespace(
				_namespace
			).withName(
				liferayEnvironment.name() + _IDENTITY_SECRET_SUFFIX
			).get();

			if (secret == null) {
				return null;
			}

			Map<String, String> data = secret.getData();

			if (data == null) {
				return null;
			}

			String offlineRequest = data.get(_OFFLINE_REQUEST_KEY);

			if (Validator.isNull(offlineRequest)) {
				return null;
			}

			Base64.Decoder decoder = Base64.getDecoder();

			return new String(decoder.decode(offlineRequest));
		}
		catch (Exception exception) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Unable to read the offline activation request", exception);
			}

			return null;
		}
	}

	@Override
	public ActivationWorkflow startOfflineActivation(String bundleFileName) {
		if (Validator.isNull(bundleFileName)) {
			return null;
		}

		return _submitActivationWorkflow(
			"bundle-name", bundleFileName, _OFFLINE_WORKFLOW_TEMPLATE_NAME,
			"offline-activation-");
	}

	@Override
	public ActivationWorkflow startOnlineActivation(String activationCode) {
		if (Validator.isNull(activationCode)) {
			return null;
		}

		return _submitActivationWorkflow(
			"activation-code", activationCode, _ONLINE_WORKFLOW_TEMPLATE_NAME,
			"online-activation-");
	}

	@Override
	public String storeOfflineActivationBundle(
		String fileName, InputStream inputStream) {

		if (Validator.isNull(fileName) || (inputStream == null)) {
			return null;
		}

		String bundleFileName = _toBundleFileName(fileName);

		try {
			Path marketplacePath = Paths.get(_marketplacePath());

			Files.createDirectories(marketplacePath);

			Path stagedPath = marketplacePath.resolve(
				StringPool.PERIOD + bundleFileName);

			Files.copy(
				inputStream, stagedPath, StandardCopyOption.REPLACE_EXISTING);

			Files.move(
				stagedPath, marketplacePath.resolve(bundleFileName),
				StandardCopyOption.ATOMIC_MOVE,
				StandardCopyOption.REPLACE_EXISTING);

			return bundleFileName;
		}
		catch (Exception exception) {
			_log.error(
				"Unable to store the offline activation bundle " +
					bundleFileName,
				exception);

			return null;
		}
	}

	@Activate
	protected void activate() {
		try {
			Config config = Config.autoConfigure(null);

			if (Validator.isNull(config.getMasterUrl()) ||
				Validator.isNull(config.getOauthToken())) {

				if (_log.isInfoEnabled()) {
					_log.info(
						"The portal does not run inside a Kubernetes cluster");
				}

				return;
			}

			_kubernetesClient = new DefaultKubernetesClient(config);

			_namespace = GetterUtil.getString(
				config.getNamespace(), _kubernetesClient.getNamespace());

			if (_log.isInfoEnabled()) {
				_log.info(
					"Initialized the license activation agent in " +
						_namespace);
			}
		}
		catch (Throwable throwable) {
			_log.error(
				"Unable to initialize the license activation agent", throwable);
		}
	}

	@Deactivate
	protected void deactivate() {
		if (_kubernetesClient != null) {
			_kubernetesClient.close();

			_kubernetesClient = null;
		}
	}

	private boolean _isAvailable() {
		if ((_kubernetesClient == null) || Validator.isNull(_namespace)) {
			return false;
		}

		return true;
	}

	private CustomResourceDefinitionContext _liferayEnvironmentContext() {
		CustomResourceDefinitionContext.Builder builder =
			new CustomResourceDefinitionContext.Builder();

		return builder.withGroup(
			"licensing.liferay.com"
		).withKind(
			"LiferayEnvironment"
		).withName(
			"liferayenvironments.licensing.liferay.com"
		).withPlural(
			"liferayenvironments"
		).withScope(
			"Namespaced"
		).withVersion(
			"v1alpha1"
		).build();
	}

	private String _marketplacePath() {
		return GetterUtil.getString(
			System.getenv("LIFERAY_MARKETPLACE_PATH"), _MARKETPLACE_PATH);
	}

	private ActivationWorkflow _submitActivationWorkflow(
		String parameterName, String parameterValue,
		String workflowTemplateName, String generateName) {

		LiferayEnvironment liferayEnvironment = getLiferayEnvironment();

		if (liferayEnvironment == null) {
			return null;
		}

		try {
			Map<String, Object> workflow = _kubernetesClient.customResource(
				_workflowContext()
			).create(
				_namespace,
				_toWorkflowJSON(
					generateName, liferayEnvironment.name(), parameterName,
					parameterValue, workflowTemplateName)
			);

			return _toActivationWorkflow(workflow);
		}
		catch (Exception exception) {
			_log.error(
				"Unable to submit the workflow " + workflowTemplateName,
				exception);

			return null;
		}
	}

	private ActivationWorkflow _toActivationWorkflow(
		Map<String, Object> workflow) {

		if (workflow == null) {
			return null;
		}

		Map<String, Object> metadata = (Map<String, Object>)workflow.get(
			"metadata");
		Map<String, Object> status = (Map<String, Object>)workflow.get(
			"status");

		String name = GetterUtil.getString(
			(metadata == null) ? null : metadata.get("name"));
		String phase = GetterUtil.getString(
			(status == null) ? null : status.get("phase"), "Pending");

		return new ActivationWorkflow() {

			@Override
			public boolean isFinished() {
				if (phase.equals("Succeeded") || phase.equals("Failed") ||
					phase.equals("Error")) {

					return true;
				}

				return false;
			}

			@Override
			public String name() {
				return name;
			}

			@Override
			public String phase() {
				return phase;
			}

			@Override
			public String url() {
				String argoBaseURL = System.getenv("LIFERAY_ARGO_BASE_URL");

				if (Validator.isNull(argoBaseURL)) {
					return null;
				}

				return StringBundler.concat(
					argoBaseURL, "/workflows/", _namespace, "/", name);
			}

		};
	}

	private String _toBundleFileName(String fileName) {
		String bundleFileName = fileName.replaceAll("[^A-Za-z0-9._-]", "-");

		if (!bundleFileName.endsWith(".zip")) {
			bundleFileName = bundleFileName + ".zip";
		}

		return bundleFileName;
	}

	private LiferayEnvironment _toLiferayEnvironment(
		Map<String, Object> liferayEnvironment) {

		Map<String, Object> metadata =
			(Map<String, Object>)liferayEnvironment.get("metadata");

		Map<String, Object> spec = (Map<String, Object>)liferayEnvironment.get(
			"spec");

		Map<String, Object> status =
			(Map<String, Object>)liferayEnvironment.get("status");

		Map<String, Object> safeSpec = (spec == null) ? new HashMap<>() : spec;
		Map<String, Object> safeStatus =
			(status == null) ? new HashMap<>() : status;

		String name = GetterUtil.getString(
			(metadata == null) ? null : metadata.get("name"));

		return new LiferayEnvironment() {

			@Override
			public String activatedAt() {
				return GetterUtil.getString(safeStatus.get("activatedAt"));
			}

			@Override
			public String environmentName() {
				return GetterUtil.getString(safeSpec.get("environmentName"));
			}

			@Override
			public boolean isActivated() {
				return Validator.isNotNull(activatedAt());
			}

			@Override
			public boolean isOffline() {
				return GetterUtil.getBoolean(safeSpec.get("offline"));
			}

			@Override
			public String name() {
				return name;
			}

			@Override
			public String namespace() {
				return _namespace;
			}

			@Override
			public String offlineActivationBundle() {
				return GetterUtil.getString(
					safeSpec.get("offlineActivationBundle"));
			}

			@Override
			public String phase() {
				return GetterUtil.getString(safeStatus.get("phase"), "Unknown");
			}

		};
	}

	private String _toWorkflowJSON(
		String generateName, String liferayEnvironmentName,
		String parameterName, String parameterValue,
		String workflowTemplateName) {

		StringBuilder sb = new StringBuilder();

		sb.append("{\"apiVersion\": \"argoproj.io/v1alpha1\", ");
		sb.append("\"kind\": \"Workflow\", \"metadata\": {");
		sb.append("\"generateName\": \"");
		sb.append(generateName);
		sb.append("\", \"labels\": {");
		sb.append("\"licensing.liferay.com/submitted-by\": ");
		sb.append("\"dxp\"}}, \"spec\": {\"arguments\": {\"parameters\": [");
		sb.append("{\"name\": \"");
		sb.append(parameterName);
		sb.append("\", \"value\": \"");
		sb.append(parameterValue);
		sb.append("\"}, {\"name\": \"liferay-environment-name\", ");
		sb.append("\"value\": \"");
		sb.append(liferayEnvironmentName);
		sb.append("\"}]}, \"workflowTemplateRef\": {\"name\": \"");
		sb.append(workflowTemplateName);
		sb.append("\"}}}");

		return sb.toString();
	}

	private CustomResourceDefinitionContext _workflowContext() {
		CustomResourceDefinitionContext.Builder builder =
			new CustomResourceDefinitionContext.Builder();

		return builder.withGroup(
			"argoproj.io"
		).withKind(
			"Workflow"
		).withName(
			"workflows.argoproj.io"
		).withPlural(
			"workflows"
		).withScope(
			"Namespaced"
		).withVersion(
			"v1alpha1"
		).build();
	}

	private static final String _IDENTITY_SECRET_SUFFIX = "-identity";

	private static final String _MARKETPLACE_PATH = "/marketplace";

	private static final String _OFFLINE_REQUEST_KEY = "offline-request";

	private static final String _OFFLINE_WORKFLOW_TEMPLATE_NAME =
		"offline-activation-workflow-template";

	private static final String _ONLINE_WORKFLOW_TEMPLATE_NAME =
		"online-activation-workflow-template";

	private static final Log _log = LogFactoryUtil.getLog(
		LicenseActivationAgentImpl.class);

	private KubernetesClient _kubernetesClient;
	private String _namespace;

}