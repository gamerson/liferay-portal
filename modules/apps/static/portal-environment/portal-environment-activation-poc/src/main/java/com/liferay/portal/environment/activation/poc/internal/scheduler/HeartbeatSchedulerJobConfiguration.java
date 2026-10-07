/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.environment.activation.poc.internal.scheduler;

import com.liferay.petra.function.UnsafeRunnable;
import com.liferay.portal.environment.activation.poc.internal.activator.LiferayEnvironmentActivator;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.scheduler.SchedulerJobConfiguration;
import com.liferay.portal.kernel.scheduler.TimeUnit;
import com.liferay.portal.kernel.scheduler.TriggerConfiguration;
import com.liferay.portal.kernel.util.GetterUtil;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Sends the provisioning heartbeat through the portal scheduler, which runs a
 * clustered job on one node only, so a cluster heartbeats once per interval.
 *
 * @author Gregory Amerson
 */
@Component(service = SchedulerJobConfiguration.class)
public class HeartbeatSchedulerJobConfiguration
	implements SchedulerJobConfiguration {

	@Override
	public UnsafeRunnable<Exception> getJobExecutorUnsafeRunnable() {
		return _liferayEnvironmentActivator::heartbeatIfActivated;
	}

	@Override
	public TriggerConfiguration getTriggerConfiguration() {
		return TriggerConfiguration.createTriggerConfiguration(
			GetterUtil.getInteger(
				System.getenv("LIFERAY_PROVISIONING_HEARTBEAT_INTERVAL"), 60),
			TimeUnit.SECOND);
	}

	@Activate
	protected void activate() {
		_liferayEnvironmentActivator = new LiferayEnvironmentActivator(
			_jsonFactory);
	}

	@Reference
	private JSONFactory _jsonFactory;

	private LiferayEnvironmentActivator _liferayEnvironmentActivator;

}