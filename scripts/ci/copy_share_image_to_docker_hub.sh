#!/usr/bin/env bash

echo "=========================== Starting Copy Share Image To Dockerhub Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set +e -v -x
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

#
# Copy Image from Quay to dockerhub
#

TAG_NAME=${RELEASE_VERSION}
SOURCE_IMAGE=quay.io/alfresco/alfresco-share
TARGET_IMAGE=docker.io/alfresco/alfresco-share

echo "${QUAY_PASSWORD}" | docker login -u="${QUAY_USERNAME}" --password-stdin quay.io
echo "${DOCKERHUB_PASSWORD}" | docker login -u="${DOCKERHUB_USERNAME}" --password-stdin docker.io

docker buildx imagetools create -t $TARGET_IMAGE:$TAG_NAME $SOURCE_IMAGE:$TAG_NAME

popd
set +vex
echo "=========================== Finishing Copy Share Image To Dockerhub Script =========================="

exit ${SUCCESS}