package com.liferay.ant.bnd.client.extension;

import java.util.Map;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.service.AnalyzerPlugin;

/**
 * @author Gregory Amerson
 */
public class ClientExtensionAnalyzerPlugin implements AnalyzerPlugin {

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		Map<TypeRef, Clazz> classspace = analyzer.getClassspace();
		
		classspace.entrySet().stream().filter(entry ->{
			return false;
		});

		return false;
	}

}
