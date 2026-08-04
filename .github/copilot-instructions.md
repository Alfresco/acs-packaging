# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

acs-packaging produces the packaging for Alfresco Content Services Enterprise: the
`alfresco/alfresco-content-repository` and `alfresco/alfresco-share` Docker images, and the distribution zips for
the product. It is a multi-module Maven reactor build (Java, Tomcat-based).

- `alfresco/alfresco-content-repository` extends the `alfresco-enterprise-repo-base` image built by the
  `alfresco-enterprise-repo` project.
- `alfresco/alfresco-share` extends the `alfresco-share-base` image built by the `alfresco-enterprise-share` project.
- This is the Enterprise counterpart of [acs-community-packaging](https://github.com/Alfresco/acs-community-packaging).

Building this repo alone only produces packaging artifacts; producing full Docker images or running the repo/share
webapps requires the upstream `alfresco-community-repo`, `alfresco-enterprise-repo`, and `alfresco-enterprise-share`
projects checked out as siblings (see Development workflow below).

## Repository layout

- `distribution/`, `distribution-share/`, `distribution-ags/` — assemble the repo/Share/AGS distribution zips.
- `docker-alfresco/`, `docker-share/` — build the repository and Share Docker images (`aws/` and `ags/` submodules
  produce AWS- and Governance Services-flavoured variants).
- `dev/` — local Tomcat development environment (see `dev/README.md`) and shell aliases (`dev/aliases`).
- `tests/` — TAS (Test Automation Suite) integration test modules, one per protocol/feature area (`tas-cmis`,
  `tas-webdav`, `tas-restapi`, `tas-elasticsearch`, `tas-email`, `tas-mtls`, `tas-sync-service`,
  `tas-distribution-zip`, `tas-all-amps`, `tas-integration`), plus `tas-*` shared environment/testcontainers helpers.
- `scripts/ci/` — CI build/release scripts invoked from GitHub Actions workflows.
- `scripts/dev/` — developer helper scripts, notably `linkPoms.sh`/`unlinkPoms.sh` for wiring SNAPSHOT versions
  between the sibling upstream projects during local development.
- `docs/` — supplementary docs (custom transforms/renditions, custom Docker images, T-Engines, DAU, etc.).

Maven profiles select which modules build: `ags` (Governance Services), `all-tas-tests` (adds `tests`), `pipeline`
(`tests/pipeline-all-amps`), `run`/`release` (adds `dev`), `build-docker-images`.

## Build commands

```sh
mvn clean install                                  # build distribution zips (no Docker images)
mvn clean install -Pbuild-docker-images -Dimage.tag=latest   # also build Docker images
mvn clean install -Pags                            # include Alfresco Governance Services (AGS)
mvn clean install -Pall-tas-tests                  # include the TAS integration test modules
```

Run a single TAS test class from its module, e.g.:

```sh
cd tests/tas-cmis && mvn test -Dtest=SomeTestClassName
```

## Local development environment

Full local development (running `alfresco.war`/`share.war` in Tomcat against your own code changes) requires the
sibling repos and the `dev/aliases` shell helpers — see `dev/README.md` for the full walkthrough. Summary:

```sh
# clone siblings next to this repo
git clone git@github.com:Alfresco/alfresco-community-repo.git
git clone git@github.com:Alfresco/alfresco-enterprise-repo.git
git clone git@github.com:Alfresco/alfresco-enterprise-share.git

source acs-packaging/dev/aliases   # exposes comR/entR/entS/entP... build aliases, see file header for full list
sh acs-packaging/scripts/dev/linkPoms.sh   # point downstream poms at the SNAPSHOT versions of the above

entR                                # build alfresco-enterprise-repo
entS                                # build alfresco-enterprise-share
docker compose -f dev/docker-compose.yml up   # or the `envUp` alias: db, ActiveMQ, Solr, transformers
entT                                 # mvn clean install -Prun -rf dev — starts Tomcat with repo+Share
```

Use `entO`/`entODebug` to restart Tomcat reusing the existing DB/`alf_data`, and `unlinkPoms.sh` to revert the pom
changes made by `linkPoms.sh`.

## CI/CD

- `.github/workflows/ci.yml` is the main workflow (build, pre-commit, PMD scan, TAS test suites, ARM64 variants) and
  runs on PRs/pushes to `feature/**`, `fix/**`, `master`, `release/**`.
- `.github/workflows/master_release.yml` handles publishing on `master` (Maven artifacts, Docker images, AWS
  releases, downstream triggers) — gated on commit message markers such as `[publish]`, `[skip tests]`,
  `[no downstream]`.
- `.github/workflows/arm64.yml` builds/tests the ARM64 image variant.
- Commit message markers (parsed by `Alfresco/alfresco-build-tools/.github/actions/get-commit-message`) control
  pipeline behaviour — check `commit_parser`/`if:` conditions in the workflows before assuming a job always runs.
