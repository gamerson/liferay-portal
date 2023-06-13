# How to Test Sample Marketplace Apps

## Joke App

This app is compromised of two client-extension projects, `joke-remote-app` and `joke-service`. 
The `joke-remote-app` is a ReactJS style project that uses the `customElement` type client-extension.
It demostrates how a remote app can call a remote-service that is also deployed as a client-extension
`joke-service`.  The communication from `joke-remote-app` is using OAuth2 Application User Agent
client extension that is registered by the `joke-service`.

### Deploy Joke App to local

1. Go to `sample-markplace-apps-workspace`
1. Run `./gradlew clean startDockerContainer logsDockerContainer
1. Once portal starts up, go to the `joke-service` directory
1. Run `./gradlew bootRun`

### Deploy Joke to LXC

1. Go to `sample-markplace-apps-workspace`
1. Run `./gradlew clean build`
1. Run `lcp login` to your LXC extension environment
1. Run `lcp deploy --extension client-extensions/apps/joke/joke-remote-app/dist/joke-remote-app.zip`
1. Run `lcp deploy --extension client-extensions/apps/joke/joke-service/dist/joke-service.zip`

### Test Joke App in DXP

1. Login to DXP after Joke App has been deployed
1. On any page, add the `Joke Remote App widget`
1. If user is logged in, it should show `Hello <User>` and then display a joke that was fetched from `joke-service`