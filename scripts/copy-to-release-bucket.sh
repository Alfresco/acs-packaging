#!/bin/bash
-e
#
# Copy from S3 Release bucket to S3 eu.dl bucket
#


build_number=$1
branch_name=$2
bamboo_stage=release
SOURCE=s3://alfresco-artefacts-staging/alfresco-content-services/$bamboo_stage/$branch_name/$build_number
DESTINATION=s3://eu.dl.alfresco.com/release/enterprise/ACS/${release_version:0:3}/$release_version/$build_number


echo "
$SOURCE\n
$DESTINATION\n"

aws s3 cp --acl private $SOURCE/alfresco.war $DESTINATION/alfresco.war
aws s3 cp --acl private $SOURCE/alfresco-content-services-distribution-$release_version.zip $DESTINATION/alfresco-content-services-distribution-$release_version.zip