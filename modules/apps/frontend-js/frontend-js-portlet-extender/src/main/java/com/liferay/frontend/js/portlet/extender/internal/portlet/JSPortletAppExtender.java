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

package com.liferay.frontend.js.portlet.extender.internal.portlet;

import com.liferay.frontend.js.loader.modules.extender.npm.JSBundle;
import com.liferay.frontend.js.loader.modules.extender.npm.JSBundleProcessor;
import com.liferay.frontend.js.loader.modules.extender.npm.JSBundleSource;
import com.liferay.frontend.js.loader.modules.extender.npm.NPMRegistry;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.service.PortletLocalService;
import com.liferay.portal.kernel.servlet.PortletServlet;
import com.liferay.portal.kernel.util.AggregateResourceBundleLoader;
import com.liferay.portal.kernel.util.HashMapDictionary;
import com.liferay.portal.kernel.util.PortletKeys;
import com.liferay.portal.kernel.util.ResourceBundleLoader;
import com.liferay.portal.kernel.util.ResourceBundleLoaderUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * @author Gregory Amerson
 */
@Component(
	configurationPid = "js.portlet.app",
	configurationPolicy = ConfigurationPolicy.REQUIRE, service = {}
)
public class JSPortletAppExtender {

	@Activate
	public void activate(Map<String, Object> properties) {
		JSPortletAppConfig jsPortletAppConfig =
			ConfigurableUtil.createConfigurable(
				JSPortletAppConfig.class, properties);

		String name = jsPortletAppConfig.name();
		String path = jsPortletAppConfig.path();

		if (Validator.isNull(name) || Validator.isNull(path)) {
			throw new IllegalArgumentException(
				"Both name and path configuration properties must be provided");
		}

		Bundle bundle = FrameworkUtil.getBundle(JSPortletAppExtender.class);

		JSBundleSource jsBundleSource = new PathJSBundleSource(
			bundle.getBundleId(), name, path);

		_jsBundle = _jsBundleProcessor.process(jsBundleSource);

		if (_jsBundle == null) {
			return;
		}

		_npmRegistry.addJSBundle(_jsBundle);

		BundleContext bundleContext = bundle.getBundleContext();

		_servletContextRegistration = _registerServletContext(
			bundleContext, name, path);

		ResourceServlet resourceServlet = new ResourceServlet(path);

		_servletRegistration = _registerServlet(
			bundleContext, resourceServlet, name);

		_portletServletRegistration = _registerPortletServlet(
			name, bundleContext);

		_resourceBundleLoaderRegistration = _registerResourceBundleLoader(
			bundleContext, name, path);

		_portletRegistration = _registerPortlet(
			bundleContext, jsBundleSource, resourceServlet);
	}

	@Deactivate
	public void deactivate() {
		_npmRegistry.removeJSBundle(_jsBundle);
		_portletRegistration.unregister();
		_portletServletRegistration.unregister();
		_resourceBundleLoaderRegistration.unregister();
		_servletContextRegistration.unregister();
		_servletRegistration.unregister();
	}

	private void _addServiceProperties(
		Dictionary<String, Object> properties, JSONObject portletJSONObject) {

		if (portletJSONObject == null) {
			return;
		}

		Iterator<String> keys = portletJSONObject.keys();

		while (keys.hasNext()) {
			String key = keys.next();

			Object value = portletJSONObject.get(key);

			if (value instanceof JSONObject) {
				String stringValue = value.toString();

				properties.put(key, stringValue);
			}
			else if (value instanceof JSONArray) {
				JSONArray jsonArray = (JSONArray)value;

				List<String> values = new ArrayList<>();

				for (int i = 0; i < jsonArray.length(); i++) {
					Object object = jsonArray.get(i);

					values.add(object.toString());
				}

				properties.put(key, values.toArray(new String[0]));
			}
			else {
				properties.put(key, value);
			}
		}
	}

	private String _getPortletName(JSONObject packageJSONObject) {
		String portletName = packageJSONObject.getString("name");

		JSONObject portletJSONObject = packageJSONObject.getJSONObject(
			"portlet");

		String javaxPortletName = portletJSONObject.getString(
			"javax.portlet.name");

		if (Validator.isNotNull(javaxPortletName)) {
			portletName = javaxPortletName;
		}

		return portletName;
	}

	private JSONObject _parse(URL url) {
		if (url == null) {
			return null;
		}

		try (InputStream inputStream = url.openStream()) {
			return _jsonFactory.createJSONObject(StringUtil.read(inputStream));
		}
		catch (Exception e) {
			_log.error("Unable to parse " + url, e);

			return null;
		}
	}

	private ServiceRegistration<?> _registerJSPortletService(
		BundleContext bundleContext, JSONObject packageJSONObject,
		Set<String> portletPreferencesFieldNames, Servlet servlet) {

		Dictionary<String, Object> properties = new Hashtable<>();

		_addServiceProperties(
			properties, packageJSONObject.getJSONObject("portlet"));

		String packageName = packageJSONObject.getString("name");

		properties.put(
			"javax.portlet.name", _getPortletName(packageJSONObject));
		properties.put("service.pid", packageName);

		String packageVersion = packageJSONObject.getString("version");

		com.liferay.portal.kernel.model.Portlet portalPortletModel =
			_portletLocalService.getPortletById(
				CompanyConstants.SYSTEM, PortletKeys.PORTAL);

		return bundleContext.registerService(
			new String[] {
				ManagedService.class.getName(),
				javax.portlet.Portlet.class.getName()
			},
			new JSAppPortlet(
				_jsonFactory, packageName, packageVersion,
				portletPreferencesFieldNames,
				portalPortletModel.getPortletApp(), servlet),
			properties);
	}

	private ServiceRegistration<?> _registerPortlet(
		BundleContext bundleContext, JSBundleSource jsPortletApp,
		Servlet servlet) {

		JSONObject packageJSONObject = _parse(
			jsPortletApp.getResource("package.json"));

		if (packageJSONObject == null) {
			return null;
		}

		Set<String> portletPreferencesFieldNames = new HashSet<>();

		JSONObject portletPreferencesJSONObject = _parse(
			jsPortletApp.getEntry("features/portlet_preferences.json"));

		if (portletPreferencesJSONObject != null) {
			JSONArray fieldsJSONArray =
				portletPreferencesJSONObject.getJSONArray("fields");

			for (int i = 0; i < fieldsJSONArray.length(); i++) {
				JSONObject jsonObject = fieldsJSONArray.getJSONObject(i);

				portletPreferencesFieldNames.add(jsonObject.getString("name"));
			}
		}

		return _registerJSPortletService(
			bundleContext, packageJSONObject, portletPreferencesFieldNames,
			servlet);
	}

	@SuppressWarnings("serial")
	private ServiceRegistration<Servlet> _registerPortletServlet(
		String name, BundleContext bundleContext) {

		return bundleContext.registerService(
			Servlet.class, new PortletServlet(),
			new Hashtable<String, String>() {
				{
					put(
						HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
						name);
					put(
						HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME,
						PortletServlet.class.getName());
					put(
						HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN,
						"/portlet-servlet/*");
				}
			});
	}

	private ServiceRegistration<ResourceBundleLoader>
		_registerResourceBundleLoader(
			BundleContext bundleContext, String name, String path) {

		try {
			Path basePath = Paths.get(path);

			URI uri = basePath.toUri();

			URLClassLoader urlClassLoader = new URLClassLoader(
				new URL[] {uri.toURL()});

			String baseName = "content.Language";

			ResourceBundleLoader resourceBundleLoader =
				ResourceBundleUtil.getResourceBundleLoader(
					baseName, urlClassLoader);

			AggregateResourceBundleLoader aggregateResourceBundleLoader =
				new AggregateResourceBundleLoader(
					resourceBundleLoader,
					ResourceBundleLoaderUtil.getPortalResourceBundleLoader());

			Dictionary<String, Object> attributes = new HashMapDictionary<>();

			attributes.put("resource.bundle.base.name", baseName);
			attributes.put("servlet.context.name", name);

			return bundleContext.registerService(
				ResourceBundleLoader.class, aggregateResourceBundleLoader,
				attributes);
		}
		catch (MalformedURLException murle) {
			murle.printStackTrace();
		}

		return null;
	}

	@SuppressWarnings("serial")
	private ServiceRegistration<Servlet> _registerServlet(
		BundleContext bundleContext, Servlet servlet, String name) {

		return bundleContext.registerService(
			Servlet.class, servlet,
			new Hashtable<String, String>() {
				{
					put(
						HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
						name);
					put(
						HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN,
						"/*");
				}
			});
	}

	@SuppressWarnings("serial")
	private ServiceRegistration<ServletContextHelper> _registerServletContext(
		BundleContext bundleContext, String name, String path) {

		ServletContextHelper servletContextHelper = new ServletContextHelper(
			bundleContext.getBundle()) {

			@Override
			public URL getResource(String name) {
				Path basePath = Paths.get(path);

				Path resource = basePath.resolve(name);

				if (Files.exists(resource)) {
					try {
						URI uri = resource.toUri();

						return uri.toURL();
					}
					catch (MalformedURLException murle) {
					}
				}

				return super.getResource(name);
			}

		};

		return bundleContext.registerService(
			ServletContextHelper.class, servletContextHelper,
			new Hashtable<String, String>() {
				{
					put(
						HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME,
						name);
					put(
						HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH,
						"/" + name);
				}
			});
	}

	private static final Log _log = LogFactoryUtil.getLog(
		JSPortletAppExtender.class);

	private JSBundle _jsBundle;

	@Reference
	private JSBundleProcessor _jsBundleProcessor;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private NPMRegistry _npmRegistry;

	@Reference
	private PortletLocalService _portletLocalService;

	private ServiceRegistration<?> _portletRegistration;
	private ServiceRegistration<Servlet> _portletServletRegistration;
	private ServiceRegistration<ResourceBundleLoader>
		_resourceBundleLoaderRegistration;
	private ServiceRegistration<ServletContextHelper>
		_servletContextRegistration;
	private ServiceRegistration<Servlet> _servletRegistration;

	private static class PathJSBundleSource implements JSBundleSource {

		public PathJSBundleSource(long id, String name, String path) {
			_id = id;
			_appName = name;
			_appPath = Paths.get(path);
		}

		@Override
		public Enumeration<URL> findResources(
			String filePattern, boolean recurse) {

			FileSystem fileSystem = FileSystems.getDefault();

			final PathMatcher pathMatcher = fileSystem.getPathMatcher(
				StringBundler.concat(
					"glob:{", filePattern, ",**/", filePattern, "}"));

			List<URL> entries = new ArrayList<>();

			try {
				Files.walkFileTree(
					_appPath,
					new SimpleFileVisitor<Path>() {

						@Override
						public FileVisitResult visitFile(
								Path filePath,
								BasicFileAttributes basicFileAttributes)
							throws IOException {

							URI uri = null;

							if (pathMatcher.matches(
									_appPath.relativize(filePath))) {

								uri = filePath.toUri();
							}

							if (uri != null) {
								entries.add(uri.toURL());
							}

							return FileVisitResult.CONTINUE;
						}

					});
			}
			catch (IOException ioe) {
			}

			return Collections.enumeration(entries);
		}

		@Override
		public URL getEntry(String path) {
			Path entry = _appPath.resolve(path);

			if (Files.exists(entry)) {
				URI uri = entry.toUri();

				try {
					return uri.toURL();
				}
				catch (MalformedURLException murle) {
				}
			}

			return null;
		}

		@Override
		public long getId() {
			return _id;
		}

		@Override
		public String getName() {
			return _appName;
		}

		@Override
		public URL getResource(String path) {
			path = path.replaceFirst("META-INF/resources/", "");

			return getEntry(path);
		}

		@Override
		public String getResourcesPath() {
			return _appPath.toString();
		}

		@Override
		public Version getVersion() {
			return new Version(1, 0, 0);
		}

		private final String _appName;
		private final Path _appPath;
		private final long _id;

	}

}