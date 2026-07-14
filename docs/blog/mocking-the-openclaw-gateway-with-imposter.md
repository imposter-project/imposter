# Mocking the OpenClaw Gateway with Imposter

A lot of the interesting work around AI agents right now is in the tooling: the operator consoles, CLIs, dashboards and background workers that drive agents, manage their sessions and stream results back to a human. [OpenClaw](https://docs.openclaw.ai) sits at the centre of a lot of this. Its gateway is the control plane between operator clients and the nodes that run the work, and it all happens over a single WebSocket connection.

Building a client for that gateway is where it gets awkward. A live gateway means a live agent behind it: responses aren't deterministic, you need real nodes paired to get anywhere, runs are slow, and each one costs tokens. Pinning down a specific sequence (a tool call, then a streamed reply, then a reconnect) so you can build against it or assert on it in CI is most of the battle.

That's the same reason you'd mock a REST or SOAP API, and Imposter now handles it. This post shows how to mock the OpenClaw gateway's WebSocket API using [Imposter](https://www.imposter.sh).

## The gateway protocol, briefly

The gateway speaks JSON frames over one WebSocket. Three kinds:

* requests: `{"type":"req","id":"…","method":"…","params":{…}}`
* responses: `{"type":"res","id":"…","ok":true,"payload":{…}}`
* events: `{"type":"event","event":"…","payload":{…}}`, pushed by the server

A client connects and gets a `connect.challenge` event, sends a `connect` request, and receives `hello-ok`. After that it lists agents and sessions, then sends a `chat.send`. Instead of one reply it gets an acknowledgement, followed by a stream of `agent` and `chat` events as the run plays out. A `tick` event fires periodically to keep the connection alive.

Request/response is easy enough to fake. The streams and the periodic pushes are the parts that trip up a traditional mock, and they're the parts an agent client leans on most.

## A WebSocket mock

Imposter 5.x has a built-in `websocket` plugin. A resource says when to act (on open, on a matching message, or on close) and what to send back. The matching, capture and [templating](https://docs.imposter.sh/templates/) are the same ones you'd use in a REST mock.

Start with the connection itself. On open, send the challenge and kick off the keepalive:

```yaml
# imposter-config.yaml
plugin: websocket

resources:
  - path: /*
    on: open
    response:
      content: '{"type":"event","event":"connect.challenge","payload":{"nonce":"${random.uuid()}","ts":${datetime.now.millis}}}'
      template: true
    schedule:
      - every: 15s
        response:
          content: '{"type":"event","event":"tick","payload":{"timestamp":${datetime.now.millis}}}'
          template: true
```

The `path: /*` accepts a connection on any path, which is how the real gateway behaves: it multiplexes on one port and upgrades on the `Upgrade: websocket` header, not a fixed URL. `${random.uuid()}` and `${datetime.now.millis}` are filled in per connection, so the nonce and timestamps look live.

### Answering a request

Match incoming messages on the JSON-RPC `method`, and capture the request `id` so you can echo it back:

```yaml
  - path: /*
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
      content: '{"type":"res","id":"${stores.request.reqId}","ok":true,"payload":{"type":"hello-ok","protocol":4}}'
      template: true
```

One resource per method (`agents.list`, `sessions.list`, `sessions.create`, `models.list`) and the mock will carry a client through its whole startup handshake to a working chat screen.

### Streaming a reply

A single `chat.send` kicks off a whole run's worth of events, spread out over time. Imposter models that with a `responses` list, each entry a full response with its own delay:

```yaml
  - path: /*
    requestBody:
      allOf:
        - jsonPath: $.type
          value: req
        - jsonPath: $.method
          value: chat.send
    capture:
      reqId:
        requestBody: { jsonPath: $.id }
    responses:
      - content: '{"type":"res","id":"${stores.request.reqId}","ok":true,"payload":{"runId":"${stores.request.reqId}","status":"started"}}'
        template: true
      - file: agent-tool-start.json
        delay: { exact: 300 }
        template: true
      - file: agent-tool-result.json
        delay: { exact: 300 }
        template: true
      - file: chat-event-delta.json
        delay: { min: 100, max: 400 }
        template: true
      - file: chat-event-final.json
        delay: { exact: 400 }
        template: true
```

The frames go out in order with real gaps between them: a tool call, its result, a couple of chat deltas, then the final message. Your streaming UI gets a proper workout, and it does the same thing every time. No model, no tokens.

The `tick` from the open resource, meanwhile, keeps firing on its 15-second [schedule](https://docs.imposter.sh/scheduled_tasks/) for as long as the connection is open. Schedules aren't specific to WebSocket. The same feature drives cron- or interval-based HTTP calls, which is handy for webhook-style pushes, but a keepalive is a natural fit too.

## Starting the mock

The plugin is built in, so there's nothing to install. With the [Imposter CLI](https://docs.imposter.sh/run_imposter_cli/):

```bash
imposter up ./websocket/openclaw
```

It listens on `http://localhost:8080` and takes a WebSocket connection on any path.

## Trying it out

Any client works. Here's [websocat](https://github.com/vi/websocat):

```bash
websocat ws://localhost:8080/ws
```

The `connect.challenge` arrives straight away. Send a handshake:

```json
{"type":"req","id":"req-1","method":"connect","params":{}}
```

and `hello-ok` comes back. Send a `chat.send` and watch the acknowledgement turn into a stream of tool and chat events, with a `tick` every 15 seconds.

## Building against it: Lucinate

[Lucinate](https://lucinate.ai) is an operator client for OpenClaw agents: it connects to a gateway, browses agents and sessions, and runs a live chat. A mock gateway covers exactly that flow. You can build the connect-list-open-chat path, drive the streaming UI frame by frame, and run it in CI, none of it needing a paired node or a single token. Point the finished client at a real gateway afterwards and nothing about the wire protocol changes.

## Summary

The `websocket` plugin brings Imposter's matching, capture and templating to a bidirectional, streaming protocol. The `responses` list handles the streamed events, and connection-scoped schedules handle the periodic pushes. Between them they cover the request/response-and-events shape that agent gateways like OpenClaw use. Config changes need a restart (`CTRL+C` to stop).

For the full picture and a complete, runnable OpenClaw example, see the [WebSocket plugin docs](https://docs.imposter.sh/websocket_plugin/), the [scheduled tasks docs](https://docs.imposter.sh/scheduled_tasks/), and the [`websocket/openclaw`](https://github.com/imposter-project/examples/tree/main/websocket/openclaw) directory in the [examples repository](https://github.com/imposter-project/examples).
