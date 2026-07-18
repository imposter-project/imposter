# Run Imposter in CI/CD pipelines

If you're using a CI/CD platform other than GitHub Actions, you can still run Imposter to mock your dependencies during testing.

> [!NOTE]
> If you use GitHub Actions, there is a dedicated integration - see [Running in GitHub Actions](./github_actions.md).

<details markdown>
<summary>Other ways to run Imposter</summary>

**Standalone mock server**

- Using the command line client - see [Imposter CLI](./run_imposter_cli.md)
- As a Docker container - see [Imposter Docker container](./run_imposter_docker.md)
- As a Lambda function in AWS - see [Imposter AWS Lambda](./run_imposter_aws_lambda.md)
- As a JAR file on the JVM - see [Imposter JAR file](./run_imposter_jar.md)

**Embed in unit/integration tests**

- Embed within your **Java/Kotlin/Scala/JVM** unit tests - see [JVM bindings](./embed_jvm.md)
- Embed within your **JavaScript/Node.js** unit tests - see [JavaScript bindings](https://github.com/imposter-project/imposter-js)

</details>

## Approaches

There are two common ways to run Imposter in a pipeline:

1. **As a service container** - if your platform supports service/sidecar containers, run the [`outofcoffee/imposter`](https://hub.docker.com/r/outofcoffee/imposter) Docker image alongside your job. This is the simplest option.
2. **Via the CLI** - install the [Imposter CLI](./run_imposter_cli.md) and start the mocks in the background, then stop them when your tests finish.

In both cases, your mock configuration is mounted from a directory in your repository (e.g. `./mocks`). See the [configuration guide](./configuration.md) for details.

## GitLab CI/CD

Run Imposter as a [service](https://docs.gitlab.com/ee/ci/services/), mounting your config directory and waiting for the server to be ready:

```yaml
test:
  image: curlimages/curl:latest
  services:
    - name: outofcoffee/imposter:latest
      alias: imposter
      command: ["--configDir=/opt/imposter/config"]
  variables:
    # mount the repo's ./mocks directory into the service container
    IMPOSTER_BASE_URL: "http://imposter:8080"
  before_script:
    - until curl -sf "$IMPOSTER_BASE_URL/system/status"; do sleep 1; done
  script:
    - echo "Running tests against mock server at $IMPOSTER_BASE_URL"
```

> [!NOTE]
> GitLab service containers cannot bind-mount repository files directly. Either bake your config into a custom image (`FROM outofcoffee/imposter`, see [Docker](./run_imposter_docker.md)), or use the CLI approach below.

## CircleCI

Run Imposter as a secondary [Docker image](https://circleci.com/docs/configuration-reference/#docker), baking your config into a custom image, or use the CLI approach:

```yaml
jobs:
  test:
    docker:
      - image: cimg/base:current
      - image: outofcoffee/imposter:latest
        # use a custom image with your config baked in
        name: imposter
    steps:
      - checkout
      - run:
          name: Wait for mocks
          command: |
            until curl -sf http://localhost:8080/system/status; do sleep 1; done
      - run:
          name: Run tests
          command: echo "Mock server available at http://localhost:8080"
```

## Jenkins

Install the CLI and run mocks in the background using a declarative pipeline:

```groovy
pipeline {
  agent any
  stages {
    stage('Setup Imposter') {
      steps {
        sh 'curl -L https://raw.githubusercontent.com/imposter-project/imposter-cli/main/install/install_imposter.sh | bash -'
      }
    }
    stage('Start mocks') {
      steps {
        sh 'nohup imposter up ./mocks -p 8080 -t docker > imposter.log 2>&1 &'
        sh 'until curl -sf http://localhost:8080/system/status; do sleep 1; done'
      }
    }
    stage('Run tests') {
      steps {
        sh 'echo "Running tests against http://localhost:8080"'
      }
    }
  }
  post {
    always {
      sh 'imposter down -t docker || true'
    }
  }
}
```

## Azure Pipelines

Install the CLI and start mocks in the background:

```yaml
steps:
  - script: |
      curl -L https://raw.githubusercontent.com/imposter-project/imposter-cli/main/install/install_imposter.sh | bash -
    displayName: 'Setup Imposter'

  - script: |
      nohup imposter up ./mocks -p 8080 -t docker > imposter.log 2>&1 &
      until curl -sf http://localhost:8080/system/status; do sleep 1; done
    displayName: 'Start mocks'

  - script: echo "Running tests against http://localhost:8080"
    displayName: 'Run tests'

  - script: imposter down -t docker || true
    displayName: 'Stop mocks'
    condition: always()
```

## Bitbucket Pipelines

Run Imposter as a [service](https://support.atlassian.com/bitbucket-cloud/docs/use-services-and-databases-in-bitbucket-pipelines/) (using a custom image with your config baked in), or via the CLI:

```yaml
definitions:
  services:
    imposter:
      image: outofcoffee/imposter:latest
      # use a custom image with your config baked in - see the Docker guide

pipelines:
  default:
    - step:
        script:
          - until curl -sf http://localhost:8080/system/status; do sleep 1; done
          - echo "Running tests against http://localhost:8080"
        services:
          - imposter
```

## Other platforms

For any platform with a shell, the CLI approach works generally:

```bash
# install the CLI
curl -L https://raw.githubusercontent.com/imposter-project/imposter-cli/main/install/install_imposter.sh | bash -

# start mocks in the background
nohup imposter up ./mocks -p 8080 -t docker > imposter.log 2>&1 &

# wait until ready
until curl -sf http://localhost:8080/system/status; do sleep 1; done

# ... run your tests against http://localhost:8080 ...

# stop mocks
imposter down -t docker
```

The `/system/status` endpoint returns HTTP 200 once the mock server is ready - poll it to avoid race conditions.

## Further information

- [Imposter CLI](./run_imposter_cli.md)
- [Imposter Docker container](./run_imposter_docker.md)
- [Running in GitHub Actions](./github_actions.md)
- [Configuration guide](./configuration.md)
