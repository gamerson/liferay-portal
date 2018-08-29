package com.liferay.project.templates;

import com.liferay.project.templates.internal.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CodeTemplatesTest {

	@Test
	public void testGenerateCodeTemplate() throws Exception {
		File destinationDir = temporaryFolder.newFolder("code");

		List<String> completeArgs = new ArrayList<>();

		completeArgs.add("--archetypes-dir");

		File archetypesDir = FileUtil.getJarFile(CodeTemplatesTest.class);

		completeArgs.add(archetypesDir.getPath());

		completeArgs.add("--destination");
		completeArgs.add(destinationDir.getPath());

		completeArgs.add("--template");
		completeArgs.add("rest");

		CodeTemplates.main(completeArgs.toArray(new String[0]));
	}

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();
}