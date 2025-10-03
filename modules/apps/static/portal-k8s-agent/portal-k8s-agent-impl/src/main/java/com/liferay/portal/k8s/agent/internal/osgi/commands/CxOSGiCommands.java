/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.agent.internal.osgi.commands;

import com.liferay.osgi.util.osgi.commands.OSGiCommands;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.util.PropsValues;

import java.io.IOException;

import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Anna Zombori-Suszter
 */
@Component(
	property = {
		"osgi.command.function=listConfigurations", "osgi.command.scope=cx"
	},
	service = OSGiCommands.class
)
public class CxOSGiCommands implements OSGiCommands {

	public void listConfigurations(String... filters)
		throws InvalidSyntaxException, IOException, PortalException {

		Configuration[] cxConfigurations = _getConfigurations(filters);

		if ((cxConfigurations != null) && (cxConfigurations.length > 0)) {
			for (Configuration cxConfiguration : cxConfigurations) {
				System.out.println(_printConfiguration(cxConfiguration));
			}
		}
		else {
			System.out.println("No configurations found.");
		}
	}

	private String _formatProperties(Dictionary<String, Object> properties) {
		if (!properties.isEmpty()) {
			Enumeration<String> keysEnumeration = properties.keys();
			StringBundler sb = new StringBundler();

			while (keysEnumeration.hasMoreElements()) {
				String key = keysEnumeration.nextElement();

				sb.append(key);

				sb.append(StringPool.COLON);
				sb.append(StringPool.SPACE);

				Object value = properties.get(key);

				if (value instanceof String[]) {
					sb.append(StringPool.NEW_LINE);

					for (String element : (String[])value) {
						sb.append(StringPool.TAB);
						sb.append(element);
						sb.append(StringPool.NEW_LINE);
					}
				}
				else {
					sb.append(value.toString());
					sb.append(StringPool.NEW_LINE);
				}
			}

			return sb.toString();
		}

		return "";
	}

	private Configuration[] _getConfigurations(String... filters)
		throws InvalidSyntaxException, IOException, PortalException {

		String deploymentFilter =
			"(|(.k8s.config.key=*)" +
				"(.persistenceManager.storagePolicy=ephemeral))";

		if (filters.length > 0) {
			StringBundler otherFiltersSB = new StringBundler();
			boolean deploymentFilterIsSet = false;

			for (String filter : filters) {
				String[] splitFilter = filter.split("=");

				if (splitFilter.length == 2) {
					String key = splitFilter[0];
					String value = splitFilter[1];

					if (key.equals("deploymentType")) {
						if (!deploymentFilterIsSet) {
							if (value.equals("agent")) {
								deploymentFilter = "(.k8s.config.key=*)";
								deploymentFilterIsSet = true;
							}
							else if (value.equals("direct")) {
								deploymentFilter =
									"(.persistenceManager.storagePolicy" +
										"=ephemeral)";
								deploymentFilterIsSet = true;
							}
						}
					}
					else if (key.equals("webId")) {
						String defaultCompanyWebId =
							PropsValues.COMPANY_DEFAULT_WEB_ID;

						otherFiltersSB.append(
							"(dxp.lxc.liferay.com.virtualInstanceId=");

						if (value.equals(defaultCompanyWebId)) {
							value = "default";
						}

						otherFiltersSB.append(
							value
						).append(
							")"
						);
					}
					else if (key.equals("cxType")) {
						otherFiltersSB.append(
							"(Factory PID="
						).append(
							value
						).append(
							")"
						);
					}
					else if (key.equals("projectName")) {
						otherFiltersSB.append(
							"(projectName="
						).append(
							value
						).append(
							")"
						);
					}
				}
			}

			String finalFilter = String.format(
				"(&%s%s)", deploymentFilter, otherFiltersSB);

			System.out.println("Filter: " + finalFilter);

			return _configurationAdmin.listConfigurations(finalFilter);
		}

		return _configurationAdmin.listConfigurations(deploymentFilter);
	}

	private String _printConfiguration(Configuration cxConfiguration) {
		StringBundler sb = new StringBundler(1);

		sb.append(
			"================================================================"
		).append(
			StringPool.NEW_LINE
		).append(
			"PID: "
		).append(
			cxConfiguration.getPid()
		).append(
			StringPool.NEW_LINE
		).append(
			"Factory PID: "
		).append(
			cxConfiguration.getFactoryPid()
		).append(
			_formatProperties(cxConfiguration.getProperties())
		);

		return sb.toString();
	}

	@Reference
	private ConfigurationAdmin _configurationAdmin;

}