/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

import groovy.transform.Field
import java.nio.file.Paths
import java.nio.file.Files

def isMaven = false;
def isGradle = false;

for (def o : request.getProperties()) {
	def key = o.getKey()
	if (key == 'buildType') {
		def value = o.getValue()

		if (value == 'maven') {
			isMaven = true
		}
		else if (value == 'gradle') {
			isGradle = true
		}
		break
	}
}
def moduleDir = Paths.get(request.getOutputDirectory(), request.getArtifactId())

if (isMaven) {
	def gradleFile = moduleDir.resolve('build.gradle')

	if (Files.exists(gradleFile)) {
		Files.delete(gradleFile)
	}
}
else if (isGradle) {
	def mavenFile = moduleDir.resolve('pom.xml')

	if (Files.exists(mavenFile)) {
		Files.delete(mavenFile)
	}
}