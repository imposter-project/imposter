---
layout: default
title: gRPC (and Protobuf) plugin
description: Imposter gRPC mock plugin
---

# gRPC (and Protobuf) plugin

Plugin name: `grpc`

The gRPC plugin mocks gRPC services using `.proto` files and JSON response definitions. It is available as an external plugin in Imposter 5.x.

## Features

- Proto file parsing (no `protoc` required)
- JSON-based responses, converted to protobuf automatically
- Native gRPC support via HTTP/2 (h2 and h2c)
- Full pipeline support — request matching, interceptors, capture, steps, scripting, and response templating

## Install plugin

### Option 1: Using the CLI

To use this plugin, install it with the [Imposter CLI](./run_imposter_cli.md):

    imposter plugin install -d grpc -t golang

This will install the plugin version matching the current engine version used by the CLI. The next time you run `imposter up`, the plugin will be available.

### Option 2: Install the plugin manually

To use this plugin, download the `plugin-grpc-<arch>` file from the [Releases page](https://github.com/imposter-project/imposter-go-plugins/releases) and extract the binary into the plugin directory.

Enable it with the following environment variables:

    IMPOSTER_EXTERNAL_PLUGINS=true
    IMPOSTER_PLUGIN_DIR="/path/to/dir/containing/plugin"

## Usage example

Once you've started Imposter, you can connect to your gRPC mock.

Here's an example using `grpcurl`:

```bash
grpcurl -plaintext -proto petstore.proto \
  -d '{"id": 1}' \
  localhost:8080 store.PetStore/GetPet
```

## Configuration

Set `plugin: grpc` in your config file. Specify proto files in the `config` block and define responses using the standard `resources` block.

```yaml
plugin: grpc

config:
  protoFiles:
    - "petstore.proto"

resources:
  - path: "/store.PetStore/GetPet"
    response:
      file: "get-pet-response.json"
  - path: "/store.PetStore/ListPets"
    response:
      file: "list-pets-response.json"
```

### Configuration reference

| Field | Description |
|-------|-------------|
| `config.protoFiles` | List of `.proto` files to parse, relative to the config directory |
| `resources[].path` | gRPC method path, e.g. `/package.Service/Method` |
| `resources[].response.file` | Path to a JSON file containing the response body |
| `resources[].response.content` | Inline JSON response body (alternative to `file`) |
| `resources[].response.template` | Enable response templating (default: `false`) |

## Response format

Response files contain JSON matching the structure of the protobuf response message. The plugin converts JSON to protobuf wire format automatically using the message descriptors from the `.proto` file.

## Availability

This plugin is available in **Imposter 5.x** (imposter-go). It is not available in 4.x.

See the [imposter-go repository](https://github.com/imposter-project/imposter-go) for source and examples.
