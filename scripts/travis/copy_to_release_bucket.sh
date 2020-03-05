#!/usr/bin/env bash
set -ev
source scripts/travis/common_functions.sh

#
# Copy from S3 Release bucket to S3 eu.dl bucket
#

# Get versions from the commit message if provided as [version=vvv]
release_version=$RELEASE_VERSION
commit_release_version=$(extract_option "version" "$TRAVIS_COMMIT_MESSAGE")
if [ -n $commit_release_version ]
then
    echo "Setting release version from commit message: $commit_release_version"
    release_version=$commit_release_version
fi

if [ -z ${release_version} ];
then
  echo "Please provide a RELEASE_VERSION in the format <acs-version>-<additional-info> (6.3.0-EA or 6.3.0-SNAPSHOT)"
  exit -1
fi


bamboo_stage=release

SOURCE=s3://alfresco-artefacts-staging/alfresco-content-services/snapshots/test/ek/${TRAVIS_BRANCH}/${TRAVIS_BUILD_NUMBER}
DESTINATION=s3://eu.dl.alfresco.com/release/enterprise/ek/ACS/${release_version:0:3}/$release_version/$TRAVIS_BUILD_NUMBER

#SOURCE=s3://alfresco-artefacts-staging/alfresco-content-services/release/$TRAVIS_BRANCH/$TRAVIS_BUILD_NUMBER
#DESTINATION=s3://eu.dl.alfresco.com/release/enterprise/ACS/${release_version:0:3}/$release_version/$TRAVIS_BUILD_NUMBER


echo "
$SOURCE\n
$DESTINATION\n"

aws s3 cp --acl private $SOURCE/alfresco.war $DESTINATION/alfresco.war
aws s3 cp --acl private $SOURCE/alfresco-content-services-distribution-$release_version.zip $DESTINATION/alfresco-content-services-distribution-$release_version.zip