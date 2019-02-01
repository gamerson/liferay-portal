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

package com.liferay.change.tracking.internal.process.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.Serializable;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Daniel Kocsis
 */
public class CTProcessLog implements Serializable {

	public static CTProcessLog from(String processLogJSON) {
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			return objectMapper.readValue(processLogJSON, CTProcessLog.class);
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	public List<CTProcessLogEntry> getLogEntries() {
		return _ctProcessLogEntry;
	}

	public void insertLogEntry(CTProcessLogEntry ctProcessLogEntry) {
		_ctProcessLogEntry.add(ctProcessLogEntry);
	}

	@Override
	public String toString() {
		ObjectMapper objectMapper = new ObjectMapper();

		ObjectWriter objectWriter =
			objectMapper.writerWithDefaultPrettyPrinter();

		try {
			return objectWriter.writeValueAsString(this);
		}
		catch (JsonProcessingException jpe) {
			return "{}";
		}
	}

	private final List<CTProcessLogEntry> _ctProcessLogEntry =
		new LinkedList<>();

}