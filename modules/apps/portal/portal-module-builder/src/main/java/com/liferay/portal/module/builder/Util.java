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

package com.liferay.portal.module.builder;

import java.io.IOException;
import java.io.InputStream;

import java.text.MessageFormat;

/**
 * @author Gregory Amerson
 */
public class Util {

	public static String loadTemplate(String name) {
		try (InputStream inputStream = Util.class.getResourceAsStream(name)) {
			return read(inputStream);
		}
		catch (IOException ioException) {
			return MessageFormat.format(
				"Error loading {0}: {1}", name, ioException.getMessage());
		}
	}

	public static String read(InputStream inputStream) throws IOException {
		byte[] buffer = new byte[8192];
		int offset = 0;

		while (true) {
			int count = inputStream.read(
				buffer, offset, buffer.length - offset);

			if (count == -1) {
				break;
			}

			offset += count;

			if (offset == buffer.length) {
				byte[] newBuffer = new byte[buffer.length << 1];

				System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);

				buffer = newBuffer;
			}
		}

		if (offset == 0) {
			return "";
		}

		return new String(buffer, 0, offset, "UTF-8");
	}

}