# Mocking gRPC Services with Imposter

When a third party service goes down and causes all your integration tests to fail, you realise how brittle this can make your CI/CD pipeline. The same is just as true for gRPC services as for REST or SOAP APIs — and the tooling to mock them has historically been thinner on the ground.

This post shows how to mock a gRPC service using [Imposter](https://www.imposter.sh) and a Protocol Buffers (`.proto`) file.

## A simple service

The example mocks a pet store service defined in protobuf, with two RPCs — `GetPet` and `ListPets`:

```protobuf
syntax = "proto3";

package store;

service PetStore {
  // GetPet returns a single pet by ID.
  rpc GetPet (GetPetRequest) returns (GetPetResponse);

  // ListPets returns all pets.
  rpc ListPets (ListPetsRequest) returns (ListPetsResponse);
}

message GetPetRequest {
  int32 id = 1;
}

message GetPetResponse {
  int32 id = 1;
  string name = 2;
  string species = 3;
}

message ListPetsRequest {}

message ListPetsResponse {
  repeated GetPetResponse pets = 1;
}
```

Imposter parses the `.proto` file at startup — there's no need to run `protoc` or generate stubs.

## Configuration

The configuration file uses Imposter's gRPC plugin. Proto files are listed under the `config` block, and responses are defined using the standard `resources` block as JSON files:

```yaml
# imposter-config.yaml
---
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

Key points:

* Configuration files must use the `-config.yaml` suffix.
* `protoFiles` are resolved relative to the configuration directory.
* The `path` for each resource is the gRPC method path, in the form `/package.Service/Method`.
* Response bodies are written as JSON and converted to protobuf wire format automatically using the message descriptors from the `.proto` file.

An example `get-pet-response.json`:

```json
{
  "id": 1,
  "name": "Fido",
  "species": "Dog"
}
```

## Starting the mock server

The gRPC plugin is an external plugin available in Imposter 5.x. The easiest way to install it is via the [Imposter CLI](https://docs.imposter.sh/run_imposter_cli/). On macOS or Linux you can install the CLI with Homebrew:

```bash
brew tap imposter-project/imposter
brew trust imposter-project/imposter
brew install imposter
```

Then install the gRPC plugin:

```bash
imposter plugin install -d grpc -t native
```

This installs the plugin version matching the engine version used by the CLI. Start the mock with:

```bash
imposter up ./examples/grpc/simple
```

The mock server listens on `http://localhost:8080` with HTTP/2 cleartext (h2c), so gRPC clients can connect directly.

## Testing the mock

The simplest way to call the mock is with [`grpcurl`](https://github.com/fullstorydev/grpcurl).

Fetch a pet by id:

```bash
grpcurl -plaintext -proto petstore.proto \
  -d '{"id": 1}' \
  localhost:8080 store.PetStore/GetPet
```

Returns:

```json
{
  "id": 1,
  "name": "Fido",
  "species": "Dog"
}
```

List all pets:

```bash
grpcurl -plaintext -proto petstore.proto \
  localhost:8080 store.PetStore/ListPets
```

Returns the full list defined in `list-pets-response.json`.

## Customising responses

Because the gRPC plugin uses Imposter's core request pipeline, you get request matching, interceptors, capture, steps, scripting and response templating for free.

For example, you can match on the request body to return a specific response when `id=1`, and a default response otherwise:

```yaml
resources:
  # when id=1, return a specific response
  - path: "/store.PetStore/GetPet"
    requestBody:
      jsonPath: $.id
      value: 1
    response:
      file: "get-pet-response.json"

  # default response for this RPC — gRPC NOT_FOUND
  - path: "/store.PetStore/GetPet"
    response:
      statusCode: 5
```

The `statusCode` field maps directly to gRPC status codes — `5` is `NOT_FOUND`.

## Inline responses and templating

For small responses, inline JSON content with templating is often the most concise option:

```yaml
resources:
  - path: "/store.PetStore/GetPet"
    response:
      content: '{"id": 1, "name": "Fido", "species": "Dog"}'
      template: true
```

## Summary

Imposter parses your `.proto` files at startup and converts JSON response bodies to protobuf wire format automatically — no codegen or hand-rolled stubs required. Configuration changes need a restart (`CTRL+C` to stop).

For full details and more examples, see the [gRPC plugin documentation](https://docs.imposter.sh/grpc_plugin/) and the [imposter-go repository](https://github.com/imposter-project/imposter-go).
