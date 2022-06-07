package com.liferay.ant.bnd.client.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Annotation;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.FieldDef;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.AnalyzerPlugin;

/**
 * @author Gregory Amerson
 */
public class ClientExtensionAnalyzerPlugin implements AnalyzerPlugin {

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		Parameters parameters = OSGiHeader.parseHeader(analyzer.getProperty("-client-extension"));

		if (parameters.isEmpty()) {
			return false;
		}
		
		List<Map<String, Object>> clientExtensions = new ArrayList<>();
		
		Instructions instructions = new Instructions(parameters);
		
		String filter = instructions.keySet().iterator().next().getLiteral();
		
		Collection<Clazz> cetTypes = analyzer.getClasses("", Clazz.QUERY.ANNOTATED.toString(), "com.liferay.client.extension.type.annotation.CETType");

		for (Clazz clazz : cetTypes) {
			if (clazz.getFQN().startsWith(filter)) {
				Stream<Annotation> annotations = clazz.annotations("com/liferay/client/extension/type/annotation/CETType");

				Optional<Annotation> maybeTypeAnnotation= annotations.findAny();
				
				if (maybeTypeAnnotation.isPresent()) {
					Annotation cetTypeAnnotation = maybeTypeAnnotation.get();
					
					Map<String, Object> clientExtensionTypeMap = new HashMap<>();
					clientExtensionTypeMap.put("name", cetTypeAnnotation.get("name"));
					clientExtensionTypeMap.put("description", cetTypeAnnotation.get("description"));
					
					clazz.fields().forEach(fieldDef -> {
						Optional<Annotation> maybeFieldAnnotation = fieldDef.annotations("com/liferay/client/extension/type/annotation/CETProperty").findFirst();
						
						if (maybeFieldAnnotation.isPresent()) {
							Annotation cetPropertyAnnotation = maybeFieldAnnotation.get();
							
							Map<String, Object> properties = new HashMap<>();
							
							if (cetPropertyAnnotation.containsKey("name")) {
								properties.put("name", cetPropertyAnnotation.get("name"));
							}
							else {
								
							}
							
							if (cetPropertyAnnotation.containsKey("type")) {
								properties.put("type", cetPropertyAnnotation.get("type"));
							}
							else {
								
							}
							
							if (cetPropertyAnnotation.containsKey("default")) {
								properties.put("default", cetPropertyAnnotation.get("default"));
							}
							else {
								
							}
							
							if (cetPropertyAnnotation.containsKey("description")) {
								properties.put("description", cetPropertyAnnotation.get("description"));
							}
							else {
								
							}
						}

					});

					clientExtensionTypeMap.put("properties", cetTypes);
				}	
			}
		}
		analyzer.getJar().putResource("", "");

		return false;
	}

}
