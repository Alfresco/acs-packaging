#!/usr/bin/env bash
set -ev

#
# Copy from S3 Release bucket to S3 eu.dl bucket
#

build_number=$1
branch_name=$2
release_version=$3
bamboo_stage=release
SOURCE=s3://alfresco-artefacts-staging/alfresco-content-services/snapshots/test/ek/${TRAVIS_BRANCH}/${TRAVIS_BUILD_NUMBER}
DESTINATION=s3://eu.dl.alfresco.com/release/enterprise/ek/ACS/${RELEASE_VERSION:0:3}/$RELEASE_VERSION/$TRAVIS_BUILD_NUMBER
#SOURCE=s3://alfresco-artefacts-staging/alfresco-content-services/release/$branch_name/$build_number
#DESTINATION=s3://eu.dl.alfresco.com/release/enterprise/ACS/${RELEASE_VERSION:0:3}/$RELEASE_VERSION/$TRAVIS_BUILD_NUMBER


echo "
$SOURCE\n
$DESTINATION\n"

aws s3 cp --acl private $SOURCE/alfresco.war $DESTINATION/alfresco.war
aws s3 cp --acl private $SOURCE/alfresco-content-services-distribution-$release_version.zip $DESTINATION/alfresco-content-services-distribution-$release_version.zip