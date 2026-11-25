#!/usr/bin/env bash
# Hands the output of build.sh from the build job to the test jobs, so they don't each rebuild and re-download it:
#   mark              before build.sh: records the Maven repository and docker image state
#   save <dir>        after build.sh: writes what the build added to <dir>
#   restore <dir>     in a test job, after init.sh (which deletes SNAPSHOTs): loads <dir> back
set -euo pipefail

M2_REPO="${HOME}/.m2/repository"
MARKER="${RUNNER_TEMP:-/tmp}/build-outputs-marker"
IMAGES_BEFORE="${RUNNER_TEMP:-/tmp}/build-outputs-images-before"
TAS_SCRIPTS_DIR="alfresco-community-repo/packaging/tests/scripts"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
UPSTREAM_ROOT="$(dirname "${REPO_ROOT}")"

list_images() {
  docker images --format '{{.Repository}}:{{.Tag}}' | grep -v '<none>' | sort -u
}

mark() {
  mkdir -p "${M2_REPO}"
  touch "${MARKER}"
  list_images >| "${IMAGES_BEFORE}"
}

save() {
  local out="$1"
  mkdir -p "${out}"

  # Files the build downloaded or installed; anything restored from the cache is older than the marker.
  (cd "${M2_REPO}" && find . -type f -newer "${MARKER}" -print0 | tar -cf "${out}/m2.tar" --null -T -)

  local images
  images="$(comm -13 "${IMAGES_BEFORE}" <(list_images))"
  echo "Saving images:"
  echo "${images}"
  # One docker save for all images, so layers shared between them are stored once.
  # shellcheck disable=SC2086
  docker save ${images} | zstd -T0 -3 -q -o "${out}/images.tar.zst"

  tar -cf "${out}/tas-scripts.tar" -C "${UPSTREAM_ROOT}" "${TAS_SCRIPTS_DIR}"

  ls -lh "${out}"
}

restore() {
  local in="$1"
  mkdir -p "${M2_REPO}"
  tar -xf "${in}/m2.tar" -C "${M2_REPO}"
  zstd -dc -q "${in}/images.tar.zst" | docker load
  tar -xf "${in}/tas-scripts.tar" -C "${UPSTREAM_ROOT}"
}

case "${1:-}" in
  mark) mark ;;
  save) save "${2:?output directory required}" ;;
  restore) restore "${2:?input directory required}" ;;
  *) echo "Usage: $0 mark | save <dir> | restore <dir>" >&2; exit 1 ;;
esac
