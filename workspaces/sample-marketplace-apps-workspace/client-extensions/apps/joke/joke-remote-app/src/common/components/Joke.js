import React from 'react';

class Joke extends React.Component {
	constructor(props) {
		super(props);

		this.oAuth2Client = props.oAuth2Client;
		this.state = {"joke": ""};
	}

	componentDidMount() {
		this._request = this.oAuth2Client.fetch(
			'/joke'
		).then(response => response.text()
		).then(text => {
			this._request = null;
			this.setState({"joke": text});
		});
	}

	componentWillUnmount() {
		if (this._request) {
			this._request.cancel();
		}
	}

	render() {
		if (this.state === null || this.state == "") {
			return <div>Loading...</div>
		}
		else {
			return <div>{this.state.joke}</div>
		}
	}
}

export default Joke;
