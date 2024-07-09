#!/usr/bin/env bash
echo "=========================== Starting Publish Script ==========================="
# Republishes the maven artifacts in the Enterprise Releases repository.

PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

source "$(dirname "${BASH_SOURCE[0]}")/build_functions.sh"

if [ -z "${RELEASE_VERSION}" ]; then
  echo "Please provide a Release version in the format <acs-version>-<additional-info> (e.g. 7.2.0-A2)"
  exit 1
fi

# Checkout the tag.
git checkout "${RELEASE_VERSION}"

# Rebuild the artifacts and publish them to enterprise-releases.
mvn -B \
  -ntp \
  -Ppublish,all-tas-tests,pipeline,ags \
  -DaltDeploymentRepository=alfresco-enterprise-releases::default::https://nexus3.alfresco.com/nexus/content/repositories/enterprise-releases \
  -DskipTests \
  clean deploy

popd
set +vex
echo "=========================== Finishing Release Script =========================="


