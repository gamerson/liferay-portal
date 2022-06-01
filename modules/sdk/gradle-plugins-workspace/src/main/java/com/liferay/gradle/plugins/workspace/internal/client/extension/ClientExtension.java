package com.liferay.gradle.plugins.workspace.internal.client.extension;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.StringWriter;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientExtension {

	@JsonAnySetter
	public void ignored(String name, Object value) {
		typeSettings.put(name, value);
	}

	public String toJSON() throws Exception {
		Map<String, Object> config = new HashMap<>();

		config.put("__baseURL", _BASE_URL_PREFIX + projectName);
		config.put("__timestamp", "${tstamp}");
		config.put("description", description);
		config.put("name", name);
		config.put("sourceCodeUrl", sourceCodeUrl);
		config.put("type", type);
		config.put("typeSettings", typeSettings);

		Map<String, Object> json = new HashMap<>();

		json.put(_CLIENT_EXTENSION_FACTORY_PREFIX + id, config);

		ObjectMapper objectMapper = new ObjectMapper();

		StringWriter sw = new StringWriter();

		objectMapper.writeValue(sw, json);

		return sw.toString();
	}

	public String id;
	public String description;
	public String name;
	public String projectName;
	public String sourceCodeUrl;
	public String type;
	public Map<String, Object> typeSettings = new HashMap<>();

	private static final String _BASE_URL_PREFIX =
		"${portalURL}${pathContext}/o/";

	private static final String _CLIENT_EXTENSION_FACTORY_PREFIX =
		"com.liferay.client.extensions.factory.configuration.ClientExtensionConfiguration~";

}