#!/bin/bash
export START_COMPOSE_PATH=$1
export DOCKER_COMPOSE_PATH=$2

if [[ -z "$DOCKER_COMPOSE_PATH" ]] || [[ -z "$START_COMPOSE_PATH" ]];
then
  echo "To launch compose with all transform services enabled please provide paths to start_compose.sh and docker-compose.yml:"
  echo "\"${0##*/} /path/to/start_compose.sh /path/to/docker-compose.yml\" ."
  echo "To enable one or more transform service this can be followed with the following flags: \"--legacy\", \"--local\", and \"--transformservice\""
  exit 1
fi

docker info
docker-compose --version

# NOTE
# In ACS 6.2 existing LOCAL transformers were renamed to LEGACY

CUSTOM_DETECTED=false 
for arg in "$@"
do
    if [ "$arg" == "--legacy" ] || [ "$arg" == "--local" ] || [ "$arg" == "--transformservice" ]
    then
      if [ "$CUSTOM_DETECTED" == false ]
      then
        export LEGACY_TRANSFORM_SERVICE_ENABLED=false
        export LOCAL_TRANSFORM_SERVICE_ENABLED=false
        export TRANSFORM_SERVICE_ENABLED=false
        CUSTOM_DETECTED=true
      fi
      case $arg in
        --legacy )
          export LEGACY_TRANSFORM_SERVICE_ENABLED=true
          echo "LEGACY_TRANSFORM_SERVICE_ENABLED=${LEGACY_TRANSFORM_SERVICE_ENABLED}" ;;
        --local )
          export LOCAL_TRANSFORM_SERVICE_ENABLED=true
          echo "LOCAL_TRANSFORM_SERVICE_ENABLED=${LOCAL_TRANSFORM_SERVICE_ENABLED}" ;;
        --transformservice )
          export TRANSFORM_SERVICE_ENABLED=true 
          echo "TRANSFORM_SERVICE_ENABLED=${TRANSFORM_SERVICE_ENABLED}";;
      esac
    fi
done
if [ $LEGACY_TRANSFORM_SERVICE_ENABLED == false ]
then
  echo "LEGACY_TRANSFORM_SERVICE_ENABLED=${LEGACY_TRANSFORM_SERVICE_ENABLED}"
fi
if [ $LOCAL_TRANSFORM_SERVICE_ENABLED == false ]
then
  echo "LOCAL_TRANSFORM_SERVICE_ENABLED=${LOCAL_TRANSFORM_SERVICE_ENABLED}"
fi
if [ $TRANSFORM_SERVICE_ENABLED == false ]
then
  echo "TRANSFORM_SERVICE_ENABLED=${TRANSFORM_SERVICE_ENABLED}"
fi

$START_COMPOSE_PATH $DOCKER_COMPOSE_PATH

