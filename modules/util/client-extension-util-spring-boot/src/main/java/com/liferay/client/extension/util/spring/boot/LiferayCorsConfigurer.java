package com.liferay.client.extension.util.spring.boot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.ArrayList;
import java.util.List;


@Configuration
@Order(Ordered.LOWEST_PRECEDENCE)
public class LiferayCorsConfigurer implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping(
			"/**"
		).allowedHeaders(
			"Authorization", "Content-Type"
		).allowedMethods(
			"DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"
		).allowedOrigins(
			_getAllowedOrigins().toArray(new String[0])
		);
	}

	private List<String> _getAllowedOrigins() {
		List<String> allowedOrigins = new ArrayList<>();

		for (String lxcDXPDomain : _lxcDXPDomains.split("\\s*[,\n]\\s*")) {
			allowedOrigins.add("http://" + lxcDXPDomain);
			allowedOrigins.add("https://" + lxcDXPDomain);
		}

		return allowedOrigins;
	}


	@Value("${com.liferay.lxc.dxp.domains}")
	private String _lxcDXPDomains;
}