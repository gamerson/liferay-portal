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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Gregory Amerson
 */
@SuppressWarnings("serial")
public class ResourceServlet extends HttpServlet {

	public ResourceServlet(String path) {
		_path = path;
	}

	@Override
	public void service(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws IOException, ServletException {

		String requestedFile = httpServletRequest.getPathInfo();

		if (requestedFile == null) {
			httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);

			return;
		}

		File file = new File(_path, URLDecoder.decode(requestedFile, "UTF-8"));

		if (!file.exists()) {
			httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);

			return;
		}

		String contentType = getServletContext().getMimeType(file.getName());

		if (contentType == null) {
			contentType = "application/octet-stream";
		}

		httpServletResponse.reset();
		httpServletResponse.setBufferSize(_DEFAULT_BUFFER_SIZE);
		httpServletResponse.setContentType(contentType);
		httpServletResponse.setHeader(
			"Content-Length", String.valueOf(file.length()));
		httpServletResponse.setHeader(
			"Content-Disposition",
			"attachment; filename=\"" + file.getName() + "\"");

		try (BufferedInputStream input = new BufferedInputStream(
				new FileInputStream(file), _DEFAULT_BUFFER_SIZE);
			BufferedOutputStream output = new BufferedOutputStream(
				httpServletResponse.getOutputStream(), _DEFAULT_BUFFER_SIZE)) {

			byte[] buffer = new byte[_DEFAULT_BUFFER_SIZE];

			int length;

			while ((length = input.read(buffer)) > 0) {
				output.write(buffer, 0, length);
			}
		}
	}

	private static final int _DEFAULT_BUFFER_SIZE = 10240;

	private final String _path;

}