#!/usr/bin/env bash

set -o errexit

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="${SCRIPT_DIR}/../../.."
LOGGING_OUT="/dev/null"
prefix=""

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
              prefix="== "
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

updateTopLevelPomVersion() {
  echo TODO
}

setPomVersion() {
  local pom_version="${1}"
  local profiles="${2}"

  echo "${prefix}  Set pom version ${pom_version} using profiles: ${profiles}"
  mvn versions:set -DnewVersion="${pom_version}" -DgenerateBackupPoms=false -P"${profiles}" &>${LOGGING_OUT}
}

getPomProperty() {
  local project="${1}"
  local property="${2}"

  grep ${property} ${ROOT_DIR}/${project}/pom.xml | sed "s|^.*<[^>]*>\([^<]*\)</[^>]*>.*$|\1|g"
}

setScmTag() {
  local tag="${1}"

  echo "${prefix}  Set <scm><tag> ${tag}"
  ed -s pom.xml &>/tmp/$$.log << EOF
/<scm>
/<tag>.*<\/tag>/s//<tag>${tag}<\/tag>/
wq
EOF

}

getSchema() {
  # TODO
  echo 17002
}

setSchema() {
  local schema="${1}"
  echo "${prefix}  TODO setSchema ${schema}"
}

setVersion() {
  local version="${1}"

  echo "${prefix}  TODO setVersion ${version}"
}

incrementSchema() {
  local schema_multiple="${1}"

  local schema=`getSchema`
  schema=`echo "(${schema} / ${schema_multiple} + 1) * ${schema_multiple}" | bc`
  setSchema "${schema}"
}

checkout() {
  local project="${1}"
  local branch="${2}"

  echo "${prefix}Checkout ${project} ${branch}"

  cd "${ROOT_DIR}/${project}/"
  git fetch                  &>${LOGGING_OUT}
  git checkout "${branch}"   &>${LOGGING_OUT}
}

createBranchFromTag() {
  local project="${1}"
  local tag="${2}"
  local newBranch="${3}"

  echo "${prefix}  Create ${project} ${newBranch} from tag ${tag}"

  cd "${ROOT_DIR}/${project}/"
  git fetch                    &>${LOGGING_OUT}
  git checkout "${tag}"        &>${LOGGING_OUT}
  git switch -c "${newBranch}" &>${LOGGING_OUT}
}

checkoutProjectBranches() {
  local branch="${1}"

  checkout acs-packaging             "${branch}"
  checkout acs-community-packaging   "${branch}"
  checkout alfresco-enterprise-share "${branch}"
  checkout alfresco-enterprise-repo  "${branch}"
  checkout alfresco-community-repo   "${branch}"
}

createProjectBranchesFromAcsVersion() {
  local version="${1}"
  local newBranch="${2}"

  createBranchFromTag acs-packaging            "${version}"                "${newBranch}"

  createBranchFromTag acs-community-packaging  "${version}"                "${newBranch}"

  shareVersion=`getPomProperty                  acs-packaging               "<dependency.alfresco-enterprise-share.version>"`
  createBranchFromTag alfresco-enterprise-share "${shareVersion}"           "${newBranch}"

  enterpriseRepoVersion=`getPomProperty         acs-packaging               "<dependency.alfresco-enterprise-repo.version>"`
  createBranchFromTag alfresco-enterprise-repo  "${enterpriseRepoVersion}"  "${newBranch}"

  communityRepoVersion=`getPomProperty          alfresco-enterprise-repo    "<dependency.alfresco-community-repo.version>"`
  createBranchFromTag alfresco-community-repo   "${communityRepoVersion}"   "${newBranch}"

  echo
}

modifyPomVersion() {
  local version="${1}"
  local version_major="${2}"
  local profiles="${3}"
  local packaging_project="${4}"

  if [[ "${version_major}" -lt 23 && "${packaging_project}" -ne "true" ]]
  then
    pom_version=`getPomMajor`
    pom_version=`increment "${pom_version}"`
  else
    pom_version="${version}"
  fi
  pom_version="${pom_version}.1-SNAPSHOT"

  setPomVersion "${pom_version}" "${profiles}"
}

modifyProject() {
  local project="${1}"
  local packaging_project="${2}"
  local version="${3}"
  local version_major="${4}"
  local schema_multiple="${5}"
  local profiles="${6}"

  echo "${prefix}${project}:"
  cd "${ROOT_DIR}/${project}/"
  modifyPomVersion "${version}" "${version_major}" "${profiles}" "${packaging_project}"
  setScmTag HEAD
  setVersion "${version}"
  incrementSchema "${schema_multiple}"
  echo
}

modifyProjectBranches() {
  local version="${1}"
  local version_major="${2}"
  local schema_multiple="${3}"

  modifyProject alfresco-community-repo   false "${version}" "${version_major}" "${schema_multiple}" ags
  modifyProject alfresco-enterprise-repo  false "${version}" "${version_major}" "${schema_multiple}" ags
  modifyProject alfresco-enterprise-share false "${version}" "${version_major}" "${schema_multiple}" ags
  modifyProject acs-packaging             true  "${version}" "${version_major}" "${schema_multiple}" dev
  modifyProject acs-community-packaging   true  "${version}" "${version_major}" "${schema_multiple}" dev
}

commitAndPush() {
  local project="${1}"
  local message="${2}"

  echo "${prefix}  Commit: ${message}"
  cd "${ROOT_DIR}/${project}/"
  git commit --allow-empty -m "${message}" &>${LOGGING_OUT}
  echo "${prefix}TODO push"
#  git push                                  &>${LOGGING_OUT}
  echo
}

commitAndPushProjectBranches() {
  local message="${1}"

  commitAndPush alfresco-community-repo   "${message}"
  commitAndPush alfresco-enterprise-repo  "${message} [skip ci]"
  commitAndPush alfresco-enterprise-share "${message} [skip ci]"
  commitAndPush acs-packaging             "${message} [skip ci]"
  commitAndPush acs-community-packaging   "${message} [skip ci]"
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
  hotfix_branch="release/test/${hotfix_version}"
  servicepack_branch="release/test/${hotfix_major}.${hotfix_minor}.N"
}

createHotFixProjectBranches() {
   local hotfix_version="${1}"
   local hotfix_branch="${2}"

   echo "${prefix}Create the HotFix branches"
   createProjectBranchesFromAcsVersion "${hotfix_version}" "${hotfix_branch}"
   commitAndPushProjectBranches "Create HotFix branch for ${hotfix_version}"
}

createServicePackProjectBranches() {
  local hotfix_version="${1}"
  local servicepack_branch="${2}"
  local servicepack_version="${3}"
  local servicepack_major="${4}"

  echo "${prefix}Create the ServicePack branches"
  createProjectBranchesFromAcsVersion "${hotfix_version}" "${servicepack_branch}"
  modifyProjectBranches "${servicepack_version}" "${servicepack_major}" 100
  commitAndPushProjectBranches "Create ServicePack branch ${servicepack_branch}"
}

modifyOriginalProjectBranches() {
  local branchType="${1}"
  local branch="${2}"
  local version="${3}"
  local version_major="${4}"
  local schema_multiple="${5}"

  echo "${prefix}Modify the ${branchType} branches"
  checkoutProjectBranches "${branch}"
  modifyProjectBranches "${version}" "${version_major}" 100
  commitAndPushProjectBranches "Update ${branchType} branch to ${version}"
}

createAndModifyProjectBranches() {
  calculateBranchVersions
  createHotFixProjectBranches "${hotfix_version}" "${hotfix_branch}"
  if [[ "${hotfix_revision}" == "0" ]]
  then
    createServicePackProjectBranches "${hotfix_version}" "${servicepack_branch}" "${servicepack_version}" "${servicepack_major}"
    modifyOriginalProjectBranches Master master-test "${master_version}" "${master_major}" 1000
  else
    modifyOriginalProjectBranches ServicePack "${servicepack_branch}" "${servicepack_version}" "${servicepack_major}" 100
  fi
}

cleanUp() {
  local project="${1}"

  echo "${prefix}Clean up ${project}"
  cd "${ROOT_DIR}/${project}/"
  git checkout .                   &>${LOGGING_OUT}
  git checkout master-test         &>${LOGGING_OUT}
  git branch -D release/test/7.2.0 &>${LOGGING_OUT}
  git branch -D release/test/7.2.1 &>${LOGGING_OUT}
  git branch -D release/test/7.2.N &>${LOGGING_OUT}
}

cleanUpProjectBranches() {
  echo
  set +o errexit
  cleanUp alfresco-community-repo
  cleanUp alfresco-enterprise-repo
  cleanUp alfresco-enterprise-share
  cleanUp acs-community-packaging
  cleanUp acs-packaging
  set -o errexit
  echo
}

readCommandLineArgs "$@"
  cleanUpProjectBranches
createAndModifyProjectBranches
echo Done