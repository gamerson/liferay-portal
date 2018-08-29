package com.liferay.project.templates;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.liferay.project.templates.internal.CodeGenerator;
import com.liferay.project.templates.internal.util.FileUtil;

import java.io.File;

public class CodeTemplates {

	public CodeTemplates(CodeTemplatesArgs codeTemplatesArgs) throws Exception {
		File destinationDir = codeTemplatesArgs.getDestinationDir();

		CodeGenerator codeGenerator = new CodeGenerator();

		codeGenerator.generateCode(codeTemplatesArgs, destinationDir);
	}

	public static void main(String[] args) throws Exception {
		CodeTemplatesArgs codeTemplatesArgs = new CodeTemplatesArgs();

		JCommander jCommander = new JCommander(codeTemplatesArgs);

		try {
			File jarFile = FileUtil.getJarFile(CodeTemplates.class);

			if (jarFile.isFile()) {
				jCommander.setProgramName("java -jar " + jarFile.getName());
			}
			else {
				jCommander.setProgramName(CodeTemplates.class.getName());
			}

			jCommander.parse(args);

			new CodeTemplates(codeTemplatesArgs);
		}
		catch (ParameterException pe) {
			System.err.println(pe.getMessage());
		}
	}
}
