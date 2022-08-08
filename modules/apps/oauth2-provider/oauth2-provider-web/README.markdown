## OAuth2 JavaScript Client

This module defines an OAuth2 Client which when used in combination with
Liferay's OAuth2 User Agent Applications simplifies the JavaScript code
required to make calls to the Application's endpoint(s).

### Steps

1. Follow the documentation to [create a OAuth2 User Agent based Application](https://learn.liferay.com/dxp/latest/en/headless-delivery/using-oauth2/creating-oauth2-applications.html?highlight=oauth2%20application).

1. Use the `Liferay.OAuth2Client` to make calls.

  For example, given a **OAuth2 User Agent Application** with the name `foo`:
  ```javascript
  Liferay.OAuth2Client.FromUserAgentApplication('foo').fetch(
    '/test'
  ).then(
    r => r.text()
  ).then(
    r => console.log('success:', r)
  ).catch(
    e => console.log('error:', e)
  );
  ```

#### Details

1. The URI passed to `fetch` (as either `string` or `Request`) is only allowed to contain the base URL of the User Agent Application or no base at all. With this in mind it's best to pass URI without a base as demonstrated in the example in order to retain portability.

1. The client will _not_ trigger authentication. If the current user session is not authenticated an error is returned, such as `error: login_required`.

1. The client's token will be cached for re-use by multiple requests. Multiple clients may be instantiated against the same Application name and still benefit from cached tokens.

1. Tokens are only valid as long as the server session and are revoked on logout.