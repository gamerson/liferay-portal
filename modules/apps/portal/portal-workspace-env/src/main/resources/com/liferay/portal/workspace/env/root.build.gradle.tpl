subprojects {
	plugins.withId("com.liferay.portal.tools.service.builder") {
		dependencies {
			serviceBuilder fileTree(dir: "%portal_workspace_env_jars_dir%", include: ["com.liferay.portal.tools.service.builder-*.jar"])
		}
	}

	plugins.withType(JavaPlugin) {
		dependencies {
			compileOnly fileTree(dir: "%portal_workspace_env_jars_dir%", include: ["*.jar"])
			compileOnly fileTree(dir: "%osgi_core_dir%", include: ["org.eclipse.osgi.jar"])
			compileOnly fileTree(dir: "%tomcat_lib_ext_dir%", include: ["com.liferay.*.jar", "portal-kernel.jar"])
		}
	}
}