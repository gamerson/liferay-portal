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

package com.liferay.frontend.js.loader.modules.extender.internal.npm.flat;

import com.liferay.frontend.js.loader.modules.extender.npm.JSBundleSource;
import com.liferay.frontend.js.loader.modules.extender.npm.JSPackage;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

import org.osgi.framework.Version;

/**
 * @author Gregory Amerson
 */
public class FlatJSBundleSource extends FlatJSBundle {

	public FlatJSBundleSource(JSBundleSource jsBundleSource) {
		super(null);

		_jsBundleSource = jsBundleSource;
	}

	public void addJSPackage(JSPackage jsPackage) {
		_jsPackages.add(jsPackage);
	}

	public Enumeration<URL> findEntries(
		String path, String filePattern, boolean recurse) {

		return _jsBundleSource.findResources(filePattern, recurse);
	}

	@Override
	public String getId() {
		return String.valueOf(_jsBundleSource.getId());
	}

	@Override
	public Collection<JSPackage> getJSPackages() {
		return _jsPackages;
	}

	@Override
	public String getName() {
		return _jsBundleSource.getName();
	}

	@Override
	public URL getResourceURL(String location) {
		return _jsBundleSource.getResource(location);
	}

	@Override
	public String getVersion() {
		Version version = _jsBundleSource.getVersion();

		return version.toString();
	}

	@Override
	public String toString() {
		StringBundler sb = new StringBundler(5);

		sb.append(getId());
		sb.append(StringPool.COLON);
		sb.append(getName());
		sb.append(StringPool.AT);
		sb.append(getVersion());

		return sb.toString();
	}

	private final JSBundleSource _jsBundleSource;
	private final Collection<JSPackage> _jsPackages = new ArrayList<>();

}