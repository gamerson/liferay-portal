package com.acme.overlay.demo;

import java.io.IOException;
import java.io.InputStream;

import java.util.Properties;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Reports which build of the bundle is running, whether or not it is
 * configured.
 */
@Component(immediate = true, service = DemoBundleComponent.class)
public class DemoBundleComponent {

	public static String getBuild() {
		Properties properties = new Properties();

		try (InputStream inputStream =
				DemoBundleComponent.class.getResourceAsStream(
					"/acme-overlay-demo.properties")) {

			properties.load(inputStream);
		}
		catch (IOException ioException) {
			return "unknown";
		}

		return properties.getProperty("build");
	}

	@Activate
	protected void activate() {
		System.out.println("[acme-overlay-demo] Started build " + getBuild());
	}

	@Deactivate
	protected void deactivate() {
		System.out.println("[acme-overlay-demo] Stopped build " + getBuild());
	}

}