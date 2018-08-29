package com.liferay.project.templates.internal;

import com.liferay.project.templates.CodeTemplatesArgs;
import com.liferay.project.templates.ProjectTemplates;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.archetype.ArchetypeManager;
import org.apache.maven.archetype.common.ArchetypeArtifactManager;
import org.apache.maven.archetype.exception.UnknownArchetype;
import org.apache.maven.archetype.metadata.ArchetypeDescriptor;
import org.apache.maven.archetype.metadata.FileSet;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.codehaus.plexus.velocity.VelocityComponent;

public class CodeGenerator {

	public void generateCode(
			CodeTemplatesArgs codeTemplatesArgs, File destinationDir)
		throws Exception {

		List<File> archetypesDirs = codeTemplatesArgs.getArchetypesDirs();
		String packageName = codeTemplatesArgs.getPackageName();

		ArchetypeGenerationRequest archetypeGenerationRequest =
				new ArchetypeGenerationRequest();

		archetypeGenerationRequest.setArchetypeArtifactId(
			ProjectTemplates.TEMPLATE_BUNDLE_PREFIX + "code");

		archetypeGenerationRequest.setArchetypeGroupId("com.liferay");

		// archetypeVersion is ignored

		archetypeGenerationRequest.setArchetypeVersion("0");

		archetypeGenerationRequest.setArtifactId("code");
		archetypeGenerationRequest.setGroupId("com.liferay");
		archetypeGenerationRequest.setInteractiveMode(false);
		archetypeGenerationRequest.setOutputDirectory(destinationDir.getPath());
		archetypeGenerationRequest.setPackage(packageName);

		VelocityComponent velocityComponent = Archetyper.createVelocityComponent();

		Archetyper archetyper = new Archetyper(archetypesDirs) {
			@Override
			protected ArchetypeArtifactManager newArchetypeArtifactManager() throws Exception {
				return new ArchetyperArchetypeArtifactManager(archetypesDirs) {
					@Override
					public ArchetypeDescriptor getFileSetArchetypeDescriptor(File archetypeFile) throws UnknownArchetype {
						ArchetypeDescriptor archetypeDescriptor = super.getFileSetArchetypeDescriptor(archetypeFile);

						for (FileSet fileSet : archetypeDescriptor.getFileSets()) {
							List<String> excludes = fileSet.getExcludes();

							Stream<String> stream = excludes.stream();

							List<String> newExcludes = stream.map(
								this::filterElement
							).collect(
								Collectors.toList()
							);

							fileSet.setExcludes(newExcludes);

							List<String> includes = fileSet.getIncludes();

							stream = includes.stream();

							List<String> newIncludes= stream.map(
								this::filterElement
							).collect(
								Collectors.toList()
							);

							fileSet.setIncludes(newIncludes);
						}

						return archetypeDescriptor;
					}

					private String filterElement(String element) {
						VelocityEngine velocityEngine = velocityComponent.getEngine();

						VelocityContext velocityContext = new VelocityContext();
						velocityContext.put("codeTemplate", codeTemplatesArgs.getTemplate());

						StringWriter stringWriter = new StringWriter();

						try {
							 boolean success = velocityEngine.evaluate(velocityContext, stringWriter, "CodeGenerator", element);

							 if (success) {
								 return stringWriter.toString();
							 }
						}
						catch (IOException e) {
						}

						return element;
					}
				};
			}
		};

		ArchetypeManager archetypeManager = archetyper.createArchetypeManager();

		archetypeManager.generateProjectFromArchetype(archetypeGenerationRequest);
	}
}
