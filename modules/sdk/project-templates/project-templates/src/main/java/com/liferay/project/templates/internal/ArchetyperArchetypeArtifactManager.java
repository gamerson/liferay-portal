package com.liferay.project.templates.internal;

import com.liferay.project.templates.internal.util.FileUtil;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.archetype.common.DefaultArchetypeArtifactManager;
import org.apache.maven.archetype.exception.UnknownArchetype;
import org.apache.maven.artifact.repository.ArtifactRepository;

public class ArchetyperArchetypeArtifactManager extends DefaultArchetypeArtifactManager {

	public ArchetyperArchetypeArtifactManager(List<File> archetypesDirs) throws Exception {
		_archetypesDirs = archetypesDirs;

		if (_archetypesDirs.isEmpty()) {
			try {
				_archetypesDirs.add(FileUtil.getJarFile(ProjectGenerator.class));
			}
			catch (Exception e) {
			}
		}
	}

	@Override
	public boolean exists(
		String archetypeGroupId, String archetypeArtifactId,
		String archetypeVersion, ArtifactRepository archetypeRepository,
		ArtifactRepository localRepository,
		List<ArtifactRepository> remoteRepositories) {

		return true;
	}

	@Override
	public File getArchetypeFile(
			String groupId, String artifactId, String version,
			ArtifactRepository archetypeRepository,
			ArtifactRepository localRepository,
			List<ArtifactRepository> repositories)
		throws UnknownArchetype {

		File archetypeFile = null;

		for (File archetypesFile : _archetypesDirs) {
			try {
				if (archetypesFile.isDirectory()) {
					Path archetypePath = FileUtil.getFile(
						archetypesFile.toPath(), artifactId + "-*.jar");

					if (archetypePath != null) {
						archetypeFile = archetypePath.toFile();
					}
				}
				else {
					archetypeFile = ProjectGenerator._getArchetypeFile(
						artifactId, archetypesFile);

					if (archetypeFile != null) {
						break;
					}
				}
			}
			catch (Exception e) {
				continue;
			}
		}

		if (archetypeFile == null) {
			throw new UnknownArchetype();
		}

		return archetypeFile;
	}

	@Override
	public ClassLoader getArchetypeJarLoader(File archetypeFile)
		throws UnknownArchetype {

		try {
			URI uri = archetypeFile.toURI();

			return new URLClassLoader(new URL[] {uri.toURL()}, null);
		}
		catch (MalformedURLException murle) {
			throw new UnknownArchetype(murle);
		}
	}

	private final List<File> _archetypesDirs;

}