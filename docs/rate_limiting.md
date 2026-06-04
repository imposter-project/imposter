# Rate limiting

> **Available in Imposter 5 and later**

Rate limiting lets you control the number of concurrent requests to your mock endpoints. This is useful for simulating real-world API behaviour, testing system resilience, and reproducing overload conditions during load testing.

## Overview

The rate limiter tracks active concurrent requests per resource and returns a different response when configurable thresholds are exceeded. It supports:

- **Concurrent request limiting**: control how many requests can be processed simultaneously
- **Multiple threshold tiers**: define different responses for different concurrency levels
- **Progressive throttling**: add delays, return errors, or apply custom responses
- **Store backend flexibility**: works with in-memory, Redis, and DynamoDB [stores](./stores.md)
- **Distributed operation**: consistent behaviour across multiple server instances (with shared stores)

## Configuration

Rate limiting is configured per resource using the `concurrency` property. Each concurrency limit defines a `threshold` and the `response` to return when that threshold is exceeded.

### Basic rate limiting

```yaml
plugin: rest
resources:
  - path: /api/users
    method: GET
    concurrency:
      - threshold: 5
        response:
          statusCode: 429
          content: "Too many concurrent requests"
          headers:
            Retry-After: "10"
    response:
      statusCode: 200
      content: "User data"
```

In this example:

- Normal requests (1-5 concurrent) return the standard 200 response
- When more than 5 requests are active, additional requests receive a 429 "Too Many Requests" response

### Multiple thresholds

You can define multiple concurrency thresholds with different responses for progressive rate limiting:

```yaml
plugin: rest
resources:
  - path: /api/heavy-operation
    method: POST
    concurrency:
      # First tier: add a delay to slow down requests
      - threshold: 3
        response:
          statusCode: 200
          content: "Request throttled"
          delay:
            exact: 2000  # 2 second delay
          headers:
            X-Throttled: "true"

      # Second tier: return 503 Service Unavailable
      - threshold: 7
        response:
          statusCode: 503
          content: "Service temporarily overloaded"
          headers:
            Retry-After: "30"

      # Third tier: return 429 Rate Limit Exceeded
      - threshold: 10
        response:
          statusCode: 429
          content: "Rate limit exceeded"
    response:
      statusCode: 202
      content: "Operation accepted"
```

With this configuration:

- **1-3 concurrent requests**: normal 202 response
- **4-7 concurrent requests**: 200 response with a 2-second delay and throttling header
- **8-10 concurrent requests**: 503 Service Unavailable
- **11+ concurrent requests**: 429 Rate Limit Exceeded

### SOAP rate limiting

Rate limiting works with SOAP endpoints using the operation name:

```yaml
plugin: soap
resources:
  - operation: getUserDetails
    concurrency:
      - threshold: 3
        response:
          statusCode: 503
          content: |
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <soap:Fault>
                  <faultcode>Server</faultcode>
                  <faultstring>Service temporarily overloaded</faultstring>
                </soap:Fault>
              </soap:Body>
            </soap:Envelope>
    response:
      statusCode: 200
      content: |
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
          <soap:Body>
            <getUserDetailsResponse>
              <user>
                <id>123</id>
                <name>John Doe</name>
              </user>
            </getUserDetailsResponse>
          </soap:Body>
        </soap:Envelope>
```

## Response configuration

Rate limit responses support the same features as regular responses.

### Status codes and content

```yaml
concurrency:
  - threshold: 5
    response:
      statusCode: 429
      content: "Rate limit exceeded"
```

### Custom headers

```yaml
concurrency:
  - threshold: 5
    response:
      statusCode: 503
      headers:
        Retry-After: "30"
        X-RateLimit-Limit: "5"
        X-RateLimit-Remaining: "0"
      content: "Service temporarily unavailable"
```

### Delays

Rate limit responses can inject delays in the same way as [performance simulation](./performance_simulation.md).

```yaml
concurrency:
  - threshold: 3
    response:
      delay:
        exact: 1000  # fixed 1-second delay
      statusCode: 200
      content: "Throttled response"

  - threshold: 5
    response:
      delay: # random delay between 0.5-2 seconds
        min: 500
        max: 2000
      statusCode: 200
      content: "Heavily throttled"
```

### Templated responses

Rate limit responses support [response templates](./templates.md):

```yaml
concurrency:
  - threshold: 5
    response:
      statusCode: 429
      template: true
      content: |
        {
          "error": "Rate limit exceeded",
          "timestamp": "${datetime.now.iso8601_datetime}",
          "retry_after": 30
        }
      headers:
        Content-Type: application/json
```

## Store backends

Rate limit counters are held in a [store](./stores.md), so the behaviour depends on the store backend you choose.

### In-memory store (default)

No configuration is needed; this is the default.

```bash
imposter up
```

**Characteristics:**

- Fast performance
- Per-instance rate limiting
- Suitable for single-instance deployments
- Counters are lost on restart

### Redis store

```bash
export IMPOSTER_STORE_DRIVER=store-redis
export REDIS_ADDR=localhost:6379
imposter up
```

**Characteristics:**

- Shared rate limiting across multiple instances
- Persistent across restarts
- High performance with atomic operations
- Suitable for distributed deployments

### DynamoDB store

```bash
export IMPOSTER_STORE_DRIVER=store-dynamodb
export IMPOSTER_DYNAMODB_TABLE=imposter-store
imposter up
```

**Characteristics:**

- Shared rate limiting across multiple instances
- Fully managed and scalable
- Automatic TTL support
- Suitable for cloud deployments

## Environment variables

Configure rate limiting behaviour with these [environment variables](./environment_variables.md):

| Variable | Purpose | Default | Example |
|----------|---------|---------|---------|
| `IMPOSTER_RATE_LIMITER_TTL` | TTL for rate limit entries (seconds) | `300` (5 minutes) | `IMPOSTER_RATE_LIMITER_TTL=600` |
| `IMPOSTER_STORE_DRIVER` | Store backend selection | in-memory | `IMPOSTER_STORE_DRIVER=store-redis` |
| `REDIS_ADDR` | Redis server address (if using Redis) | - | `REDIS_ADDR=localhost:6379` |
| `IMPOSTER_DYNAMODB_TABLE` | DynamoDB table name (if using DynamoDB) | - | `IMPOSTER_DYNAMODB_TABLE=imposter-store` |

## How it works

### Threshold logic

Rate limiting uses "greater than" logic to determine when thresholds are exceeded:

- A `threshold` of `3` means rate limiting applies when there are **more than 3** concurrent requests (i.e. 4 or more)
- Multiple thresholds are evaluated in order, with the highest matching threshold taking precedence

### Request processing flow

1. **Request arrives** at the configured endpoint
2. **Counter incremented** atomically for the resource
3. **Threshold evaluation** checks whether any limits are exceeded
4. **If rate limited**: the counter is rolled back and the rate limit response is returned
5. **If not rate limited**: the request is processed normally
6. **After the response**: the counter is decremented automatically

### Resource identification

Each resource is identified by a unique key that includes:

- **HTTP method** (GET, POST, etc.) or `*` for SOAP
- **Resource path** (REST) or operation name (SOAP)
- **Hash of matching criteria** (if the resource has specific headers, query parameters, etc.)

This ensures that resources with the same path but different matching criteria get separate rate limit counters.

## Error handling

The rate limiter follows a "fail-open" approach:

- **Store failures** allow requests to proceed normally (rate limiting is disabled)
- **Network issues** do not block request processing
- **Counter errors** are logged but do not impact request flow

This ensures that rate limiting enhances your testing without compromising availability.

## Load testing

Rate limiting integrates well with load testing tools such as [`hey`](https://github.com/rakyll/hey):

```bash
# Test concurrent requests
hey -n 100 -c 20 -m GET http://localhost:8080/api/users

# Test a different endpoint
hey -n 50 -c 10 -m POST http://localhost:8080/api/heavy-operation
```

Monitor the output for the status code distribution to see rate limiting in action:

```
Status code distribution:
  [200]  75 responses  # normal responses
  [429]  20 responses  # rate limited
  [503]   5 responses  # service overloaded
```

## Best practices

### 1. Progressive throttling

Use multiple thresholds to handle load gracefully:

```yaml
concurrency:
  - threshold: 5    # warn with delays
    response:
      delay: { exact: 1000 }
      headers: { X-Throttled: "true" }
  - threshold: 10   # soft rejection
    response:
      statusCode: 503
      headers: { Retry-After: "10" }
  - threshold: 20   # hard rejection
    response:
      statusCode: 429
```

### 2. Appropriate HTTP status codes

- **429 Too Many Requests**: for hard rate limits
- **503 Service Unavailable**: for temporary overload
- **200 OK with delays**: for throttling without errors

### 3. Helpful headers

Include headers to help clients understand the rate limiting:

```yaml
headers:
  Retry-After: "30"
  X-RateLimit-Limit: "10"
  X-RateLimit-Remaining: "0"
  X-RateLimit-Reset: "${datetime.now.plus_seconds(30).epoch_second}"
```

### 4. Realistic thresholds

Set thresholds based on your actual API capacity:

- **Database APIs**: lower thresholds (2-5 concurrent)
- **Cache APIs**: higher thresholds (20-50 concurrent)
- **File uploads**: very low thresholds (1-2 concurrent)

### 5. Store selection

Choose the appropriate store backend for your testing scenario:

- **In-memory**: single-instance testing
- **Redis**: multi-instance testing with shared state
- **DynamoDB**: cloud-based distributed testing

## Availability

Rate limiting is available in **Imposter 5.x** onwards. It is not available in 4.x.
