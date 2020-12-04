#!/usr/bin/env bash

set -e

PROJECT_NAME=repo
[[ "$BUILD_ENABLED" == "true" ]] && mvn clean install
POM_PROPERTY_NAME=dependency.alfresco-enterprise-repo.version
POM_FILE=../../../pom.xml
IMAGE_TAG=$(python -c "from xml.etree.ElementTree import parse; print(parse(open('$POM_FILE')).find('.//{http://maven.apache.org/POM/4.0.0}$POM_PROPERTY_NAME').text)")
echo using IMAGE_TAG=$IMAGE_TAG
sed -e "s@\${repo.image.tag}@$IMAGE_TAG@" Dockerfile > target/Dockerfile
env \
  HOST_PORT=8080 \
	DOCKER_IMAGE_REPO=alfresco/alfresco-pipeline-all-amps-$PROJECT_NAME \
	../run.sh "$@"
