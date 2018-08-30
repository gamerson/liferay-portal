package com.liferay.project.templates;

import com.beust.jcommander.Parameter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CodeTemplatesArgs {

	public List<File> getArchetypesDirs() {
		return _archetypesDirs;
	}

	public String getPackageName() {
		return _packageName;
	}

	public void setClassName(String className) {
		_className = className;
	}

	public String getClassName() {
		return _className;
	}

	@Parameter(
		description = "Provide the name of the class to be generated.",
		names = "--class-name",
		required = true
	)
	private String _className;

	@Parameter(
		description = "Provide the name of the model class to be generated.",
		names = "--model-class-name"
	)
	private String _modelClassName;


	public String getModelClassName() {
		return _modelClassName;
	}

	public void setModelClassName(String modelClassName) {
		_modelClassName = modelClassName;
	}

	public File getDestinationDir() {
		return _destinationDir;
	}

	public void setDestinationDir(File destinationDir) {
		_destinationDir = destinationDir;
	}

	public void setArchetypesDirs(List<File> archetypesDirs) {
		_archetypesDirs = archetypesDirs;
	}

	public void setPackageName(String packageName) {
		_packageName = packageName;
	}

	public void setTemplate(String template) {
		_template = template;
	}

	public void setAuthor(String author) {
		_author = author;
	}

	@Parameter(
		description = "The name of the user associated with the code.",
		names = "--author"
	)
	private String _author;

	@Parameter(
		description = "The destination package for the code generator.",
		names = "--package-name"
	)
	private String _packageName;

	@Parameter(
		description = "The code template to generate into destination dir.",
		names = "--template"
	)
	private String _template;

	@Parameter(hidden = true, names = {"--archetypes-dir", "--archetypes-dirs"})
	private List<File> _archetypesDirs = new ArrayList<>();

	public String getTemplate() {
		return _template;
	}

	@Parameter(
		description = "The directory where to generate the code.",
		names = "--destination"
	)
	private File _destinationDir;

	public String getAuthor() {
		return _author;
	}
}