#!/bin/sh
gw clean install
VERSION=2.0.0
LIFERAY_M2_DIR="../../../../.m2/com/liferay/com.liferay.bean.portlet.spring.extension/$VERSION"
mvn install:install-file \
	-Dfile=$LIFERAY_M2_DIR/com.liferay.bean.portlet.spring.extension-$VERSION.jar \
	-DgroupId=com.liferay \
	-DartifactId=com.liferay.bean.portlet.spring.extension \
	-DpomFile=$LIFERAY_M2_DIR/com.liferay.bean.portlet.spring.extension-$VERSION.pom \
	-Dversion=$VERSION
