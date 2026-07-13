# Roadmap

This section lists future ideas for features and improvements. Feel free to submit a suggestion by raising an issue.

## Features

* Non-HTTP transports (WebSocket supported since 5.x - see [WebSocket plugin](websocket_plugin.md))
* Asynchronous requests (i.e. callbacks)
* Asynchronous responses (WebSocket streaming supported since 5.x - see [WebSocket plugin](websocket_plugin.md))
* Scheduled HTTP(S) invocations (supported since 5.x - see [Scheduled tasks](scheduled_tasks.md))
* Scheduled script executions (supported since 5.x - see [Scheduled tasks](scheduled_tasks.md))
* Request and response validation against a JSON Schema file (instead of just OpenAPI spec)
* SOAP plugin - validate request/response body against XSD.
* Autogenerate OpenAPI spec/UI for REST plugin.

## Improvements

### Capture

- Replace `const` and `expression` with `value` key in capture block.

### HBase

* Add content type header to HBase response
* Reuse HBase model classes for JSON serialisation

## Breaking changes

The following items are breaking changes, such as removal of deprecated functionality. They will be removed or changed in a backwards incompatible way in a future major version.

- Remove deprecated `withData(String?)` script function
- Request and response validation will be enabled by default
- Remove deprecated `IMPOSTER_S3_CONFIG_URL` environment variable
