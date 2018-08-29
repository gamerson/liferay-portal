package com.liferay.project.templates.internal;

import com.liferay.project.templates.internal.util.ReflectionUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;

import org.apache.maven.archetype.ArchetypeManager;
import org.apache.maven.archetype.DefaultArchetypeManager;
import org.apache.maven.archetype.common.ArchetypeArtifactManager;
import org.apache.maven.archetype.common.DefaultArchetypeFilesResolver;
import org.apache.maven.archetype.common.DefaultPomManager;
import org.apache.maven.archetype.generator.ArchetypeGenerator;
import org.apache.maven.archetype.generator.DefaultArchetypeGenerator;
import org.apache.maven.archetype.generator.DefaultFilesetArchetypeGenerator;
import org.apache.maven.archetype.generator.FilesetArchetypeGenerator;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.AbstractLogger;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.velocity.DefaultVelocityComponent;
import org.codehaus.plexus.velocity.VelocityComponent;

public class Archetyper {

	private static final Field _loggerField;

	static {
		try {
			_loggerField = ReflectionUtil.getField(
				AbstractLogEnabled.class, "logger");
		}
		catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final static Logger _logger = new ArchetyperLogger();
	private List<File> _archetypesDirs;

	public Archetyper(List<File> archetypesDirs) {
		_archetypesDirs = archetypesDirs;
	}

	public ArchetypeArtifactManager createArchetypeArtifactManager()
		throws Exception {

		ArchetypeArtifactManager archetypeArtifactManager = newArchetypeArtifactManager();

		ReflectionUtil.setFieldValue(
			_loggerField, archetypeArtifactManager, _logger);

		return archetypeArtifactManager;
	}

	protected ArchetypeArtifactManager newArchetypeArtifactManager() throws Exception {
		return new ArchetyperArchetypeArtifactManager(_archetypesDirs);
	}

	private ArchetypeGenerator _createArchetypeGenerator(
			List<File> archetypesDirs)
		throws Exception {

		ArchetypeGenerator archetypeGenerator = new DefaultArchetypeGenerator();

		ArchetypeArtifactManager archetypeArtifactManager =
			createArchetypeArtifactManager();

		ReflectionUtil.setFieldValue(
			DefaultArchetypeGenerator.class, "archetypeArtifactManager",
			archetypeGenerator, archetypeArtifactManager);
		ReflectionUtil.setFieldValue(
			DefaultArchetypeGenerator.class, "filesetGenerator",
			archetypeGenerator,
			_createFilesetArchetypeGenerator(archetypeArtifactManager));

		return archetypeGenerator;
	}

	public ArchetypeManager createArchetypeManager() throws Exception {
		DefaultArchetypeManager archetypeManager =
			new DefaultArchetypeManager();

		ReflectionUtil.setFieldValue(_loggerField, archetypeManager, _logger);
		ReflectionUtil.setFieldValue(
			DefaultArchetypeManager.class, "generator", archetypeManager,
			_createArchetypeGenerator(_archetypesDirs));

		return archetypeManager;
	}

	private FilesetArchetypeGenerator _createFilesetArchetypeGenerator(
			ArchetypeArtifactManager archetypeArtifactManager)
		throws Exception {

		FilesetArchetypeGenerator filesetArchetypeGenerator =
			new DefaultFilesetArchetypeGenerator();

		ReflectionUtil.setFieldValue(
			_loggerField, filesetArchetypeGenerator, _logger);
		ReflectionUtil.setFieldValue(
			DefaultFilesetArchetypeGenerator.class, "archetypeArtifactManager",
			filesetArchetypeGenerator, archetypeArtifactManager);
		ReflectionUtil.setFieldValue(
			DefaultFilesetArchetypeGenerator.class, "archetypeFilesResolver",
			filesetArchetypeGenerator, new DefaultArchetypeFilesResolver());
		ReflectionUtil.setFieldValue(
			DefaultFilesetArchetypeGenerator.class, "pomManager",
			filesetArchetypeGenerator, new DefaultPomManager());
		ReflectionUtil.setFieldValue(
			DefaultFilesetArchetypeGenerator.class, "velocity",
			filesetArchetypeGenerator, createVelocityComponent());

		return filesetArchetypeGenerator;
	}

	public static VelocityComponent createVelocityComponent() throws Exception {
		DefaultVelocityComponent defaultVelocityComponent =
			new DefaultVelocityComponent();

		ReflectionUtil.setFieldValue(
			_loggerField, defaultVelocityComponent, _logger);

		Properties properties = new Properties();

		properties.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		properties.setProperty(
			"classpath.resource.loader.class",
			ClasspathResourceLoader.class.getName());

		ReflectionUtil.setFieldValue(
			DefaultVelocityComponent.class, "properties",
			defaultVelocityComponent, properties);

		defaultVelocityComponent.initialize();

		return defaultVelocityComponent;
	}

	private static class ArchetyperLogger extends AbstractLogger {

		public ArchetyperLogger() {
			super(0, "archetyper");
		}

		@Override
		public void debug(String message, Throwable throwable) {
		}

		@Override
		public void error(String message, Throwable throwable) {
		}

		@Override
		public void fatalError(String message, Throwable throwable) {
		}

		@Override
		public Logger getChildLogger(String name) {
			return this;
		}

		@Override
		public void info(String message, Throwable throwable) {
		}

		@Override
		public void warn(String message, Throwable throwable) {
		}

	}
}