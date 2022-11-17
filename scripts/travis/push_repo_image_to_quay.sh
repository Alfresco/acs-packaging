#!/usr/bin/env bash

echo "=========================== Starting to push Repo Image ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set +e -v -x
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

#
# Push the Image to Quay
#
echo "${QUAY_PASSWORD}" | docker login -u="${QUAY_USERNAME}" --password-stdin quay.io

docker image tag alfresco/alfresco-pipeline-all-amps-repo:latest alfresco/dev:all-amps-repo
docker push alfresco/dev:all-amps-repo

popd
set +vex
echo "=========================== Finishing Repo Image To Quay =========================="

exit ${SUCCESS}