package com.acme.overlay.demo;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

/**
 * Exists only while its configuration does, so the log shows when an overlay
 * configuration is applied, changed, and removed.
 */
@Component(
	configurationPid = "com.acme.overlay.demo.DemoConfiguration",
	configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true,
	service = DemoConfiguredComponent.class
)
public class DemoConfiguredComponent {

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		System.out.println(
			"[acme-overlay-demo] Configured build " +
				DemoBundleComponent.getBuild() + " with greeting \"" +
					properties.get("greeting") + "\"");
	}

	@Deactivate
	protected void deactivate() {
		System.out.println("[acme-overlay-demo] Configuration removed");
	}

}