# Mocking APIs with OpenAPI and Imposter

When building an API-driven application it's common for the API specification to be defined up front, then for the client and server to be developed in parallel. While the specification enables that parallel development, testing the client really needs something more tangible: a live mock service that behaves like the real thing.

This post walks through standing up a mock for a simple API using [Imposter](https://www.imposter.sh) and an OpenAPI/Swagger specification.

## A simple specification

The example is a pet store API with a single `/pets` endpoint that returns a JSON array of pets:

```json
[
  {
    "id": 101,
    "name": "Cat"
  },
  {
    "id": 102,
    "name": "Dog"
  }
]
```

### The OpenAPI specification

The Swagger 2.0 specification describing this API looks like:

```yaml
---
swagger: "2.0"
info:
  version: "1.0.0"
  title: "Swagger Petstore"
consumes:
- "application/json"
produces:
- "application/json"
paths:
  /pets:
    get:
      description: "Returns all pets from the system"
      produces:
        - "application/json"
      responses:
        "200":
          description: "A list of pets."
          schema:
            type: "array"
            items:
              $ref: "#/definitions/Pet"
          examples:
            application/json: |-
              [
                {
                  "id": 101,
                  "name": "Cat"
                },
                {
                  "id": 102,
                  "name": "Dog"
                }
              ]
definitions:
  Pet:
    type: "object"
    required:
      - "id"
      - "name"
    properties:
      id:
        type: "integer"
        format: "int64"
      name:
        type: "string"
```

The key elements here are:

* a `GET` endpoint at `/pets`
* a JSON response
* an HTTP 200 response
* a `Pet` data model
* an example response

## Creating a mock

### Configuration file

A minimal Imposter configuration looks like this:

```yaml
---
plugin: openapi
specFile: petstore.yaml
```

> **Important:** Configuration files must use the `-config.yaml` suffix — for example, `petstore-config.yaml`.

### Prerequisites

1. Docker must be installed and running.
2. The configuration file and the specification file must be in the same directory.

For example:

```
-rw-r--r--  1 pete  staff  104 16 Sep 13:55 petstore-config.yaml
-rw-r--r--  1 pete  staff  998 16 Sep 13:55 petstore.yaml
```

### Starting the mock server

Launch Imposter with Docker:

```bash
docker run -ti -p 8080:8080 \
    -v $PWD:/opt/imposter/config \
    outofcoffee/imposter
```

You should see output similar to:

```
Loading configuration file: /opt/imposter/config/petstore-config.yaml
Loaded 1 plugin configuration files from: [/opt/imposter/config]
...
reading from /opt/imposter/config/petstore.yaml
Adding mock endpoint: GET -> /pets
Mock engine up and running on http://localhost:8080
```

### Testing the mock

Verify the mock is working with `curl`:

```bash
$ curl -X GET "http://localhost:8080/pets"
[
  {
    "id": 101,
    "name": "Cat"
  },
  {
    "id": 102,
    "name": "Dog"
  }
]
```

### Exploring the mock

Imposter also exposes a Swagger UI for the mock at [http://localhost:8080/_spec/](http://localhost:8080/_spec/), so you can explore the specification visually.

## Key advantages

Mock endpoints are generated automatically from the specification. When the specification changes, restart Imposter to pick up the changes. Stop the running container with `CTRL+C`.

## Advanced customisation

Beyond serving the static examples embedded in the specification, Imposter supports JavaScript and Groovy scripts for dynamic response control. See the [scripting documentation](https://docs.imposter.sh/scripting/) for details.

## Hosted alternative

A managed hosting option is available at [www.mocks.cloud](https://www.mocks.cloud), with a free trial.

## Resources

Complete example code is available on [GitHub](https://github.com/outofcoffee/imposter/tree/main/examples/openapi).
