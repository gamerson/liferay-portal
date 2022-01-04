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

import ClayDropDown from '@clayui/drop-down';
import ClayIcon from '@clayui/icon';
import classnames from 'classnames';
import PropTypes from 'prop-types';
import React, {useCallback} from 'react';

const ImportMappingDropdownItem = ({item, onClick, selectedItem}) => {
	const selected = selectedItem === item.value;

	const onItemClick = useCallback(() => {
		if (!selected) {
			onClick(item.value);
		}
	}, [item, onClick, selected]);

	return (
		<ClayDropDown.Item
			className="align-items-center d-flex justify-content-between"
			onClick={onItemClick}
		>
			<span className={classnames({'text-muted': selected})}>
				{item.label}
			</span>

			{selected && <ClayIcon className="text-success" symbol="check" />}
		</ClayDropDown.Item>
	);
};

export const ImportFieldPropType = {
	label: PropTypes.string.isRequired,
	required: PropTypes.bool,
	value: PropTypes.string.isRequired,
};

ImportMappingDropdownItem.propTypes = {
	item: PropTypes.shape(ImportFieldPropType).isRequired,
	onClick: PropTypes.func.isRequired,
	selectedItem: PropTypes.string,
};

export default ImportMappingDropdownItem;
