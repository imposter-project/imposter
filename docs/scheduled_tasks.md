---
layout: default
title: Scheduled tasks
description: Run periodic tasks such as webhook-style HTTP pushes
---

# Scheduled tasks

> **Note**
> Schedules are available in Imposter 5.x (the `imposter-go` engine). They are not available in 4.x.

Schedules let a mock *initiate* actions on a timer, independent of any inbound request. A common use is sending webhook-style HTTP notifications to another system while the mock is running.

Schedules run [steps](./steps.md) — a `remote` step performs an outbound HTTP(S) request, and a `script` step runs JavaScript.

## Configuration

Add a top-level `schedules` array to any plugin's configuration:

```yaml
plugin: rest
resources:
  - path: /orders
    method: POST
    capture:
      lastOrderId:
        store: orders
        requestBody:
          jsonPath: $.id
    response:
      statusCode: 201

schedules:
  - name: order-webhook
    every: 30s
    steps:
      - type: remote
        url: ${env.WEBHOOK_URL}
        method: POST
        headers:
          Content-Type: application/json
        body: '{"event":"order.updated","orderId":"${stores.orders.lastOrderId}","at":"${datetime.now.iso8601_datetime}"}'
```

Step bodies, URLs and headers support [response templates](./templates.md), so scheduled requests can reference [store](./stores.md) data captured from earlier requests, environment variables, timestamps and random values.

### Schedule properties

| Property | Required | Meaning |
|----------|----------|---------|
| `name` | No | Identifies the schedule in logs |
| `every` | One of `every`/`cron` | Interval between runs, e.g. `30s`, `5m`, `1h` |
| `cron` | One of `every`/`cron` | Standard 5-field cron expression, e.g. `"0 * * * *"` |
| `limit` | No | Maximum number of times the schedule fires; when omitted, the schedule fires for the lifetime of the mock |
| `steps` | Yes | The [steps](./steps.md) to run on each firing |

Each schedule entry must declare exactly one of `every` or `cron`.

Runs of a given schedule do not overlap: if a run takes longer than the interval, the next run is delayed until it completes.

### Limiting how many times a schedule fires

Think carefully about setting `limit`. A schedule without one keeps firing for as long as the mock runs — for outbound pushes such as webhooks, that can mean an unbounded stream of requests to the receiving system, especially in long-lived deployments. Set `limit` unless the schedule genuinely needs to run forever:

```yaml
schedules:
  - name: order-webhook
    every: 30s
    limit: 10        # fire at most 10 times, then stop
    steps:
      - type: remote
        url: ${env.WEBHOOK_URL}
        method: POST
```

Once a schedule reaches its limit it stops permanently (until the mock restarts) and logs that it has done so.

Operators can also set a global default with the `IMPOSTER_SCHEDULE_LIMIT` environment variable — it applies to any schedule that does not declare its own `limit`, including connection-scoped WebSocket schedules. A schedule's own `limit` always takes precedence. There is no default value; when neither is set, schedules are unlimited.

## Cron expressions

The `cron` property accepts standard 5-field expressions (minute, hour, day of month, month, day of week):

```yaml
schedules:
  - name: hourly-report
    cron: "0 * * * *"    # top of every hour
    steps:
      - type: remote
        url: https://example.com/report
        method: POST
```

## Connection-scoped schedules (WebSocket)

The [WebSocket plugin](./websocket_plugin.md) supports the same schedule syntax on `on: open` resources. These schedules run for the lifetime of each connection (stopping when it closes) and can additionally send messages to the connected client via `response`/`responses`:

```yaml
plugin: websocket
resources:
  - path: /gateway
    on: open
    schedule:
      - every: 15s
        response:
          content: '{"event":"tick"}'
```

## Limitations

- Schedules are not supported when running as an AWS Lambda function, which does not provide a long-lived process.
- Top-level schedules cannot declare `response`/`responses` — there is no client to send them to.
