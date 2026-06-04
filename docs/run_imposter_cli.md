# Running Imposter with the CLI

There are many ways to run Imposter. This section describes using the command line interface (CLI) tool.

<details markdown>
<summary>Other ways to run Imposter</summary>

**Standalone mock server**

- As a Lambda function in AWS - see [Imposter AWS Lambda](./run_imposter_aws_lambda.md)
- As a Docker container - see [Imposter Docker container](./run_imposter_docker.md)
- As a JAR file on the JVM - see [Imposter JAR file](./run_imposter_jar.md)

**Embedded in tests**

- Embedded within your **Java/Kotlin/Scala/JVM** unit tests - see [JVM bindings](./embed_jvm.md)
- Embedded within your **JavaScript/Node.js** unit tests - see [JavaScript bindings](https://github.com/imposter-project/imposter-js)

**Within your CI/CD pipeline**

- Use the [Imposter GitHub Actions](./github_actions.md) to start and stop Imposter during your CI/CD pipeline.
- For other CI/CD platforms (GitLab, CircleCI, Jenkins, Azure Pipelines, Bitbucket, etc.) - see [Running in CI/CD pipelines](./ci_cd.md).

</details>

## CLI Features

- Start mocks (`imposter up`)
- Generate mock configuration from OpenAPI and WSDL files (`imposter scaffold`)
- Supports all [plugins](./plugins.md)
- Supports native binary, Docker and JVM engine types
- Supports both 'core' and 'all' distributions

## Installation

### Prerequisites

The CLI itself has no dependencies. Additional prerequisites depend on the [engine type](#engine-types) you choose to run Imposter with:

- **Native binary** (default from Imposter 5.x onwards) — no dependencies.
- **Docker** — requires [Docker](https://docs.docker.com/get-docker/) to be installed.
- **JVM** — requires a JVM to be installed. The JVM engine is only available for Imposter 4.x and earlier.

### Homebrew

If you have Homebrew installed:

    brew tap imposter-project/imposter
    brew install imposter

<details markdown>
<summary>Homebrew installation troubleshooting</summary>

If you previously installed Imposter using Homebrew from the deprecated tap `gatehill/imposter`, you may need to run the following command to update your Homebrew installation:

```shell
brew untap gatehill/imposter
brew tap imposter-project/imposter
```

</details>

### Shell script

Or, use this one liner (macOS and Linux only):

```shell
curl -L https://raw.githubusercontent.com/imposter-project/imposter-cli/main/install/install_imposter.sh | bash -
```

### Other installation options

See the full [Installation](https://github.com/imposter-project/imposter-cli/blob/main/docs/install.md) instructions for your system.

## Example

```shell
$ cd /path/to/config
$ imposter up

Starting server on port 8080...
Parsing configuration file: someapi-config.yaml
...
Mock server is up and running
```

## Engine types

The CLI can run Imposter using different engines. Choose the one that suits your environment:

| Engine type     | Flag value      | Prerequisites | Notes                                                  |
|-----------------|-----------------|---------------|--------------------------------------------------------|
| Native binary   | `native`        | None          | Default from Imposter 5.x onwards. No dependencies.    |
| Docker          | `docker`        | Docker        | Runs Imposter inside a Docker container.               |
| JVM             | `jvm`           | JVM           | Imposter 4.x and earlier only.                         |

Use the `-t` (engine type) flag to choose the engine:

```shell
$ imposter up -t docker
```

## Different distributions

The previous command starts Imposter using the 'core' distribution, which includes common [plugins](./plugins.md) only. To use the 'all' distribution, which includes all plugins, append `-all` to the engine type:

```shell
$ imposter up -t docker-all
```

## CLI usage

See full usage instructions on [Imposter CLI](https://github.com/imposter-project/imposter-cli).

---

## What's next

- Learn how to use Imposter with the [Configuration guide](configuration.md).
