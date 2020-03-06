#!/usr/bin/env bash
set -e

#export scm_path=$(mvn help:evaluate -Dexpression=project.scm.url -q -DforceStdout)

# Use full history for release
git checkout -B "${TRAVIS_BRANCH}"
# Add email to link commits to user
git config user.email "${GIT_EMAIL}"

if [ -z ${RELEASE_VERSION} ] || [ -z ${DEVELOPMENT_VERSION} ];
    then echo "Please provide a Release and Development verison in the format <acs-version>-<additional-info> (6.3.0-EA or 6.3.0-SNAPSHOT)"
         exit -1
else
    mvn --batch-mode -q \
    -Dusername="${GIT_USERNAME}" \
    -Dpassword="${GIT_PASSWORD}" \
    -DreleaseVersion=6.3.0-test-release-repo-4736-4 \
    -DdevelopmentVersion=6.3.0-test-1-SNAPSHOT \
    -Dbuild-number=${TRAVIS_BUILD_NUMBER} \
    -Dbuild-name="${TRAVIS_BUILD_STAGE_NAME}" \
    -Dversion.edition="Enterprise" \
    -DscmCommentPrefix="[maven-release-plugin][skip ci]" \
    -DskipTests \
    "-Darguments=-DskipTests -Dversion.edition=Enterprise -Dbuild-number=${TRAVIS_BUILD_NUMBER} '-Dbuild-name=${TRAVIS_BUILD_STAGE_NAME}' -Dscm-path=${scm_path} " \
    release:clean release:prepare release:perform \
    -Prelease


#     mvn --batch-mode -q \
#    -Dusername="${GIT_USERNAME}" \
#    -Dpassword="${GIT_PASSWORD}" \
#    -DreleaseVersion=${RELEASE_VERSION} \
#    -DdevelopmentVersion=${DEVELOPMENT_VERSION} \
#    -Dbuild-number=${TRAVIS_BUILD_NUMBER} \
#    -Dbuild-name="${TRAVIS_BUILD_STAGE_NAME}" \
#    -Dversion.edition="Enterprise" \
#    -Dscm-path=${scm_path} \
#    -DscmCommentPrefix="[maven-release-plugin][skip ci]" \
#    -DskipTests \
#    "-Darguments=-DskipTests -Dversion.edition=Enterprise -Dbuild-number=${TRAVIS_BUILD_NUMBER} '-Dbuild-name=${TRAVIS_BUILD_STAGE_NAME}' -Dscm-path=${scm_path} " \
#    release:clean release:prepare release:perform \
#    -Prelease
fi