#!/usr/bin/env bash
echo "=========================== Starting Publish Script ==========================="
# Downloads the distribution zips from the Nexus Internal Releases repository and then uploads them to the Enterprise
# Releases repository

PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"


if [ -z "${RELEASE_VERSION}" ]; then
  echo "Please provide a Release version in the format <acs-version>-<additional-info> (7.2.0-EA or 7.2.0)"
  exit 1
fi

function publishDistributionZip() {
  local GROUP_ID="${1}"
  local ARTIFACT_ID="${2}"
  local VERSION="${3}"

  local ARTIFACT_PATH="$(echo ${GROUP_ID}/${ARTIFACT_ID} | sed 's/\./\//g')"
  local LOCAL_PATH="${HOME}/.m2/repository/${ARTIFACT_PATH}"
  local TMP_PATH="/tmp/${ARTIFACT_ID}"

  # Download the artifact. Make sure we are not using a cached version
  rm -rf "${LOCAL_PATH}"
  mvn org.apache.maven.plugins:maven-dependency-plugin:get  \
    -Dartifact=${GROUP_ID}:${ARTIFACT_ID}:${VERSION}:zip \
    -Dtransitive=false
  ls -l "${LOCAL_PATH}/${VERSION}"

  # The local maven repo must not contain the downloaded artifacts otherwise the upload can fail
  rm -rf "${TMP_PATH}"
  mv "${LOCAL_PATH}/${VERSION}" "${TMP_PATH}"

  # Upload the artifact
  mvn deploy:deploy-file \
       -Dfile="${TMP_PATH}/${ARTIFACT_ID}-${VERSION}.zip" \
    -DrepositoryId=alfresco-enterprise-releases \
    -Durl=https://nexus.alfresco.com/nexus/content/repositories/enterprise-releases/ \
    -DgroupId="${GROUP_ID}" -DartifactId="${ARTIFACT_ID}" -Dversion="${VERSION}" \
    -Dpackaging=zip
}

publishDistributionZip org.alfresco alfresco-content-services-distribution               ${RELEASE_VERSION}
publishDistributionZip org.alfresco alfresco-content-services-share-distribution         ${RELEASE_VERSION}
publishDistributionZip org.alfresco alfresco-governance-services-enterprise-distribution ${RELEASE_VERSION}

popd
set +vex
echo "=========================== Finishing Release Script =========================="


