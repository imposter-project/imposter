# Project configuration file

When you run mocks with the [Imposter CLI](./run_imposter_cli.md), you can add a project configuration file to your mock directory to control how that project runs — for example, pinning the Imposter engine version, listing the plugins it needs, and setting environment variables. Because the file lives alongside your mocks, you can commit it to version control so everyone on your team runs the mocks the same way.

> **Note**
> This file is separate from your [mock configuration files](./configuration.md) (the `-config.yaml` files that define responses). It configures the engine version, plugins and environment variables, not the mock behaviour.

## File name

Name the file `imposter-project.yaml` (a `.yml` or `.json` extension also works) and place it in your mock directory:

```
imposter-project.yaml
```

> **Tip**
> Running [`imposter scaffold`](./scaffold.md) creates this file for you.

## Example

```yaml
# pin the engine version so everyone runs the same build
version: "3.2.1"

# install the plugins this project needs
plugins:
  - store-dynamodb

# set environment variables when the mocks run
env:
  IMPOSTER_LOG_LEVEL: DEBUG
```

See the [environment variables](./environment_variables.md) reference for values you can set under `env`.

## How settings are combined

Imposter reads settings from several places. Where the same setting appears more than once, the later source wins:

1. Your global CLI configuration (`$HOME/.imposter/config.yaml`), if it exists
2. The project configuration file in your mock directory, if it exists
3. Environment variables
4. Command line flags

So a value in `imposter-project.yaml` overrides your global default, and a command line flag overrides the file.

## Backward compatibility

Earlier versions used a hidden file named `.imposter.yaml`. That name still works, but is deprecated: when Imposter finds one it prints a warning asking you to rename it to `imposter-project.yaml`. If both files are present, `imposter-project.yaml` takes precedence.

## What's next

- Generate a mock and project file with [scaffolding](./scaffold.md)
- Set defaults that apply to every project using [environment variables](./environment_variables.md)
