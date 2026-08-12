# Bunbun Android

Minimal text messenger client built with Kotlin, Jetpack Compose and Material 3.

## Configure the API

Add the HTTPS base URL of the uploaded `bunbun-api` directory to the project-level
`local.properties` file (which is ignored by Git):

```properties
bunbunApiBaseUrl=https://example.org/bunbun-api/
```

The trailing slash is required. The app intentionally rejects cleartext HTTP.
Re-sync Gradle after changing the value.

The application stores only the access token, encrypted by an AES/GCM key held in
Android Keystore. Passwords are never persisted. The active chat polls every four
seconds using the highest known server message ID; the polling coroutine is
cancelled when its navigation entry and `ChatViewModel` are removed.

