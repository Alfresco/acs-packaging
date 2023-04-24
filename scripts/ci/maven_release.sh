#!/usr/bin/env bash
echo "=========================== Starting Release Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex

BUILD_NUMBER=$1

pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

source "$(dirname "${BASH_SOURCE[0]}")/build_functions.sh"

if [ -z "${RELEASE_VERSION}" ] || [ -z "${DEVELOPMENT_VERSION}" ]; then
  echo "Please provide a Release and Development version in the format <acs-version>-<additional-info> (e.g. 7.2.0-A2)"
  exit 1
fi

# Use full history for release
git checkout -B "${BRANCH_NAME}"

mvn -B \
  -ntp \
  -Prelease,all-tas-tests,pipeline -Pags \
  -DreleaseVersion="${RELEASE_VERSION}" \
  -DdevelopmentVersion="${DEVELOPMENT_VERSION}" \
  "-Darguments=-Prelease,all-tas-tests,pipeline -Pags -DskipTests -Dbuild-number=${BUILD_NUMBER}" \
  release:clean release:prepare release:perform \
  -DscmCommentPrefix="[maven-release-plugin][skip ci] " \
  -Dusername="${GIT_USERNAME}" \
  -Dpassword="${GIT_PASSWORD}"



# The alfresco-content-services-share-distribution was in the Nexus 'Releases' repository prior to 7.1.0, which was visible to Community.
copyArtifactToAnotherRepo org.alfresco alfresco-content-services-share-distribution ${RELEASE_VERSION} zip \
 alfresco-enterprise-releases https://nexus.alfresco.com/nexus/content/repositories/releases/

popd
set +vex
echo "=========================== Finishing Release Script =========================="

