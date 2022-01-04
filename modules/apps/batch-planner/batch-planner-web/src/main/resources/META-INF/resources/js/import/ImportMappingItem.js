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

import ClayButton from '@clayui/button';
import ClayDropDown from '@clayui/drop-down';
import ClayForm from '@clayui/form';
import ClayIcon from '@clayui/icon';
import PropTypes from 'prop-types';
import React, {useCallback, useMemo, useState} from 'react';

import ImportMappingDropdownItem, {
	ImportFieldPropType,
} from './ImportMappingDropdownItem';

const ImportMappingItem = ({
	field,
	onChange,
	portletNamespace,
	selectableFields,
	selectedField,
}) => {
	const [searchLabel, setSearchLabel] = useState();
	const [dropDownActive, setDropDownActive] = useState(false);

	const [requiredFields, optionalFields] = useMemo(
		() =>
			buildDropdownItemsFromFields(
				selectableFields,
				searchLabel,
				selectedField
			),
		[selectableFields, searchLabel, selectedField]
	);

	const inputId = `input-field-${field}`;

	const onSearchChange = useCallback((event) => {
		setSearchLabel(event.target.value);
	}, []);

	const onDropdownItemClick = useCallback(
		(newValue) => {
			if (onChange) {
				onChange(newValue, field);
			}
			setDropDownActive(false);
		},
		[field, onChange]
	);

	const selectFirstElement = useCallback(
		(event) => {
			event.preventDefault();
			const firstElement = requiredFields?.at(0) || optionalFields?.at(0);

			if (firstElement) {
				if (onChange) {
					onChange(firstElement, field);
				}
				setDropDownActive(false);
				setSearchLabel();
			}
		},
		[field, onChange, optionalFields, requiredFields]
	);

	return (
		<ClayForm.Group>
			<label htmlFor={inputId}>{field}</label>

			{selectedField && (
				<>
					<input
						hidden
						name={`${portletNamespace}externalFieldName_${selectedField}`}
						readOnly
						value={field}
					/>
					<input
						hidden
						name={`${portletNamespace}internalFieldName_${selectedField}`}
						readOnly
						value={selectedField}
					/>
				</>
			)}

			<ClayDropDown
				active={dropDownActive}
				onActiveChange={setDropDownActive}
				tabIndex={-1}
				trigger={
					<ClayButton
						aria-expanded={dropDownActive ? 'true' : 'false'}
						aria-haspopup="listbox"
						className="w-100"
						displayType="secondary"
						id={inputId}
					>
						<span className="align-items-center d-flex justify-content-between">
							<span>{selectedField ?? '\u00A0'}</span>

							<ClayIcon symbol="caret-double" />
						</span>
					</ClayButton>
				}
			>
				<ClayDropDown.Search
					formProps={{onSubmit: selectFirstElement}}
					onChange={onSearchChange}
					placeholder={Liferay.Language.get('search')}
					value={searchLabel}
				/>

				<ClayDropDown.ItemList>
					{requiredFields.length > 0 && (
						<ClayDropDown.Group
							header={Liferay.Language.get('required')}
						>
							<ClayDropDown.Divider />

							{requiredFields.map((item) => (
								<ImportMappingDropdownItem
									item={item}
									key={item.value}
									onClick={onDropdownItemClick}
									selectedItem={selectedField}
								/>
							))}
						</ClayDropDown.Group>
					)}

					{optionalFields.length > 0 && (
						<ClayDropDown.Group
							header={Liferay.Language.get('optional')}
						>
							<ClayDropDown.Divider />

							{optionalFields.map((item) => (
								<ImportMappingDropdownItem
									item={item}
									key={item.value}
									onClick={onDropdownItemClick}
									selectedItem={selectedField}
								/>
							))}
						</ClayDropDown.Group>
					)}
				</ClayDropDown.ItemList>
			</ClayDropDown>
		</ClayForm.Group>
	);
};

const buildDropdownItemsFromFields = (
	selectableFields = [],
	searchLabel,
	selectedField
) => {
	const allFields = [...selectableFields];

	if (selectedField) {
		allFields.push({label: selectedField, value: selectedField});
		allFields.sort((a, b) => (a.label > b.label ? 1 : -1));
	}

	const searchedFields = allFields.filter((fields) =>
		searchLabel
			? fields.label.toLowerCase().includes(searchLabel.toLowerCase())
			: true
	);

	const {optionalFields, requiredFields} = searchedFields.reduce(
		(accumulator, currentField) => {
			if (currentField.required) {
				accumulator.requiredFields.push(currentField);
			}
			else {
				accumulator.optionalFields.push(currentField);
			}

			return accumulator;
		},
		{optionalFields: [], requiredFields: []}
	);

	return [requiredFields, optionalFields];
};

ImportMappingItem.propTypes = {
	field: PropTypes.string.isRequired,
	onChange: PropTypes.func,
	portletNamespace: PropTypes.string.isRequired,
	selectableFields: PropTypes.arrayOf(PropTypes.shape(ImportFieldPropType)),
	selectedField: PropTypes.string,
};

export default ImportMappingItem;
