#!/usr/bin/env bash
echo "=========================== Starting Publish Script ==========================="
# downloads the distribution zips from the internal Nexus repository and then uploads them to the enterprise release repository

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
  local LOCAL_PATH="~/.m2/repository/${ARTIFACT_PATH}"
  local TMP_PATH="/tmp/${ARTIFACT_ID}"
  local FILE_NAME=${ARTIFACT_ID}-${VERSION}

  echo
  echo "     GROUP_ID=${GROUP_ID}"
  echo "  ARTIFACT_ID=${ARTIFACT_ID}"
  echo "   LOCAL_PATH=${LOCAL_PATH}"
  echo "ARTIFACT_PATH=${ARTIFACT_PATH}"
  echo "   LOCAL_PATH=${LOCAL_PATH}"
  echo "     TMP_PATH=${TMP_PATH}"
  echo "    FILE_NAME=${FILE_NAME}"

  rm -rf "${LOCAL_PATH}"
  mvn org.apache.maven.plugins:maven-dependency-plugin:get  \
    -Dartifact=${GROUP_ID}:${ARTIFACT_ID}:${VERSION}:${VERSION} \
    -Dtransitive=false
  ls -l "${LOCAL_PATH}/${VERSION}"

  rm -rf "${TMP_PATH}"
  mv "${LOCAL_PATH}/${VERSION}" "${TMP_PATH}"

  echo mvn deploy:deploy-file \
       -Dfile="${TMP_PATH}/${FILE_NAME}.${TYPE}" \
    -DpomFile="${TMP_PATH}/${FILE_NAME}.pom" \
    -Dsources="${TMP_PATH}/${FILE_NAME}-sources.jar"\
      -Dfiles="${TMP_PATH}/${FILE_NAME}.zip" \
    -Dtypes=zip
    -DrepositoryId=alfresco-enterprise-releases \
    -Durl=https://nexus.alfresco.com/nexus/content/repositories/enterprise-releases/ \
    -DgroupId="${GROUP_ID}" -DartifactId="${ARTIFACT_ID}" -Dversion="${VERSION}"
}

publishDistributionZip org.alfresco alfresco-content-services-distribution ${RELEASE_VERSION}

popd
set +vex
echo "=========================== Finishing Release Script =========================="

