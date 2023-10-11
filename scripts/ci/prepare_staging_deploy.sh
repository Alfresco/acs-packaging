#!/usr/bin/env bash

echo "========================== Starting Prepare Staging Deploy Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

# Identify latest annotated tag (latest version)
export VERSION=$(git describe --abbrev=0 --tags)

# Move the final artifacts to a single folder (deploy_dir) to be copied to S3
mkdir -p deploy_dir
cp distribution/target/alfresco.war deploy_dir
cp distribution/target/*-distribution*.zip deploy_dir
echo "Local deploy directory content:"
ls -lA deploy_dir

# Create deploy directory for Share.
mkdir -p deploy_dir_share
cp distribution-share/target/*.war deploy_dir_share
cp distribution-share/target/*.zip deploy_dir_share
echo "Local Share deploy directory content:"
ls -lA deploy_dir_share

# Create deploy directory for AGS.
mkdir -p deploy_dir_ags
cp distribution-ags/target/*.zip deploy_dir_ags
echo "Local AGS deploy directory content:"
ls -lA deploy_dir_ags

popd
set +vex
echo "========================== Finishing Prepare Staging Deploy Script =========================="
