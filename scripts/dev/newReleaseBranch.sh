#!/usr/bin/env bash

set -o errexit

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="${SCRIPT_DIR}/../../.."
LOGGING_OUT="/dev/null"

usage() {
    cat << ???  1>&2;

Creates HotFix and ServicePack (.N) branches for repo, share and packaging projects, and updates the master
or ServicePack branches ready for the next release.

Usage: $0 <hotfix_version> [-v <master_version>] [-h] [-l]
  <hotfix_version>:  HotFix Branch to be created or modified.
  -v <master_version>: Overrides the next development version on master so that the hotfix_major version may be changed.
                    Ignored if the master branch is not going to be modified. Must contain 3 numbers separated by dots.
  -h: Display this help
  -l: Output extra logging

Examples:
  1. After the release of 23.1.0 from the master branch:
     $ $0 23.1.0
     Creates the HotFix branch for 23.1.0, creates the 23.1.N ServicePack branch and modifies the master branch for
     use by the next minor version 23.2.0.

  2. After the release of 23.1.1 from the ServicePack branch release/23.1.N:
     $ $0 23.1.1
     Creates the HotFix branch for 23.1.1 and modifies the 23.1.N ServicePack branch for use by the next version 23.1.2

  3. After the release of 23.1.2 from the ServicePack branch release/23.1.N
     $ $0 23.1.2
     Creates the HotFix branch for 23.1.2 and modifies the 23.1.N ServicePack branch for use by the next version 23.1.3

  4. Switching to the next hotfix_major version:
     $ $0 23.2.0 -v 25.1.0
     Creates the HotFix branch for 23.2.0, creates the 23.2.N ServicePack branch and modifies the master branch for
     use by the next hotfix_major version 25.1.0.
???
}

readCommandLineArgs() {
  hotfix_version="${1}"
  if [ -z "${hotfix_version}" ]
  then
    usage
    exit 1
  fi
  shift 1

  master_version=""
  while getopts "v:lh" arg; do
      case $arg in
          v)
              master_version=${OPTARG}
              ;;
          l)
              LOGGING_OUT=`tty`
              ;;
          h | *)
              usage
              exit 0
              ;;
      esac
  done
  shift $((OPTIND-1))

  if [ "$#" != "0" ]; then
    usage
    exit 1
  fi
}

getVersionMajor() {
  echo "${1}" | sed -n "s/^\([0-9]*\)\.[0-9]*\.[0-9]*$/\1/p"
}

getVersionMinor() {
  echo "${1}" | sed -n "s/^[0-9]*\.\([0-9]*\)\.[0-9]*$/\1/p"
}

getVersionRevision() {
  echo "${1}" | sed -n "s/^[0-9]*\.[0-9]*\.\([0-9]*\)$/\1/p"
}

increment() {
  local number="${1}"
  echo "${number} + 1" | bc
}

getPomMajor() {
  # TODO
  echo 14
}

setPomVersion() {
  local version="${1}"
  local version_major="${2}"

  if [[ "${version_major}" -lt 23 ]]
  then
    pom_version=`getPomMajor`
    pom_version=`increment "${pom_version}"`
  else
    pom_version="${version}"
  fi
  pom_version="${pom_version}.1-SNAPSHOT"

  echo TODO setPomVersion "${pom_version}"
}

getSchema() {
  # TODO
  echo 17002
}

setSchema() {
  local schema="${1}"
  echo TODO setSchema "${schema}"
}

incrementSchema() {
  local schema_multiple="${1}"

  local schema=`getSchema`
  schema=`echo "(${schema} / ${schema_multiple} + 1) * ${schema_multiple}" | bc`
  setSchema "${schema}"
}

checkout() {
    local project="${1}"
    local version="${2}"

    cd "${ROOT_DIR}/${project}/"
    git fetch &>${LOGGING_OUT}
    git checkout "${version}" &>${LOGGING_OUT}
}

checkout_from_project() {
    local targetProject="${1}"
    local sourceProject="${2}"
    local targetProperty="${3}"

    local targetVersion=`grep ${targetProperty} ${ROOT_DIR}/${sourceProject}/pom.xml | sed "s|^.*<[^>]*>\([^<]*\)</[^>]*>.*$|\1|g"`
    if [[ "${version}" != "" ]]
    then
        checkout "${targetProject}" "${targetVersion}"
    else
        echo "ERROR: Could not find version for ${targetProject} from ${targetProperty} in ${sourceProject}."
        exit 2
    fi
}

checkoutBranch() {
  local branch="${1}"
  local version="${2}"

  echo checkoutBranch "${branch}"

  checkout acs-packaging           "${version}"
  checkout acs-community_packaging "${version}"

  checkout_from_project alfresco-enterprise-repo  acs-packaging            "<dependency.alfresco-enterprise-repo.version>"
  checkout_from_project alfresco-community-repo   alfresco-enterprise-repo "<dependency.alfresco-community-repo.version>"
  checkout_from_project alfresco-enterprise-share acs-packaging            "<dependency.alfresco-enterprise-share.version>"
}

forkBranch() {
  local sourceBranch="${1}"
  local version="${2}"
  local branch="${3}"

  echo TODO forkBranch from "${sourceBranch} tag ${version} to ${branch}"
}

setVersion() {
  local version="${1}"

  echo TODO setVersion "${version}"
}

modifyAndBuildBranch() {
  local version="${1}"
  local version_major="${2}"
  local schema_multiple="${3}"

  setPomVersion "${version}" "${version_major}"
  setVersion "${version}"
  incrementSchema "${schema_multiple}"
  buildBranch
}

buildBranch() {
  echo TODO buildBranch
  echo
}

calculateBranchVersions() {

  # HotFix version
  hotfix_major=`getVersionMajor "${hotfix_version}"`
  hotfix_minor=`getVersionMinor "${hotfix_version}"`
  hotfix_revision=`getVersionRevision "${hotfix_version}"`
  if [[ "${hotfix_version}" != "${hotfix_major}.${hotfix_minor}.${hotfix_revision}" ]]
  then
    echo 'The <hotfix_version> is invalid. Must contain 3 numbers separated by dots.'
    exit 1
  fi

  # ServicePack version
  servicepack_major="${hotfix_major}"
  servicepack_minor="${hotfix_minor}"
  servicepack_revision=`increment "${hotfix_revision}"`
  servicepack_version="${servicepack_major}.${servicepack_minor}.${servicepack_revision}"

  # Master version
  if [ -z "${master_version}" ]
  then
    master_major="${hotfix_major}"
    master_minor=`increment "${hotfix_minor}"`
    master_revision="0"
    master_version="${master_major}.${master_minor}.${master_revision}"
  else
    master_major=`getVersionMajor "${master_version}"`
    master_minor=`getVersionMinor "${master_version}"`
    master_revision=`getVersionRevision "${master_version}"`
    if [[ "${master_version}" != "${master_major}.${master_minor}.${master_revision}" ]]
    then
      echo 'The <master_version> is invalid. Must contain 3 numbers separated by dots.'
      exit 1
    fi
  fi

  # Branches
  hotfix_branch="release/${hotfix_version}"
  servicepack_branch="release/${hotfix_major}.${hotfix_minor}.N"
}

createAndModifyBranches() {

  calculateBranchVersions

  if [[ "${hotfix_revision}" == "0" ]]
  then
    # Create the HotFix branch
    forkBranch master "${hotfix_version}" "${hotfix_branch}"
    buildBranch

    # Create the ServicePack branch
    forkBranch "${hotfix_branch}" "${hotfix_version}" "${servicepack_branch}"
    modifyAndBuildBranch "${servicepack_version}" "${servicepack_major}" 100

    checkoutBranch master "${hotfix_version}"
    modifyAndBuildBranch "${master_version}" "${master_major}" 1000
  else
    # Create the HotFix branch
    forkBranch "${servicepack_branch}" "${hotfix_version}" "${hotfix_branch}"
    buildBranch

    # Modify the ServicePack branch
    checkoutBranch "${servicepack_branch}" "${hotfix_version}"
    modifyAndBuildBranch "${servicepack_version}" "${servicepack_major}" 100
  fi
}

readCommandLineArgs "$@"
createAndModifyBranches