#!/usr/bin/env bash
set -e
source scripts/travis/common_functions.sh

#TODO Remove (only) this echo

echo "Travis commit message echo: $TRAVIS_COMMIT_MESSAGE"

# Get versions from the commit message if provided as [version=vvv]
release_version=$RELEASE_VERSION
commit_release_version=$(extract_option "version" " $TRAVIS_COMMIT_MESSAGE")
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