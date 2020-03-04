#!/usr/bin/env bash
set -e

branch_name=$1
build_number=$2


echo_s3_location=https://s3.console.aws.amazon.com/s3/buckets/alfresco-artefacts-staging/alfresco-content-services/$bamboo_stage

distribution_zip_name=`ls distribution/target/*-distribution*.zip | xargs -n 1 basename`

echo "$echo_s3_location/$branch_name/$build_number/alfresco.war"
echo $echo_s3_location/$branch_name/$build_number/$distribution_zip_name