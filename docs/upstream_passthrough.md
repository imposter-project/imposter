# Upstream services (passthrough)

Imposter can forward a matched request to a real upstream HTTP service and
return the upstream's response verbatim, instead of serving a mocked one.

This is useful for **partial mocking** — mocking some endpoints while
proxying others to a live backend. It is supported by both the JVM and Go
engines and uses the same configuration schema, so a single config file works
across both.

> Not to be confused with the [`imposter proxy` CLI command](./proxy_endpoint.md),
> which records responses from a live endpoint into mock files, or with the
> [interceptor 'passthrough' mode](./interceptors.md#passthrough), which
> controls whether request processing continues to the next matcher.

## Overview

- **Named upstreams** — declare upstream services once at the top level
  under `upstreams`.
- **Per-resource opt-in** — a resource forwards by setting
  `passthrough: <upstream-name>`.
- **Verbatim forwarding** — the request method, path, query string, headers
  and body are forwarded; the upstream's status code, headers and body are
  returned to the client.
- **Mutually exclusive** — when a resource declares `passthrough`, normal
  response processing is bypassed: `response`, `steps`, `capture` and
  templating are not applied.

## Configuration

```yaml
plugin: rest

upstreams:
  myBackend:
    url: "http://upstream-api.example.com"
  secondService:
    url: "http://another-api.example.com:8080/api"

resources:
  - path: /users
    method: GET
    passthrough: myBackend

  - path: /products
    method: GET
    passthrough: secondService
```

In this example, a `GET /users` request is forwarded to
`http://upstream-api.example.com/users`, and a `GET /products` request is
forwarded to `http://another-api.example.com:8080/api/products`.

### Partial mocking

You can mix mocked resources and passthrough resources in the same config.
Exact-path matches outscore wildcard matches, so you can override a single
endpoint and proxy the rest:

```yaml
upstreams:
  jsonplaceholder:
    url: "https://jsonplaceholder.typicode.com"

resources:
  # Locally mocked — overrides the upstream for /users/1.
  - path: /users/1
    method: GET
    response:
      statusCode: 200
      content: |
        {"id": 1, "name": "Locally Mocked User"}

  # Everything else under /users is proxied verbatim.
  - path: /users/*
    method: GET
    passthrough: jsonplaceholder
```

### Path joining

The upstream's base path (if any) is prefixed to the incoming request path:

| Upstream `url`     | Request path  | Forwarded to              |
|--------------------|---------------|---------------------------|
| `http://api/v1`    | `/users`      | `http://api/v1/users`     |
| `http://api/v1/`   | `/users`      | `http://api/v1/users`     |
| `http://api`       | `/v1/users`   | `http://api/v1/users`     |

The request's query string is forwarded unchanged.

## Header handling

All request and response headers are forwarded, except the following
hop-by-hop headers (per RFC 2616 §13.5.1, plus `Accept-Encoding` and `Host`):

`Accept-Encoding`, `Host`, `Connection`, `Keep-Alive`, `Proxy-Authenticate`,
`Proxy-Authorization`, `TE`, `Trailers`, `Transfer-Encoding`, `Upgrade`.

The `Host` header sent to the upstream is derived from the upstream URL.

### Forwarded headers (optional)

Proxy-style headers are **not** added by default. Set
`IMPOSTER_PASSTHROUGH_FORWARDED_HEADERS=true` to inject:

- `X-Forwarded-For` — the client IP, appended to any existing value
- `X-Forwarded-Host` — the original `Host` header
- `X-Forwarded-Proto` — `http` or `https`
- `Via` — `1.1 imposter`

## Logging

Each forwarded request emits two log lines at `INFO`:

```
[INFO] forwarding to upstream "jsonplaceholder" - method:GET, url:https://jsonplaceholder.typicode.com/users/2
[INFO] upstream "jsonplaceholder" responded - status:200, length:509, took:62ms
```

The first line lets you see the rewritten target URL as the request leaves
Imposter; the second records the upstream's status, response body size and
round-trip latency. Both are followed by the standard `handled request` line
covering the response returned to the client.

## Timeouts and errors

The upstream request uses a default timeout of 30 seconds, overridable via
`IMPOSTER_PASSTHROUGH_TIMEOUT` (Go duration syntax, e.g. `5s`, `1m`).

| Condition | Response to client |
|-----------|--------------------|
| Upstream reachable, returns any status | The upstream status, headers and body are returned verbatim (including 4xx/5xx). |
| Upstream unreachable, times out, or connection refused | `502 Bad Gateway` |
| Resource references an upstream name that is not defined | Startup fails with a validation error. |

## System endpoints

Imposter's own system endpoints (paths under `/system`, such as
`/system/status` and `/system/store`) are handled before passthrough is
evaluated and are never forwarded to an upstream.

## Environment variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `IMPOSTER_PASSTHROUGH_TIMEOUT` | `30s` | Timeout for the forwarded upstream request. |
| `IMPOSTER_PASSTHROUGH_FORWARDED_HEADERS` | `false` | When `true`, inject `X-Forwarded-*` and `Via` headers. |

## Limitations

- Passthrough is supported on resources only; declaring it on an interceptor
  is ignored (a warning is logged at startup).
- The upstream URL is static and is not templated.
- The upstream response is not captured into stores and is not run through
  response templates.

## See also

- [Proxy an existing endpoint](./proxy_endpoint.md) — the `imposter proxy`
  CLI command for capturing responses to scaffold mocks.
- [Interceptors](./interceptors.md) — the unrelated 'passthrough mode' for
  interceptors that controls whether request processing continues.
- [REST passthrough example](https://github.com/imposter-project/imposter-go/tree/main/examples/rest/passthrough)
