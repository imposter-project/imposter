# Imposter: Scriptable, multipurpose mock server

[![CI](https://github.com/imposter-project/imposter-jvm-engine/actions/workflows/cicd.yaml/badge.svg)](https://github.com/imposter-project/imposter-jvm-engine/actions/workflows/cicd.yaml)

> Mock server for REST APIs, OpenAPI (and Swagger) specifications, SOAP web services (and WSDL files), Salesforce and HBase APIs.
>
> - Run **standalone** mock servers in Docker, Kubernetes, AWS Lambda or on the JVM.
> - **Embed** mocks within your tests (JVM or Node.js) to remove external dependencies.
> - Script **dynamic** responses using JavaScript, Groovy or Java.
> - **Capture** data from requests, then store it or return a **templated** response.
> - **Proxy** an existing endpoint to replay its responses as a mock.

![Imposter logo](./docs/images/composite_logo13_cropped.png)

## [Read the documentation here](https://docs.imposter.sh/)

## Features

- run standalone mocks in place of real systems
- turn an OpenAPI/Swagger file or WSDL file into a mock API for dev or QA (use it before the real API is built)
- decouple your integration tests from the cloud/back-end systems and take control of your dependencies
- validate your API requests against an OpenAPI specification
- capture data to retrieve later, or use in templates to for conditional responses
- proxy an existing endpoint to replay its responses as a mock

Send dynamic responses:

- Provide mock responses using static files or customise behaviour based on characteristics of the request.
- Power users can control mock responses with JavaScript or Java/Groovy script engines.
- Advanced users can write their own plugins in a JVM language of their choice.

## Getting started

The quickest way to get up and running is to use the free cloud-hosted service at **[mocks.cloud](https://www.mocks.cloud)**

## User documentation

**[Read the user documentation here](https://docs.imposter.sh/)**

## Tutorials

- [Mocking APIs with OpenAPI and Imposter](https://medium.com/@outofcoffee/mocking-apis-with-swagger-and-imposter-3694bd1733c0)
- [Mocking REST APIs with Imposter](https://medium.com/@outofcoffee/mocking-apis-with-imposter-53bd908632e5)
- [Mocking SOAP web services with Imposter](https://medium.com/@outofcoffee/mocking-soap-web-services-with-imposter-da8e9666b5b4)

*****

## Mock types

Imposter provides specialised mocks for the following scenarios:

- **[OpenAPI](https://docs.imposter.sh/openapi_plugin)** - Support for OpenAPI (and Swagger) API specifications.
- **[REST](https://docs.imposter.sh/rest_plugin)** - Mocks RESTful or plain HTTP APIs.
- **[SOAP](https://docs.imposter.sh/soap_plugin)** - Support for SOAP web services (and WSDL files).
- **[gRPC](https://docs.imposter.sh/grpc_plugin)** - Support for gRPC services using `.proto` files. _(5.x only)_
- **[HBase](https://docs.imposter.sh/hbase_plugin)** - Basic HBase mock implementation.
- **[SFDC (Salesforce)](https://docs.imposter.sh/sfdc_plugin)** - Basic Salesforce mock implementation.
- **[WireMock](https://docs.imposter.sh/wiremock_plugin)** - Support for WireMock mappings files.

These use a plugin system, so you can also create your own plugins, using any JVM language.

## Example

```shell
$ imposter up

Starting server on port 8080...
Parsing configuration file: someapi-config.yaml
...
Mock server is up and running
```

Your mock server is now running! Here Imposter provides HTTP responses to simulate an API that accepts users and returns a dynamic response containing the user ID from the request.

```shell
$ curl -v -X PUT http://localhost:8080/users/alice

HTTP/1.1 201 Created
Content-Type: application/json

{ "userName": "alice" }
```

This is a trivial example, which you can extend with conditional logic, request validation, data capture and much more... 

## How to run Imposter

There are many ways to run Imposter.

### Standalone mock server

- Using the command line client - see [Imposter CLI](https://docs.imposter.sh/run_imposter_cli)
- As a Docker container - see [Imposter Docker container](https://docs.imposter.sh/run_imposter_docker)
- As a Lambda function in AWS - see [Imposter AWS Lambda](https://docs.imposter.sh/run_imposter_aws_lambda)
- As a JAR file on the JVM - see [Imposter JAR file](https://docs.imposter.sh/run_imposter_jar)

### Embedded in tests

- Embedded within your **Java/Kotlin/Scala/JVM** unit tests - see [JVM bindings](https://docs.imposter.sh/embed_jvm)
- Embedded within your **JavaScript/Node.js** unit tests - see [JavaScript bindings](https://github.com/imposter-project/imposter-js)

### Within your CI/CD pipeline

- Use the [Imposter GitHub Actions](https://docs.imposter.sh/github_actions) to start and stop Imposter during your CI/CD pipeline.

---

## Contributing

- Pull requests are welcome.
- For Imposter versions up to and including 4.x, see [imposter-jvm-engine](https://github.com/imposter-project/imposter-jvm-engine)
- For Imposter version 5.x, see [imposter-go](https://github.com/imposter-project/imposter-go)

## Author

Pete Cornish (outofcoffee@gmail.com)
