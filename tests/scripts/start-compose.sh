#!/usr/bin/env bash

export DOCKER_COMPOSE_PATH=$1

if [ -z "$DOCKER_COMPOSE_PATH" ]
then
  echo "Please provide path to docker-compose.yml: \"${0##*/} /path/to/docker-compose.yml\""
  exit 1
fi

echo "Starting ACS stack in ${DOCKER_COMPOSE_PATH}"

docker-compose --file "${DOCKER_COMPOSE_PATH}" up -d

if [ $? -eq 0 ]
then
  echo "Docker Compose started ok"
else
  echo "Docker Compose failed to start" >&2
  exit 1
fi

# find this script's directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

source ${SCRIP_DIR}/wait-for-alfresco-start.sh "http://localhost:8081/alfresco"
source ${SCRIP_DIR}/wait-for-alfresco-start.sh "http://localhost:8082/alfresco"
