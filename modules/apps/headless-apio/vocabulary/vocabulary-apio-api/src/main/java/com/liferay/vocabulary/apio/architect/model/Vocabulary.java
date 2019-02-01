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

package com.liferay.vocabulary.apio.architect.model;

import java.util.Locale;
import java.util.Map;

/**
 * Holds information about a {@code
 * com.liferay.asset.kernel.model.AssetVocabulary} exposed through the API.
 *
 * @author Rubén Pulido
 * @review
 */
public interface Vocabulary {

	/**
	 * Returns the asset vocabulary's description map.
	 *
	 * @return the description map
	 */
	public Map<Locale, String> getDescriptionMap(Locale locale);

	/**
	 * Returns the asset vocabulary's names map.
	 *
	 * @return the title map
	 */
	public Map<Locale, String> getNameMap(Locale locale);

}