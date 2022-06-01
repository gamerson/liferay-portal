package com.liferay.gradle.plugins.workspace.internal.client.extension;

import org.gradle.api.Project;

public interface ClientExtensionConfigurer {

	public void apply(Project project, ClientExtension clientExtension);

}