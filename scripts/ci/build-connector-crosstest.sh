#!/usr/bin/env bash
# =============================================================================
# TEMPORARY - cross-testing helper (NOT for merge).
#
# Builds the Alfresco Elasticsearch Connector Docker images locally from a
# feature branch instead of pulling a published tag from quay.io/docker.io.
#
# Activated only when CROSSTEST_CONNECTOR_REF is set (e.g. in the workflow env),
# so normal CI runs are completely unaffected.
#
# On success it prints the connector project version on stdout; callers should
# use that value as LIVE_INDEXING_TAG / ES_CONNECTOR_TAG / REINDEXING_TAG so the
# locally-built images are used (docker/compose never pull when the image with
# that exact name:tag already exists locally).
# =============================================================================
set -euo pipefail

CONNECTOR_REF="${CROSSTEST_CONNECTOR_REF:-}"
if [ -z "${CONNECTOR_REF}" ]; then
  # Cross-testing not requested - nothing to do.
  exit 0
fi

CONNECTOR_REPO="${CROSSTEST_CONNECTOR_REPO:-github.com/Alfresco/alfresco-elasticsearch-connector.git}"
CONNECTOR_DIR="${CROSSTEST_CONNECTOR_DIR:-${RUNNER_TEMP:-/tmp}/alfresco-elasticsearch-connector-crosstest}"

# Credentials: prefer the broad BOT credentials (GIT_USERNAME/GIT_PASSWORD),
# fall back to AUTH if those are not available.
if [ -n "${CROSSTEST_CONNECTOR_TOKEN:-}" ]; then
  CLONE_URL="https://x-access-token:${CROSSTEST_CONNECTOR_TOKEN}@${CONNECTOR_REPO}"
elif [ -n "${GIT_USERNAME:-}" ] && [ -n "${GIT_PASSWORD:-}" ]; then
  CLONE_URL="https://${GIT_USERNAME}:${GIT_PASSWORD}@${CONNECTOR_REPO}"
else
  CLONE_URL="https://${AUTH:-}${CONNECTOR_REPO}"
fi

echo "[crosstest] Building ES connector images from ${CONNECTOR_REPO}@${CONNECTOR_REF}" 1>&2

rm -rf "${CONNECTOR_DIR}"
git clone -b "${CONNECTOR_REF}" --depth=1 "${CLONE_URL}" "${CONNECTOR_DIR}" 1>&2

pushd "${CONNECTOR_DIR}" >/dev/null
  echo "[crosstest] connector HEAD: $(git rev-parse HEAD) on ${CONNECTOR_REF}" 1>&2
  CONNECTOR_VERSION="$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)"
  # Build the application jars, then the connector Docker images. buildDockerImagesCi.sh
  # tags each image as <repository>:<connector-version> in the local Docker daemon.
  mvn -B -ntp -V -q clean install -DskipTests -Dmaven.javadoc.skip=true -Pdocker-image 1>&2
  bash scripts/ci/buildDockerImagesCi.sh 1>&2
popd >/dev/null

echo "[crosstest] Built connector images tagged :${CONNECTOR_VERSION}" 1>&2

# Only the connector version is written to stdout (for command substitution).
echo "${CONNECTOR_VERSION}"
