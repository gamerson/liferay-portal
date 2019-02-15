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

package com.liferay.headless.form.dto.v1_0;

import com.liferay.petra.function.UnsafeSupplier;

import javax.annotation.Generated;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Javier Gamarra
 * @generated
 */
@Generated("")
@XmlRootElement(name = "Fields")
public class Fields {

	public Boolean getAutocomplete() {
		return _autocomplete;
	}

	public String getDataSourceType() {
		return _dataSourceType;
	}

	public String getDataType() {
		return _dataType;
	}

	public String getDisplayStyle() {
		return _displayStyle;
	}

	public Grid getGrid() {
		return _grid;
	}

	public Boolean getHasFormRules() {
		return _hasFormRules;
	}

	public Long getId() {
		return _id;
	}

	public Boolean getImmutable() {
		return _immutable;
	}

	public Boolean getInline() {
		return _inline;
	}

	public String getInputControl() {
		return _inputControl;
	}

	public String getLabel() {
		return _label;
	}

	public Boolean getLocalizable() {
		return _localizable;
	}

	public Boolean getMultiple() {
		return _multiple;
	}

	public String getName() {
		return _name;
	}

	public Options getOptions() {
		return _options;
	}

	public String getPlaceholder() {
		return _placeholder;
	}

	public String getPredefinedValue() {
		return _predefinedValue;
	}

	public Boolean getReadOnly() {
		return _readOnly;
	}

	public Boolean getRepeatable() {
		return _repeatable;
	}

	public Boolean getRequired() {
		return _required;
	}

	public Boolean getShowAsSwitcher() {
		return _showAsSwitcher;
	}

	public Boolean getShowLabel() {
		return _showLabel;
	}

	public String getStyle() {
		return _style;
	}

	public String getText() {
		return _text;
	}

	public String getTooltip() {
		return _tooltip;
	}

	public Validation getValidation() {
		return _validation;
	}

	public void setAutocomplete(Boolean autocomplete) {
		_autocomplete = autocomplete;
	}

	public void setAutocomplete(
		UnsafeSupplier<Boolean, Throwable> autocompleteUnsafeSupplier) {

		try {
			_autocomplete = autocompleteUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setDataSourceType(String dataSourceType) {
		_dataSourceType = dataSourceType;
	}

	public void setDataSourceType(
		UnsafeSupplier<String, Throwable> dataSourceTypeUnsafeSupplier) {

		try {
			_dataSourceType = dataSourceTypeUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setDataType(String dataType) {
		_dataType = dataType;
	}

	public void setDataType(
		UnsafeSupplier<String, Throwable> dataTypeUnsafeSupplier) {

		try {
			_dataType = dataTypeUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setDisplayStyle(String displayStyle) {
		_displayStyle = displayStyle;
	}

	public void setDisplayStyle(
		UnsafeSupplier<String, Throwable> displayStyleUnsafeSupplier) {

		try {
			_displayStyle = displayStyleUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setGrid(Grid grid) {
		_grid = grid;
	}

	public void setGrid(UnsafeSupplier<Grid, Throwable> gridUnsafeSupplier) {
		try {
			_grid = gridUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setHasFormRules(Boolean hasFormRules) {
		_hasFormRules = hasFormRules;
	}

	public void setHasFormRules(
		UnsafeSupplier<Boolean, Throwable> hasFormRulesUnsafeSupplier) {

		try {
			_hasFormRules = hasFormRulesUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setId(Long id) {
		_id = id;
	}

	public void setId(UnsafeSupplier<Long, Throwable> idUnsafeSupplier) {
		try {
			_id = idUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setImmutable(Boolean immutable) {
		_immutable = immutable;
	}

	public void setImmutable(
		UnsafeSupplier<Boolean, Throwable> immutableUnsafeSupplier) {

		try {
			_immutable = immutableUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setInline(Boolean inline) {
		_inline = inline;
	}

	public void setInline(
		UnsafeSupplier<Boolean, Throwable> inlineUnsafeSupplier) {

		try {
			_inline = inlineUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setInputControl(String inputControl) {
		_inputControl = inputControl;
	}

	public void setInputControl(
		UnsafeSupplier<String, Throwable> inputControlUnsafeSupplier) {

		try {
			_inputControl = inputControlUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setLabel(String label) {
		_label = label;
	}

	public void setLabel(
		UnsafeSupplier<String, Throwable> labelUnsafeSupplier) {

		try {
			_label = labelUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setLocalizable(Boolean localizable) {
		_localizable = localizable;
	}

	public void setLocalizable(
		UnsafeSupplier<Boolean, Throwable> localizableUnsafeSupplier) {

		try {
			_localizable = localizableUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setMultiple(Boolean multiple) {
		_multiple = multiple;
	}

	public void setMultiple(
		UnsafeSupplier<Boolean, Throwable> multipleUnsafeSupplier) {

		try {
			_multiple = multipleUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setName(String name) {
		_name = name;
	}

	public void setName(UnsafeSupplier<String, Throwable> nameUnsafeSupplier) {
		try {
			_name = nameUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setOptions(Options options) {
		_options = options;
	}

	public void setOptions(
		UnsafeSupplier<Options, Throwable> optionsUnsafeSupplier) {

		try {
			_options = optionsUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setPlaceholder(String placeholder) {
		_placeholder = placeholder;
	}

	public void setPlaceholder(
		UnsafeSupplier<String, Throwable> placeholderUnsafeSupplier) {

		try {
			_placeholder = placeholderUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setPredefinedValue(String predefinedValue) {
		_predefinedValue = predefinedValue;
	}

	public void setPredefinedValue(
		UnsafeSupplier<String, Throwable> predefinedValueUnsafeSupplier) {

		try {
			_predefinedValue = predefinedValueUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setReadOnly(Boolean readOnly) {
		_readOnly = readOnly;
	}

	public void setReadOnly(
		UnsafeSupplier<Boolean, Throwable> readOnlyUnsafeSupplier) {

		try {
			_readOnly = readOnlyUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setRepeatable(Boolean repeatable) {
		_repeatable = repeatable;
	}

	public void setRepeatable(
		UnsafeSupplier<Boolean, Throwable> repeatableUnsafeSupplier) {

		try {
			_repeatable = repeatableUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setRequired(Boolean required) {
		_required = required;
	}

	public void setRequired(
		UnsafeSupplier<Boolean, Throwable> requiredUnsafeSupplier) {

		try {
			_required = requiredUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setShowAsSwitcher(Boolean showAsSwitcher) {
		_showAsSwitcher = showAsSwitcher;
	}

	public void setShowAsSwitcher(
		UnsafeSupplier<Boolean, Throwable> showAsSwitcherUnsafeSupplier) {

		try {
			_showAsSwitcher = showAsSwitcherUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setShowLabel(Boolean showLabel) {
		_showLabel = showLabel;
	}

	public void setShowLabel(
		UnsafeSupplier<Boolean, Throwable> showLabelUnsafeSupplier) {

		try {
			_showLabel = showLabelUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setStyle(String style) {
		_style = style;
	}

	public void setStyle(
		UnsafeSupplier<String, Throwable> styleUnsafeSupplier) {

		try {
			_style = styleUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setText(String text) {
		_text = text;
	}

	public void setText(UnsafeSupplier<String, Throwable> textUnsafeSupplier) {
		try {
			_text = textUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setTooltip(String tooltip) {
		_tooltip = tooltip;
	}

	public void setTooltip(
		UnsafeSupplier<String, Throwable> tooltipUnsafeSupplier) {

		try {
			_tooltip = tooltipUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setValidation(
		UnsafeSupplier<Validation, Throwable> validationUnsafeSupplier) {

		try {
			_validation = validationUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setValidation(Validation validation) {
		_validation = validation;
	}

	private Boolean _autocomplete;
	private String _dataSourceType;
	private String _dataType;
	private String _displayStyle;
	private Grid _grid;
	private Boolean _hasFormRules;
	private Long _id;
	private Boolean _immutable;
	private Boolean _inline;
	private String _inputControl;
	private String _label;
	private Boolean _localizable;
	private Boolean _multiple;
	private String _name;
	private Options _options;
	private String _placeholder;
	private String _predefinedValue;
	private Boolean _readOnly;
	private Boolean _repeatable;
	private Boolean _required;
	private Boolean _showAsSwitcher;
	private Boolean _showLabel;
	private String _style;
	private String _text;
	private String _tooltip;
	private Validation _validation;

}