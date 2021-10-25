#!/usr/bin/env bash
echo "=========================== Starting Publish Script ==========================="
# Downloads the distribution zips from the Nexus Internal Releases repository and then uploads them to the Enterprise
# Releases repository

PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

source "$(dirname "${BASH_SOURCE[0]}")/build_functions.sh"


if [ -z "${RELEASE_VERSION}" ]; then
  echo "Please provide a Release version in the format <acs-version>-<additional-info> (7.2.0-EA or 7.2.0)"
  exit 1
fi

copyArtifactToAnotherRepo org.alfresco alfresco-content-services-distribution               ${RELEASE_VERSION} zip \
    alfresco-enterprise-releases https://nexus.alfresco.com/nexus/content/repositories/enterprise-releases/
copyArtifactToAnotherRepo org.alfresco alfresco-governance-services-enterprise-distribution ${RELEASE_VERSION} zip \
    alfresco-enterprise-releases https://nexus.alfresco.com/nexus/content/repositories/enterprise-releases/

popd
set +vex
echo "=========================== Finishing Release Script =========================="


