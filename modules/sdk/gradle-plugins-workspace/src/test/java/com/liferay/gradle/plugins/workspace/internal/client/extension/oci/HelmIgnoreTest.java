/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import org.gradle.api.GradleException;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Gregory Amerson
 */
public class HelmIgnoreTest {

	@Test
	public void testIsIgnored() {
		HelmIgnore helmIgnore = new HelmIgnore(
			"# Comment\n\ntests/\n*.md\n/values.schema.json\n" +
				"templates/*.txt\n");

		Assert.assertTrue(helmIgnore.isIgnored("charts/able/tests/a.yaml"));
		Assert.assertTrue(helmIgnore.isIgnored("docs/README.md"));
		Assert.assertTrue(helmIgnore.isIgnored("templates/.hidden"));
		Assert.assertTrue(helmIgnore.isIgnored("templates/NOTES.txt"));
		Assert.assertTrue(helmIgnore.isIgnored("tests/a.yaml"));
		Assert.assertTrue(helmIgnore.isIgnored("values.schema.json"));

		Assert.assertFalse(
			helmIgnore.isIgnored("charts/able/values.schema.json"));
		Assert.assertFalse(helmIgnore.isIgnored("templates/able/NOTES.txt"));
		Assert.assertFalse(helmIgnore.isIgnored("templates/deployment.yaml"));
		Assert.assertFalse(helmIgnore.isIgnored("tests"));
	}

	@Test
	public void testIsIgnoredWhenRuleIsNegated() {
		HelmIgnore helmIgnore = new HelmIgnore("!*.yaml\n");

		Assert.assertFalse(helmIgnore.isIgnored("Chart.yaml"));
		Assert.assertTrue(helmIgnore.isIgnored("README.md"));
	}

	@Test
	public void testIsIgnoredWhenRuleUsesDoubleStar() {
		try {
			new HelmIgnore("**/tests\n");

			Assert.fail();
		}
		catch (GradleException gradleException) {
			String message = gradleException.getMessage();

			Assert.assertTrue(message, message.contains("\"**\""));
		}
	}

}