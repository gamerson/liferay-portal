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

package com.liferay.portal.vulcan.util;

import com.liferay.petra.function.UnsafeFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Brian Wing Shun Chan
 */
public class TransformUtil {

	public static <T, R> List<R> transform(
		List<T> list, UnsafeFunction<T, R, Throwable> unsafeFunction) {

		List<R> newList = new ArrayList<>();

		for (T item : list) {
			try {
				R newItem = unsafeFunction.apply(item);

				newList.add(newItem);
			}
			catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}

		return newList;
	}

}