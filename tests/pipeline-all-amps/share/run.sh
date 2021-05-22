#!/usr/bin/env bash

set -e

# image specific env vars
PROJECT_NAME=share
POM_PROPERTY_NAME=dependency.alfresco-enterprise-share.version
TAG_PROPERTY_NAME=$PROJECT_NAME.image.tag
DEFAULT_HOST_PORT=8081

[[ "$BUILD_ENABLED" == "true" ]] && mvn clean install
POM_FILE=../../../pom.xml
IMAGE_TAG=$(python -c "from xml.etree.ElementTree import parse; print(parse(open('$POM_FILE')).find('.//{http://maven.apache.org/POM/4.0.0}$POM_PROPERTY_NAME').text)")
echo using IMAGE_TAG=$IMAGE_TAG
sed -e "s@\${$PROJECT_NAME.image.tag}@$IMAGE_TAG@" Dockerfile > target/Dockerfile

HOST_PORT=${HOST_PORT:-$DEFAULT_HOST_PORT}
CONTAINER_PORT=${CONTAINER_PORT:-8080}
DOCKER_IMAGE_REPO=alfresco/alfresco-pipeline-all-amps-$PROJECT_NAME

docker rmi -f $DOCKER_IMAGE_REPO
docker build -f target/Dockerfile -t $DOCKER_IMAGE_REPO .
echo "http://localhost:${HOST_PORT}"
DOCKER_OPTS="--rm -it --env SERVER_PORT=$CONTAINER_PORT --publish $HOST_PORT:$CONTAINER_PORT"
[[ "$UNPRIVILEGED_ENABLED" == "true" ]] && DOCKER_OPTS="$DOCKER_OPTS --user 1000:1000"
docker run $DOCKER_OPTS $DOCKER_IMAGE_REPO "$@"
