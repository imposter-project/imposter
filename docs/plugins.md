# Plugins

Imposter uses plugins to control its behaviour and provide specialised mocks.

The following table describes the available plugins.

| Category       | Plugin name       | Description                            | Details                                                                                                                                          | Supported versions |
|----------------|-------------------|----------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|--------------------|
| Mock           | `grpc`            | gRPC mocks.                            | External plugin (5.x). See [gRPC plugin](grpc_plugin.md).                                                                                        | 5.x                |
| Mock           | `hbase`           | HBase mocks.                           | See [HBase plugin](hbase_plugin.md).                                                                                                             | 4.x                |
| Mock           | `openapi`         | OpenAPI (and Swagger) mocks.           | Built-in. See [OpenAPI (and Swagger) plugin](openapi_plugin.md).                                                                                 | 4.x, 5.x           |
| Mock           | `rest`            | REST mocks.                            | Built-in. See [REST plugin](rest_plugin.md).                                                                                                     | 4.x, 5.x           |
| Mock           | `sfdc`            | SFDC (Salesforce) mocks.               | See [SFDC (Salesforce) plugin](sfdc_plugin.md).                                                                                                  | 4.x                |
| Mock           | `soap`            | SOAP (and WSDL) mocks.                 | Built-in. See [SOAP plugin](soap_plugin.md).                                                                                                     | 4.x, 5.x           |
| Mock           | `wiremock`        | WireMock mappings support.             | See [WireMock plugin](wiremock_plugin.md).                                                                                                       | 4.x                |
| Scripting      | `js-graal`        | Graal.js scripting.                    | Graal.js JavaScript scripting support. This is the default JavaScript script engine. See [Modern JavaScript features](./scripting_modern_js.md). | 4.x                |
| Scripting      | `js-nashorn`      | Nashorn scripting.                     | This is the legacy JavaScript script engine                                                                                                      | 4.x                |
| Store          | `store-dynamodb`  | DynamoDB store implementation.         | See [DynamoDB store](https://github.com/imposter-project/imposter-jvm-engine/tree/main/store/dynamodb).                                          | 4.x                |
| Store          | `store-redis`     | Redis store implementation.            | See [Redis store](https://github.com/imposter-project/imposter-jvm-engine/tree/main/store/redis).                                                | 4.x                |
| Store          | `store-graphql`   | GraphQL store queries.                 | See [GraphQL](stores_graphql.md).                                                                                                                | 4.x                |
| Configuration  | `config-detector` | Detects plugins from `*-config` files. | Built-in                                                                                                                                         | 4.x                |
| Configuration  | `meta-detector`   | Detects plugins from `META-INF`.       | Built-in                                                                                                                                         | 4.x                |
| Data generator | `fake-data`       | Generates fake data.                   | See [Fake data generator](fake_data.md).                                                                                                         | 4.x, 5.x           |
| Authentication | `oidc-server`     | OpenID Connect authorization server.  | External plugin (5.x). Provides a full OIDC authorization server for testing auth flows.                                                         | 5.x                |
| Documentation  | `swaggerui`       | Interactive Swagger UI viewer.         | External plugin (5.x). Built-in (4.x). Serves an interactive UI for OpenAPI specifications at `/_spec/`.                                                         | 4.x, 5.x           |
| Documentation  | `wsdlweb`         | Interactive WSDL/SOAP viewer.          | External plugin (5.x). Serves an interactive UI for WSDL files at `/_wsdl/`.                                                                    | 5.x                |

You can also write your own plugins, if you want to customise behaviour further.

## Plugin loading

Imposter loads plugins from the _plugin directory_. This is configured using the following environment variable:

    IMPOSTER_PLUGIN_DIR="/path/to/dir/containing/plugins"

### Imposter 4.x

When you set the plugin directory environment variable in Imposter 4.x, plugin JAR files placed in this directory will be loaded by Imposter on startup.

### Imposter 5.x

Imposter 5.x requires you to also set the `IMPOSTER_EXTERNAL_PLUGINS` environment variable:

    IMPOSTER_EXTERNAL_PLUGINS=true
    IMPOSTER_PLUGIN_DIR="/path/to/dir/containing/plugins"

See [imposter-go](https://github.com/imposter-project/imposter-go/tree/main/external/plugins) for available plugins.

## Using the CLI

If you are using the [Imposter CLI](./run_imposter_cli.md), you can install a plugin with:

    imposter plugin install -d <plugin name>

The CLI automatically manages the plugin directory, so you do not have to set the `IMPOSTER_PLUGIN_DIR` environment variable.

For example:

    imposter plugin install -d stores-dynamodb

This will install the plugin version matching the current engine version used by the CLI. The next time you run `imposter up`, the plugin will be available.

## Using the Docker image

If you are using the [Docker image](./run_imposter_docker.md), you can bind-mount a local directory to the `/opt/imposter/plugins` directory within the container.

For example:

    docker run --rm -it \
        -v /path/to/plugin/dir:/opt/imposter/plugins \
        -v /path/to/config/dir:/opt/imposter/config \
        -p 8080:8080 \
        outofcoffee/imposter

The Docker container sets the environment variable `IMPOSTER_PLUGIN_DIR=/opt/imposter/plugins`, so you do not have to set it explicitly.
