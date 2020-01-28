#!/bin/bash

alfresco_docker_image="alfresco/alfresco-content-repository"
get_tags="$(curl https://hub.docker.com/r/$alfresco_docker_image/tags/ | grep -o '\"result\".*\"]')"
arrayTags=($get_tags)

echo "Existing Tags: $get_tags"

for tag in "${arrayTags[@]}"
do
    if [[ $tag = $release_version ]]; then
        echo "Tag $release_version already pushed, release process will interrupt."
        exit -1
    fi
done

echo "The $release_version tag was not found"