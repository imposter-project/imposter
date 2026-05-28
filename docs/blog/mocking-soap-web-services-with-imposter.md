# Mocking SOAP Web Services with Imposter

When a third party service goes down and causes all your integration tests to fail, you realise how brittle this can make your CI/CD pipeline. The same is just as true for SOAP services as for REST APIs.

This post shows how to mock a SOAP web service using [Imposter](https://www.imposter.sh).

## A simple web service

The example mocks a pet store service with a `/pets/` endpoint that returns the following SOAP envelope:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<env:Envelope xmlns:env="http://www.w3.org/2001/12/soap-envelope">
    <env:Header/>
    <env:Body>
        <getPetByIdResponse xmlns="urn:com:example:petstore">
            <id>3</id>
            <name>string</name>
        </getPetByIdResponse>
    </env:Body>
</env:Envelope>
```

## Configuration

The configuration file uses Imposter's SOAP plugin:

```yaml
# petstore-config.yaml
---
plugin: soap
wsdlFile: petstore.wsdl
```

Key points:

* Configuration files must use the `-config.yaml` suffix.
* The WSDL file defines the service endpoint and operations.
* The example defines a `getPetById` operation with request and response messages.

## Starting the mock server

**Prerequisites:**

* Docker installed and running.
* WSDL and configuration files in your working directory.

**Command:**

```bash
docker run -ti -p 8080:8080 \
    -v $PWD:/opt/imposter/config \
    outofcoffee/imposter-all
```

The output should confirm the mock is running on `http://localhost:8080`.

## Testing the mock

Send a sample request:

```bash
curl -X POST "http://localhost:8080/pets/" \
  -H 'Content-Type: application/soap+xml' \
  -d '<?xml version="1.0" encoding="UTF-8"?>
  <env:Envelope xmlns:env="http://www.w3.org/2001/12/soap-envelope">
      <env:Body>
          <getPetByIdRequest xmlns="urn:com:example:petstore">
              <id>3</id>
          </getPetByIdRequest>
      </env:Body>
  </env:Envelope>'
```

The mock returns the predefined SOAP response.

## Customising responses

Enhanced configuration enables request matching:

```yaml
plugin: soap
wsdlFile: petstore.wsdl
resources:
  - operation: getPetById
    requestBody:
      xPath: "/env:Envelope/env:Body/pets:getPetByIdRequest/pets:id"
      value: "3"
      xmlNamespaces:
        env: "http://www.w3.org/2001/12/soap-envelope"
        pets: "urn:com:example:petstore"
    response:
      staticFile: getPetByIdResponse.xml
```

This matches requests containing `id="3"` and returns a custom XML file.

## Fault responses

Simulate error conditions:

```yaml
  - operation: getPetById
    requestBody:
      xPath: "/env:Envelope/env:Body/pets:getPetByIdRequest/pets:id"
      value: "99"
      xmlNamespaces:
        env: "http://www.w3.org/2001/12/soap-envelope"
        pets: "urn:com:example:petstore"
    response:
      staticFile: fault.xml
      statusCode: 500
```

## Summary

Imposter automatically synchronises mocks with WSDL definitions. Configuration changes require restarting the container (`CTRL+C` to stop). For advanced scenarios, Imposter supports JavaScript and Groovy scripting for dynamic responses.

A hosted alternative is available at [mocks.cloud](https://www.mocks.cloud), and example code is available on [GitHub](https://github.com/outofcoffee/imposter).
