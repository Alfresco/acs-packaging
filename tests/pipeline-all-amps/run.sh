#!/usr/bin/env bash

set -e

HOST_PORT=${HOST_PORT:-8080}
CONTAINER_PORT=${CONTAINER_PORT:-8080}
cp ../docker-entrypoint.sh target/

docker rmi -f $DOCKER_IMAGE_REPO
docker build -f target/Dockerfile -t $DOCKER_IMAGE_REPO .
echo "http://localhost:${HOST_PORT}"
# --user 1000:1000 \
docker run --rm -it \
  --env SERVER_PORT=$CONTAINER_PORT \
  --publish $HOST_PORT:$CONTAINER_PORT $DOCKER_IMAGE_REPO "$@"
