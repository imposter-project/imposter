---
layout: default
title: WebSocket plugin
description: Imposter WebSocket mock plugin
---

# WebSocket plugin

Plugin name: `websocket`

> **Note**
> This plugin is available in Imposter 5.x (the `imposter-go` engine). It is not available in 4.x.

The WebSocket plugin mocks WebSocket APIs. Clients connect over a standard WebSocket upgrade, then the mock can:

- send server-initiated messages when a connection opens
- match incoming messages using the standard [request matching](./request_matching.md) syntax and reply
- stream an ordered sequence of messages with configurable delays
- push messages periodically for the lifetime of a connection (e.g. a keepalive tick)

It is built in to the engine — no plugin installation is required.

## Features

- Full pipeline support — request matching, interceptors, capture, steps, scripting, and [response templating](./templates.md)
- Connection lifecycle triggers: `open`, `message` (the default) and `close`
- Multi-message streaming via the `responses` list, with per-message [delays](./performance_simulation.md)
- Connection-scoped [schedules](./scheduled_tasks.md) for periodic server-initiated messages

## Configuration

Resources describe *when to act* and *what to send*. The `on` property selects the trigger:

| `on` value | Trigger | Matched against |
|------------|---------|-----------------|
| `open` | The connection is established | `path`, `queryParams`, `requestHeaders` of the upgrade request |
| `message` (default) | A text message arrives | the upgrade request fields, plus `requestBody` against the message payload |
| `close` | The connection closes | the upgrade request fields; responses cannot be sent |

### Responding when a connection opens

```yaml
plugin: websocket

resources:
  - path: /gateway
    on: open
    response:
      content: '{"event":"welcome","connectionId":"${random.uuid()}"}'
      template: true
```

### Matching messages

Messages are matched with the familiar `requestBody` syntax — including `jsonPath`, `xPath`, operators, and `allOf`/`anyOf` — applied to each incoming text message:

```yaml
resources:
  - path: /gateway
    requestBody:
      allOf:
        - jsonPath: $.type
          value: req
        - jsonPath: $.method
          value: connect
    capture:
      reqId:
        requestBody:
          jsonPath: $.id
    response:
      content: '{"type":"res","id":"${stores.request.reqId}","ok":true}'
      template: true
```

Values captured with [`capture`](./data_capture.md) go to the request store, which for WebSocket configurations is **scoped to the connection**: a value captured from one message can be used in responses to later messages on the same connection.

Messages that match no resource are logged and dropped; the connection stays open.

### Streaming multiple messages

To send an ordered sequence of messages, use `responses` (plural) instead of `response`. Each entry is a full response block with its own `content`/`file`, `delay` and `template` settings:

```yaml
resources:
  - path: /gateway
    requestBody:
      jsonPath: $.method
      value: agent
    responses:
      - content: '{"type":"res","ok":true}'
      - file: agent-event-1.json
        delay:
          exact: 250
      - file: agent-event-2.json
        delay:
          min: 100
          max: 400
```

A singular `response` block is exactly equivalent to a `responses` list with one element. A resource cannot declare both.

### Periodic messages (connection-scoped schedules)

An `on: open` resource can declare a `schedule` — periodic actions that run for as long as the connection is open:

```yaml
resources:
  - path: /gateway
    on: open
    response:
      content: '{"event":"welcome"}'
    schedule:
      - every: 15s
        response:
          content: '{"event":"tick","timestamp":"${datetime.now.millis}"}'
          template: true
```

Each schedule entry uses `every` (a duration such as `30s` or `5m`) or `cron` (a standard 5-field cron expression), and can send `response`/`responses` and/or run [`steps`](./steps.md). See [Scheduled tasks](./scheduled_tasks.md) for the shared schedule syntax.

### Acting on disconnect

An `on: close` resource runs when the client disconnects. It cannot send messages, but its `capture` and `steps` run — useful for notifying another system or recording state:

```yaml
resources:
  - path: /gateway
    on: close
    steps:
      - type: remote
        url: https://example.com/session-ended
        method: POST
```

## Complete example

This configuration simulates a JSON request/response-plus-events protocol: the server sends a challenge when a client connects, answers `connect` requests echoing the request ID, streams events in response to `agent` requests, and ticks every 15 seconds.

```yaml
plugin: websocket

resources:
  - path: /gateway
    on: open
    response:
      content: '{"type":"event","event":"connect.challenge","payload":{"nonce":"${random.uuid()}"}}'
      template: true
    schedule:
      - every: 15s
        response:
          content: '{"type":"event","event":"tick","payload":{"timestamp":"${datetime.now.millis}"}}'
          template: true

  - path: /gateway
    requestBody:
      allOf:
        - jsonPath: $.type
          value: req
        - jsonPath: $.method
          value: connect
    capture:
      reqId:
        requestBody:
          jsonPath: $.id
    response:
      file: hello-ok.json
      template: true

  - path: /gateway
    requestBody:
      jsonPath: $.method
      value: agent
    capture:
      reqId:
        requestBody:
          jsonPath: $.id
    responses:
      - content: '{"type":"res","id":"${stores.request.reqId}","ok":true}'
        template: true
      - file: agent-event-1.json
        delay:
          exact: 300
      - file: chat-event-final.json
        delay:
          exact: 400
```

Test it with a WebSocket client such as [websocat](https://github.com/vi/websocat):

    websocat ws://localhost:8080/gateway

## Limitations

- Only text (UTF-8) messages are matched; binary messages are ignored.
- WebSocket upgrades require HTTP/1.1; WebSocket over HTTP/2 (RFC 8441) is not supported.
- The plugin is not supported when running as an AWS Lambda function.
