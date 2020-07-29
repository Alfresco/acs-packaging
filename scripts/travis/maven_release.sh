#!/usr/bin/env bash
set -ev

# Use full history for release
git checkout -B "${TRAVIS_BRANCH}"
# Add email to link commits to user
git config user.email "${GIT_EMAIL}"

if [ -z "${RELEASE_VERSION}" ] || [ -z "${DEVELOPMENT_VERSION}" ]; then
  echo "Please provide a Release and Development verison in the format <acs-version>-<additional-info> (7.0.0-EA or 7.0.0-SNAPSHOT)"
  exit 1
fi

mvn -B \
  -PfullBuild,all-tas-tests \
  -Prelease \
  -DskipTests \
  -Dusername="${GIT_USERNAME}" \
  -Dpassword="${GIT_PASSWORD}" \
  -DreleaseVersion="${RELEASE_VERSION}" \
  -DdevelopmentVersion="${DEVELOPMENT_VERSION}" \
  -Dbuild-number="${TRAVIS_BUILD_NUMBER}" \
  -Dbuild-name="${TRAVIS_BUILD_STAGE_NAME}" \
  -Dversion.edition="Enterprise" \
  -DscmCommentPrefix="[maven-release-plugin][skip ci] " \
  "-Darguments=-DskipTests -Dversion.edition=Enterprise -Dbuild-number=${TRAVIS_BUILD_NUMBER} '-Dbuild-name=${TRAVIS_BUILD_STAGE_NAME}' -PfullBuild,all-tas-tests" \
  release:clean release:prepare release:perform

