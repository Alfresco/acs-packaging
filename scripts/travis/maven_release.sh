#!/usr/bin/env bash
echo "=========================== Starting Release Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

source "$(dirname "${BASH_SOURCE[0]}")/build_functions.sh"

if [ -z "${RELEASE_VERSION}" ] || [ -z "${DEVELOPMENT_VERSION}" ]; then
  echo "Please provide a Release and Development version in the format <acs-version>-<additional-info> (7.1.2-EA or 7.1.2-SNAPSHOT)"
  exit 1
fi

# Use full history for release
git checkout -B "${TRAVIS_BRANCH}"
# Add email to link commits to user
git config user.email "${GIT_EMAIL}"

mvn -B \
  -ntp \
  -Prelease,all-tas-tests,pipeline -Pags \
  -DreleaseVersion="${RELEASE_VERSION}" \
  -DdevelopmentVersion="${DEVELOPMENT_VERSION}" \
  "-Darguments=-Prelease,all-tas-tests,pipeline -Pags -DskipTests -Dbuild-number=${TRAVIS_BUILD_NUMBER}" \
  release:clean release:prepare release:perform \
  -DscmCommentPrefix="[maven-release-plugin][skip ci] " \
  -Dusername="${GIT_USERNAME}" \
  -Dpassword="${GIT_PASSWORD}" \
2>&1 | grep -v 'original line endings' | grep -v 'replaced by CRLF'
# We ended up with +23,000 warning messages about the wrong line endings because of updates of the copyright year in RM
# files and the Travis logs filled up and terminated. The above greps remove them, while we look for a better solution.


# The alfresco-content-services-share-distribution was in the Nexus 'Releases' repository prior to 7.1.0, which was visible to Community.
publishDistributionZip org.alfresco alfresco-content-services-share-distribution ${RELEASE_VERSION} https://nexus.alfresco.com/nexus/content/repositories/releases/

popd
set +vex
echo "=========================== Finishing Release Script =========================="

