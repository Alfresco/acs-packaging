#!/usr/bin/env bash
# Keeps the Maven cache bounded: only the upstream versions this build uses survive, older versions and SNAPSHOTs of the
# Alfresco artifacts that change on every version bump are removed.
set -e

source "$(dirname "${BASH_SOURCE[0]}")/build_functions.sh"

M2_ALFRESCO="${M2_ALFRESCO:-${HOME}/.m2/repository/org/alfresco}"

ENT_VERSION="$(retrievePomProperty "dependency.alfresco-enterprise-repo.version")"
SHARE_VERSION="$(retrievePomProperty "dependency.alfresco-enterprise-share.version")"
COM_VERSION="$(retrievePomProperty "dependency.alfresco-community-repo.version" "github.com/Alfresco/alfresco-enterprise-repo.git")"
KEEP=("${ENT_VERSION}" "${SHARE_VERSION}" "${COM_VERSION}")
echo "Keeping versions: ${KEEP[*]}"

ARTIFACT_PATTERNS=(
  "acs-community-packaging"
  "alfresco-community-repo*"
  "alfresco-core"
  "alfresco-data-model"
  "alfresco-enterprise-remote-api"
  "alfresco-enterprise-repo*"
  "alfresco-enterprise-repository"
  "alfresco-remote-api"
  "alfresco-repository"
  "content-services*"
  "share"
  "tas/alfresco-community-repo-*-test"
  "tas/alfresco-enterprise-repo-*-test"
)

shopt -s nullglob
for pattern in "${ARTIFACT_PATTERNS[@]}"; do
  for artifact_dir in "${M2_ALFRESCO}"/${pattern}/; do
    for version_dir in "${artifact_dir}"*/; do
      version="$(basename "${version_dir}")"
      if [[ ! " ${KEEP[*]} " =~ " ${version} " ]]; then
        rm -rf "${version_dir}"
      fi
    done
  done
done
