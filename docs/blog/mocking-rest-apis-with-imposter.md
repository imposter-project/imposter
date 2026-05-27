# Mocking REST APIs with Imposter

When a third party service goes down and causes all your integration tests to fail, you realise how brittle this can make your CI/CD pipeline. Mocking those services is a great way to keep your tests fast, reliable and self-contained.

This post shows how to mock a simple REST API using [Imposter](https://www.imposter.sh).

## A simple API

The example mocks an endpoint that returns a JSON array of cats — each one with an `id`, a `name` and a `type`.

## Configuration

The setup needs two files: a YAML configuration file (suffixed `-config.yaml`) and a JSON data file. The configuration looks like this:

```yaml
---
plugin: rest
contentType: application/json
resources:
  - path: "/cats"
    response:
      staticFile: cats.json
  - path: "/cats/:id"
    type: array
    response:
      staticFile: cats.json
```

Key features:

* The `/cats` endpoint returns the complete JSON array.
* The `/cats/:id` endpoint returns the item from the array whose `id` matches the path parameter.
* Both files must live in the same directory.

## Starting the mock server

### Prerequisites

* Docker installed and running.
* Configuration and JSON files in your working directory.

### Launch command

```bash
docker run -ti -p 8080:8080 \
    -v $PWD:/opt/imposter/config \
    outofcoffee/imposter
```

The server runs on `http://localhost:8080`.

## Testing

### Full array request

```bash
curl "http://localhost:8080/cats"
```

Returns the complete array with status 200.

### Single item request

```bash
curl "http://localhost:8080/cats/1"
```

Returns the matching object — the one whose `id` matches the path parameter.

## Advanced configuration

A more sophisticated example demonstrates `POST` requests with custom status codes and headers:

```yaml
---
plugin: rest
path: /example-two
contentType: "text/html"
method: POST
response:
  statusCode: 201
  headers:
    X-Example: "foo"
  staticData: |
    <html>
      <head>
        <title>Example</title>
      </head>
      <body>
        Hello, world!
      </body>
    </html>
```

Test it with:

```bash
curl -X POST "http://localhost:8080/example-two"
```

## Additional capabilities

* **Scripts** — JavaScript or Groovy scripts can drive dynamic responses.
* **Configuration updates** — restart Imposter to apply changes (`CTRL+C` to stop).
* **Hosted option** — [www.mocks.cloud](https://www.mocks.cloud) provides a managed alternative.
* **Example code** — available on [GitHub](https://github.com/outofcoffee/imposter).
