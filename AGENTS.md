# AGENTS.md

This file provides guidance to Claude Code (claude.ai/code) and other AI coding agents when working with code in this repository.

## What this project is

`acs-packaging` assembles the Alfresco Content Services Enterprise product from upstream artifacts. It does not contain repository/business logic itself — it pulls in prebuilt WARs/AMPs from `alfresco-enterprise-repo` and `alfresco-enterprise-share` (declared as Maven dependencies, versions pinned in the root `pom.xml`), then produces:

- Distribution zips (`distribution`, `distribution-share`, `distribution-ags`)
- Docker images `alfresco/alfresco-content-repository` and `alfresco/alfresco-share` (`docker-alfresco`, `docker-share`, plus `aws`/`ags` variants)
- TAS (Test Automation Suite) integration test jars (`tests/*`)
- A local dev Tomcat environment (`dev/`)

It's the enterprise counterpart of [acs-community-packaging](https://github.com/Alfresco/acs-community-packaging), and is normally developed alongside `alfresco-community-repo`, `alfresco-enterprise-repo`, and `alfresco-enterprise-share` cloned as sibling directories (see `dev/README.md`).

## Build

Root reactor build (distribution zips + docker modules, no AGS, no docker images):
```
mvn clean install
```

Key profiles (combine as needed):
- `-Pags` — include Alfresco Governance Services (Records Management) modules (`distribution-ags`, AGS docker variants)
- `-Pall-tas-tests` — activate the `tests` module (TAS test jars)
- `-Pbuild-docker-images` / `-Ppush-docker-images` — build/push docker images (used by the `dev` module's docker profiles too, see `dev/aliases`)
- `-Prun` / `-Prun,withShare` — build and start a local Tomcat via the `dev` module
- `-Prelease` / `-Ppublish` — release-related, also activate `dev`
- `-Ppipeline` — activate `tests/pipeline-all-amps`

Format/license headers use spotless, enforced by pre-commit (`.pre-commit-config.yaml`) rather than a plain `mvn` phase:
```
mvn spotless:apply validate -DlicenseUpdateHeaders=true -Pags,all-tas-tests
```

## Local dev environment

Source `dev/aliases` for shorthand commands (`entR`, `entS`, `entP`, `entT`, `entO`, `envUp`, ...). The general workflow, detailed in `dev/README.md`:

1. Clone `alfresco-community-repo`, `alfresco-enterprise-repo`, `alfresco-enterprise-share`, `acs-packaging`, `acs-community-packaging` as siblings.
2. Link them to build against each other's SNAPSHOT versions: `sh acs-packaging/scripts/dev/linkPoms.sh` (undo with `unlinkPoms.sh`).
3. Build upstream repo/share projects (`comR`, `entR`, `entS` aliases), skipping tests.
4. Start supporting services: `docker compose -f dev/docker-compose.yml up` (or the `envUp` alias) — provides Postgres, Solr, ActiveMQ, transform services, shared-file-store.
5. Start Tomcat with the repo/Share AMPs applied: `mvn clean install -Prun -rf dev` (`entT` alias); restart without full rebuild via `mvn install -Prun,withShare -rf dev-acs-amps-overlay` (`entO`).

## Tests

`tests/` is a Maven module (only built with `-Pall-tas-tests`) containing one submodule per protocol/suite: `tas-cmis`, `tas-restapi`, `tas-webdav`, `tas-email`, `tas-integration`, `tas-sync-service`, `tas-elasticsearch`, `tas-all-amps`, `tas-distribution-zip`, `tas-mtls`, plus `testcontainers-env` and `pipeline-all-amps`.

Each TAS module has its own profiles selecting the binding/environment under test, e.g. in `tas-cmis`: `run-cmis-browser`, `run-cmis-webservices`, `run-cmis-atom`, `run-cmis-with-elastic` (and `-with-aims` variants for identity-service auth). These set things like `excludedGroups` (TestNG group exclusions, e.g. `bug-browser.*`) and the CMIS binding path.

TAS tests run against a live stack started via docker-compose files in `tests/environment/*.yml`, brought up with `${TAS_SCRIPTS}/start-compose.sh <compose-file>` (`TAS_SCRIPTS` points at a script living in the sibling `alfresco-community-repo` checkout — see `ci.yml`). Once the environment is up, run a suite directly, e.g.:
```
mvn -B install -f tests/tas-cmis/pom.xml -Pall-tas-tests,run-cmis-browser -Denvironment=default
```

CI (`.github/workflows/ci.yml`) runs these as a matrix (`tas_tests`, `tas_tests_with_aims`, `cmis_tas_tests_elasticsearch`, `cmis_tas_tests_opensearch`, `tas_tests_search_api`, `upgrade_tas_tests`, `all_amps_tests`, `tas_test_with_mtls`, `distribution_zip_content_tests`, `single_pipeline_image_tests`, `test_tomcat_deployment`), plus a `pmd_scan` job and a `precommit` job (detect-secrets + spotless).

## Architecture notes

- Root `pom.xml` is a reactor POM whose parent is `alfresco-enterprise-repo`; upstream artifact versions (`dependency.alfresco-enterprise-repo.version`, `dependency.alfresco-enterprise-share.version`, plus various connector/integration AMP versions) are pinned as properties there — bumping them is a routine, mechanical change (see recent commit history).
- `docker-alfresco` extends the `alfresco-enterprise-repo-base` image (built upstream) and installs additional AMPs (share-services, device-sync) via the `alfresco-mmt` tool; `docker-share` extends `alfresco-share-base` similarly. `aws` subfolders build variants for AWS Marketplace; `ags` subfolders add Governance Services.
- `.github/release-versions.yml` holds the current `RELEASE_VERSION`/`DEVELOPMENT_VERSION` used by `master_release.yml` for cutting releases — release version must start with the real product version for docker image builds to succeed.
- Cross-repo scripts live in `scripts/dev/` (`linkPoms.sh`/`unlinkPoms.sh` for local multi-repo linking, `checkout.sh`, `newReleaseBranch.sh`/`.py`) and `scripts/ci/` (staging deploy, cache cleanup, downstream update, Jira integration under `scripts/ci/jira/`).
- Further guides live under `docs/`: custom Docker images, custom transforms/renditions, T-Engine creation, metadata extract/embed, direct access URLs, query accelerator, transform services, legacy transformer migration.
- The currently active/supported release branches are `release/23.N`, `release/25.N`, and `release/26.N`. Any other `release/*` branch is an unsupported ACS version.
