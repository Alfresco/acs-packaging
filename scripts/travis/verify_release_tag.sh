#!/usr/bin/env bash
set -e
source scripts/travis/common_functions.sh


################################################# remove
# Get versions from the commit message if provided as [version=vvv] or [next-version=vvv]
release_version=$RELEASE_VERSION
development_version=$DEVELOPMENT_VERSION
export commit_release_version=$(extract_option "version" "$TRAVIS_COMMIT_MESSAGE")
export commit_develop_version=$(extract_option "next-version" "$TRAVIS_COMMIT_MESSAGE")

if [ ! -z "$commit_release_version" ]
then
    echo "Setting release version from commit message: $commit_release_version"
    release_version=$commit_release_version
fi

if [ ! -z "$commit_develop_version" ]
then
    echo "Setting next development version from commit message: $commit_release_version"
    development_version=$commit_develop_version
fi

export scm_path=$(mvn help:evaluate -Dexpression=project.scm.url -q -DforceStdout)

# Use full history for release
git checkout -B "${TRAVIS_BRANCH}"
# Add email to link commits to user
git config user.email "${GIT_EMAIL}"
#################################################

# Get versions from the commit message if provided as [version=vvv]
release_version=$RELEASE_VERSION
commit_release_version=$(extract_option "version" "$TRAVIS_COMMIT_MESSAGE")
if [ ! -z "$commit_release_version" ]; then
    echo "Setting release version from commit message: $commit_release_version"
    release_version=$commit_release_version
fi

# get the image name from the pom file
alfresco_docker_image=$(mvn help:evaluate -f ./docker-alfresco/pom.xml -Dexpression=image.name -q -DforceStdout)
if [ -v ${release_version} ]; then
    echo "Please provide a RELEASE_VERSION in the format <acs-version>-<additional-info> (6.3.0-EA or 6.3.0-SNAPSHOT)"
    exit -1
fi
docker_image_full_name="$alfresco_docker_image:$release_version"

function docker_image_exists() {
  local image_full_name="$1"; shift
    local wait_time="${1:-5}"
    local search_term='Pulling|is up to date|not found'
    echo "Looking to see if $image_full_name already exists..."
    local result="$((timeout --preserve-status "$wait_time" docker 2>&1 pull "$image_full_name" &) | grep -v 'Pulling repository' | egrep -o "$search_term")"
    test "$result" || { echo "Timed out too soon. Try using a wait_time greater than $wait_time..."; return 1 ;}
    if echo $result | grep -vq 'not found'; then
        true
    else 
        false
    fi
}

if docker_image_exists $docker_image_full_name; then
    echo "Tag $RELEASE_VERSION already pushed, release process will interrupt."
    exit -1 
else
    echo "The $RELEASE_VERSION tag was not found"
fi