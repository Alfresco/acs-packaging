#!/usr/bin/env bash
set -e
source scripts/travis/common_functions.sh

# Get versions from the commit message if provided as [version=vvv] or [next-version=vvv]
release_version=$RELEASE_VERSION
development_version=$DEVELOPMENT_VERSION
commit_release_version=$(extract_option "version" "$TRAVIS_COMMIT_MESSAGE")
commit_develop_version=$(extract_option "next-version" "$TRAVIS_COMMIT_MESSAGE")

#Remove (only) this echo
echo "Travis commit message echo: $TRAVIS_COMMIT_MESSAGE"

if [ -n $commit_release_version ]
then
    echo "Setting release version from commit message: $commit_release_version"
    release_version=$commit_release_version
fi

if [ -n $commit_develop_version ]
then
    echo "Setting next development version from commit message: $commit_release_version"
    development_version=$commit_develop_version
fi

scm_path=$(mvn help:evaluate -Dexpression=project.scm.url -q -DforceStdout)
# Use full history for release
git checkout -B "${TRAVIS_BRANCH}"
# Add email to link commits to user
git config user.email "${GIT_EMAIL}"

if [ -z ${release_version} ] || [ -z ${development_version} ];
    then echo "Please provide a Release and Development verison in the format <acs-version>-<additional-info> (6.3.0-EA or 6.3.0-SNAPSHOT)"
         exit -1
else   
    mvn --batch-mode -q \
    -Dusername="${GIT_USERNAME}" \
    -Dpassword="${GIT_PASSWORD}" \
    -DreleaseVersion=${release_version} \
    -DdevelopmentVersion=${development_version} \
    -Dbuild-number=${TRAVIS_BUILD_NUMBER} \
    -Dbuild-name="${TRAVIS_BUILD_STAGE_NAME}" \
    -Dversion.edition="Enterprise" \
    -Dscm-path=${scm_path} \
    -DscmCommentPrefix="[maven-release-plugin][skip ci]" \
    -DskipTests \
    "-Darguments=-DskipTests -Dversion.edition=Enterprise -Dbuild-number=${TRAVIS_BUILD_NUMBER} '-Dbuild-name=${TRAVIS_BUILD_STAGE_NAME}' -Dscm-path=${scm_path} " \
    release:clean release:prepare release:perform \
    -Prelease
fi