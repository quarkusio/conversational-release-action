
# Conversational Release Action

> Release Quarkus by conversing with a GitHub Action

<p align="center"><img src="https://design.jboss.org/quarkus/bot/final/images/quarkusbot_full.svg" width="128" height="128" /></p>

This GitHub Action is used to release [Quarkus](https://github.com/quarkusio/quarkus) and the [Quarkus Platform](https://github.com/quarkusio/quarkus-platform/).
It is developed in Quarkus using the [Quarkus GitHub Action](https://github.com/quarkiverse/quarkus-github-action/) extension.

It is used as part of the GitHub Actions workflows hosted in the [release project](https://github.com/quarkus-release/release).

This action is composed of several subactions.
The subactions are all part of the same action and uses the `action` name feature of [Quarkus GitHub Action](https://github.com/quarkiverse/quarkus-github-action/).

The subactions are designed to be carefully orchestrated by a carefully crafted workflow.

> [!WARNING]
> This action is automatically published when the `main` branch is pushed.

## `looking`

This action is extremely basic and adds a `EYES` reaction to the issues and issue comments.
It provides visual feedback that the workflow has started.

### Example

```yaml
- name: Provide visual feedback
  uses: quarkusio/conversational-release-action@main
  with:
    github-token: ${{ secrets.GITHUB_TOKEN }}
    action: looking
```

### Inputs

No inputs.

### Outputs

No outputs.

## `get-release-information`

Extract the release information from the issue description.

The release information is included as a comment in the issue description.

### Example

```yaml
- name: Get release information
  id: get-release-information
  uses: quarkusio/conversational-release-action@main
  with:
    github-token: ${{ secrets.GITHUB_TOKEN }}
    action: get-release-information
```

### Inputs

No inputs.

### Outputs

| Name   | Description  |
|---|---|
| `branch`  | The branch to release  |
| `qualifier`  | The qualifier (e.g. `Alpha1`, `CR1`)  |
| `major`  | If the release is a major new release  |
| `version`  | The version to release, once it has been determined  |
| `jdk`  | The JDK version to release with (e.g. `11`, `17`)  |

## `get-release-status`

Extract the release status from the issue description.

The release status is included as a comment in the issue description.

### Example

```yaml
- name: Get release status
  id: get-release-status
  uses: quarkusio/conversational-release-action@main
  with:
    github-token: ${{ secrets.GITHUB_TOKEN }}
    action: get-release-status
```

### Inputs

No inputs.

### Outputs

| Name   | Description  |
|---|---|
| `status`  | The global status of the release  |
| `current-step`  | The current step  |
| `current-step-status`  | The status of the current step  |
| ` workflow-run-id`  | The current/previous workflow run id |

## The default action

Actually handle the release.
The release is separated as steps.
Steps can be paused to wait for user input.
Most steps are recoverable and can be retried in case of an error.

The steps are using the [original scripts used to release Quarkus manually](https://github.com/quarkusio/quarkus-release).

The workflow will be executed several times and is able to continue from the previous steps until completion.
In case of an error, it will report the error to the release issue.

At the end of each workflow execution, the `work` directory is uploaded as an artifact.
In the very worst case, it can be downloaded to finalize the release manually.

### Example

```yaml
- name: Start release
  id: release
  uses: quarkusio/conversational-release-action@main
  with:
    github-token: ${{ secrets.GITHUB_TOKEN }}
  env:
    MAVEN_OPTS: -Xmx3g
    GPG_PASSPHRASE: ${{ secrets.MAVEN_GPG_PRIVATE_PASSPHRASE }}
    RELEASE_GITHUB_TOKEN: ${{ secrets.RELEASE_GITHUB_TOKEN }}
    JRELEASER_GITHUB_TOKEN: ${{ secrets.JRELEASER_GITHUB_TOKEN }}
    JRELEASER_CHOCOLATEY_GITHUB_TOKEN: ${{ secrets.JRELEASER_CHOCOLATEY_GITHUB_TOKEN }}
    JRELEASER_CHOCOLATEY_GITHUB_USERNAME: ${{ secrets.JRELEASER_CHOCOLATEY_GITHUB_USERNAME }}
    JRELEASER_HOMEBREW_GITHUB_TOKEN: ${{ secrets.JRELEASER_HOMEBREW_GITHUB_TOKEN }}
    JRELEASER_HOMEBREW_GITHUB_USERNAME: ${{ secrets.JRELEASER_HOMEBREW_GITHUB_USERNAME }}
    JRELEASER_SDKMAN_CONSUMER_KEY: ${{ secrets.JRELEASER_SDKMAN_CONSUMER_KEY }}
    JRELEASER_SDKMAN_CONSUMER_TOKEN: ${{ secrets.JRELEASER_SDKMAN_CONSUMER_TOKEN }}
```

### Inputs

No inputs **BUT** environment variables need to be set to provide the secrets for the various scripts used to release.
The environment variable names are self explanatory.

### Outputs

| Name   | Description  |
|---|---|
| `interaction-comment`  | A comment that will be posted at the very end of the workflow execution. This is useful because in most cases, you need the `work` directory uploaded before the user can post a comment to continue with the workflow  |

## `post-interaction-comment`

Post a comment at the very end of the workflow.

### Example

```yaml
- name: Post interaction comment
  uses: quarkusio/conversational-release-action@main
  if: always()
  with:
    github-token: ${{ secrets.GITHUB_TOKEN }}
    action: post-interaction-comment
    interaction-comment: ${{ steps.release.outputs.interaction-comment }}
```

### Inputs

| Name   | Description  |
|---|---|
| `interaction-comment`  | A comment that will be posted at the very end of the workflow execution. This is useful because in most cases, you need the `work` directory uploaded before the user can post a comment to continue with the workflow  |

### Outputs

No outputs.
