import React from 'react';
import propTypes from 'prop-types';

class IntegerInput extends React.Component {
	static propTypes = {
		onChange: propTypes.func.isRequired,
		value: propTypes.oneOfType(
			[
				propTypes.string,
				propTypes.number
			]
		)
	};

	_handleIntegerChange = event => {
		const value = parseInt(event.target.value, 10);

		if (!isNaN(value)) {
			this.props.onChange(value.toString());
		}
	}

	render() {
		return (
			<input
				className="criterion-input form-control"
				data-testid="integer-number"
				onChange={this._handleIntegerChange}
				type="number"
				value={this.props.value}
			/>
		);
	}
}

export default IntegerInput;