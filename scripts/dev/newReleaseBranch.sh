#!/usr/bin/env bash

set -o errexit

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="${SCRIPT_DIR}/../../.."

usage() {
    cat << ???  1>&2;

Creates HotFix and ServicePack (.N) branches for repo, share and packaging projects, and updates the master
or ServicePack branches ready for the next release.

Usage: $0 <hotFixVersion> [-v <masterVersion>] [-h] [-l]
  <hotFixVersion>:  HotFix Branch to be created or modified.
  -v <masterVersion>: Overrides the next development version on master so that the HotFix major version may be changed.
                    Ignored if the master branch is not going to be modified. Must contain 3 numbers separated by dots.
  -h: Display this help
  -l: Output extra logging
  -t: use test branches: master-test and release/test/X.x.x
  -c: cleanup (delete) local .../X.x.x branches that are about to be created or modified
  -p: skip the push to the remote git repository

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

  4. Switching to the next HotFix major version:
     $ $0 23.2.0 -v 25.1.0
     Creates the HotFix branch for 23.2.0, creates the 23.2.N ServicePack branch and modifies the master branch for
     use by the next Hot Fix major version 25.1.0.
???
}
#  Test switching to 23.1.0 after the release of 7.2.0
#     $ $0 7.2.0 -ptc -v 23.1.0

readCommandLineArgs() {
  hotFixVersion="${1}"
  if [ -z "${hotFixVersion}" ]
  then
    usage
    exit 1
  fi
  shift 1

  masterVersion=""
  doPush="true"
  doTest=""
  doCleanup=""
  loggingOut="/dev/null"
  prefix=""
  while getopts "v:ptclh" arg; do
      case $arg in
          v)
              masterVersion=${OPTARG}
              ;;
          p)
              doPush=""
              ;;
          t)
              doTest="true"
              ;;
          c)
              doCleanup="true"
              ;;
          l)
              loggingOut=`tty`
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

function readTopLevelTag() {
  local tagName="${1}"
  local pomFile="${2}"
  # Might be possible to generalise this function to accept an XPath so it could be used in place of sed commands

  # Read the file with an IFS (Input Field Separator) of the start of XML tag character <
  local IFS=\>
  local depth=-99
  while read -d \< ENTITY CONTENT
  do
    if [[ $ENTITY == project\ * ]] # outer <project> tag
    then
      depth=0
    elif [[ $ENTITY == /* ]] # end tag
    then
      ((depth=depth-1))
    else                     # start tag
      ((depth=depth+1))
    fi

    if [[ $ENTITY = "${tagName}" ]] && [[ $depth == 1 ]] ; then
        echo $CONTENT
        exit
    fi
  done < $pomFile
  exit 1
}

getPomVersion() {
  # Same as slower/simpler: "mvn help:evaluate -Dexpression=project.version"
  readTopLevelTag version pom.xml
}

getPomMajor() {
  getPomVersion | sed -n "s/^\([0-9]*\).*/\1/p"
}

getPomMinor() {
  getPomVersion | sed -n "s/^[0-9]*\.\([0-9]*\).*/\1/p"
}

getPomLabel() {
  getPomVersion | sed -n "s/^[0-9]*\.[0-9]*\.[0-9]*\.\([0-9]*\).*/\1/p"
}

setPomVersion() {
  local pomVersion="${1}"
  local profiles="${2}"

  echo "${prefix}    Set pom version ${pomVersion} using profiles: ${profiles}"
  mvn versions:set -DnewVersion="${pomVersion}" -DgenerateBackupPoms=false -P"${profiles}" &>${loggingOut}
}

getPomProperty() {
  local project="${1}"
  local property="${2}"

  grep ${property} ${ROOT_DIR}/${project}/pom.xml | sed "s|^.*<[^>]*>\([^<]*\)</[^>]*>.*$|\1|g"
}

setScmTag() {
  local newBranch="${1}"
  local tag="${2}"

  if [[ "${newBranch}" == "NewBranch" ]]
  then
    echo "${prefix}    Set <scm><tag> ${tag}"
    ed -s pom.xml &>${loggingOut} << EOF
/<scm>
/<tag>.*<\/tag>/s//<tag>${tag}<\/tag>/
wq
EOF
  fi
}

getSchema() {
  grep '^version\.schema=\d*.*$' repository/src/main/resources/alfresco/repository.properties | sed 's/.*=\(\d*\)/\1/'
}

setSchema() {
  local schema="${1}"

  echo "${prefix}    setSchema ${schema}"
  ed -s repository/src/main/resources/alfresco/repository.properties &>${loggingOut} << EOF
/^version\.schema=\d*.*$/s//version.schema=${schema}/
wq
EOF
}

getCurrentProject() {
  local pwd=`pwd`
  basename ${pwd}
}

setVersion() {
  local version="${1}"

  echo "${prefix}    TODO setVersion ${version}"
}

incrementSchema() {
  local schemaMultiple="${1}"

  if [[ `getCurrentProject` == "alfresco-community-repo" && "${schemaMultiple}" -gt 0 ]]
  then
    local schema=`getSchema`
    schema=`echo "(${schema} / ${schemaMultiple} + 1) * ${schemaMultiple}" | bc`
    setSchema "${schema}"
  fi
}

checkout() {
  local project="${1}"
  local branch="${2}"

  echo "${prefix}  Checkout ${project} ${branch}"

  cd "${ROOT_DIR}/${project}/"
  git fetch                  &>${loggingOut}
  git checkout "${branch}"   &>${loggingOut}
}

createBranchFromTag() {
  local project="${1}"
  local tag="${2}"
  local branch="${3}"

  echo "${prefix}  Create ${project} ${branch} from tag ${tag}"

  cd "${ROOT_DIR}/${project}/"
  git fetch                 &>${loggingOut}
  git checkout "${tag}"     &>${loggingOut}
  git switch -c "${branch}" &>${loggingOut}
}

modifyPomVersion() {
  local version="${1}"
  local branchType="${2}"
  local profiles="${3}"
  local projectType="${4}"

  if [[ "${projectType}" == "Packaging" ]]
  then
    pomVersion="${version}-SNAPSHOT"
  else
    local versionMajor=`getVersionMajor "${version}"`
    if [[ "${versionMajor}" -lt 23 ]]
    then
      pomMajor=`getPomMajor`
      if [[ "${branchType}" == "HotFix" ]]
      then
        pomMinor=`getPomMinor`
        pomMinor=`increment "${pomMinor}"`
        pomVersion="${pomMajor}.${pomMinor}-SNAPSHOT"
    else
        if [[ "${branchType}" == "Master" ]]
        then
          # Allow for two service packs before the next release off master
          pomMajor=`increment "${pomMajor}"`
          pomMajor=`increment "${pomMajor}"`
        fi
        pomMajor=`increment "${pomMajor}"`
        pomVersion="${pomMajor}.1-SNAPSHOT"
      fi
    else
      if [[ "${branchType}" == "HotFix" ]]
      then
        pomLabel=`getPomLabel`
        pomLabel=`increment "${pomLabel}"`
      else
        pomLabel="1"
      fi
      pomVersion="${version}.${pomLabel}-SNAPSHOT"
    fi
  fi

  setPomVersion "${pomVersion}" "${profiles}"
}

modifyProject() {
  local version="${1}"
  local branchType="${2}"
  local schemaMultiple="${3}"
  local profiles="${4}"
  local projectType="${5}"
  local newBranch="${6}"

  modifyPomVersion "${version}" "${branchType}" "${profiles}" "${projectType}"
  setScmTag "${newBranch}" HEAD
  setVersion "${version}"
  incrementSchema "${schemaMultiple}"
}

commitAndPush() {
  local message="${1}"

  echo "${prefix}    git commit"
  git commit --all --allow-empty -m "${message}" &>${loggingOut}
  if [[ -n "${doPush}" ]]
  then
    echo "${prefix}     git push"
    git push &>${loggingOut}
  else
    echo  "${prefix}    # git push"
  fi
}

createProjectBranchesFromAcsVersion() {
  local hotFixVersion="${1}"
  local branch="${2}"
  local version="${3}"
  local branchType="${4}"
  local schemaMultiple="${5}"
  local message="${6}"

  createBranchFromTag acs-packaging             "${hotFixVersion}"        "${branch}"
  modifyProject "${version}"                    "${branchType}"           "${schemaMultiple}"       dev Packaging NewBranch
  commitAndPush "${message} [skip ci]"

  createBranchFromTag acs-community-packaging   "${hotFixVersion}"        "${branch}"
  modifyProject "${version}"                    "${branchType}"           "${schemaMultiple}"       dev Packaging NewBranch
  commitAndPush "${message} [skip ci]"

  shareVersion=`getPomProperty                  acs-packaging              "<dependency.alfresco-enterprise-share.version>"`
  createBranchFromTag alfresco-enterprise-share "${shareVersion}"          "${branch}"
  modifyProject "${version}"                    "${branchType}"            "${schemaMultiple}"      ags Library   NewBranch
  commitAndPush "${message} [skip ci]"

  enterpriseRepoVersion=`getPomProperty         acs-packaging              "<dependency.alfresco-enterprise-repo.version>"`
  createBranchFromTag alfresco-enterprise-repo  "${enterpriseRepoVersion}" "${branch}"
  modifyProject "${version}"                    "${branchType}"            "${schemaMultiple}"      ags Library   NewBranch
  commitAndPush "${message} [skip ci]"

  communityRepoVersion=`getPomProperty          alfresco-enterprise-repo   "<dependency.alfresco-community-repo.version>"`
  createBranchFromTag alfresco-community-repo   "${communityRepoVersion}"  "${branch}"
  modifyProject "${version}"                    "${branchType}"            "${schemaMultiple}"      ags Library   NewBranch
  commitAndPush "${message}"
}

modifyOriginalProjectBranchesForNextRelease() {
  local branch="${1}"
  local version="${2}"
  local branchType="${3}"
  local schemaMultiple="${4}"
  local message="${5}"

  checkout acs-packaging             "${branch}"
  modifyProject "${version}" "${branchType}" "${schemaMultiple}" dev Packaging OriginalBranch
  commitAndPush "${message} [skip ci]"

  checkout acs-community-packaging   "${branch}"
  modifyProject "${version}" "${branchType}" "${schemaMultiple}" dev Packaging OriginalBranch
  commitAndPush "${message} [skip ci]"

  checkout alfresco-enterprise-share "${branch}"
  modifyProject "${version}" "${branchType}" "${schemaMultiple}" ags Library   OriginalBranch
  commitAndPush "${message} [skip ci]"

  checkout alfresco-enterprise-repo  "${branch}"
  modifyProject "${version}" "${branchType}" "${schemaMultiple}" ags Library   OriginalBranch
  commitAndPush "${message} [skip ci]"

  checkout alfresco-community-repo   "${branch}"
  modifyProject "${version}" "${branchType}" "${schemaMultiple}" ags Library   OriginalBranch
  commitAndPush "${message}"
}

calculateBranchVersions() {

  # HotFix version
  hotFixMajor=`getVersionMajor "${hotFixVersion}"`
  hotFixMinor=`getVersionMinor "${hotFixVersion}"`
  hotFixRevision=`getVersionRevision "${hotFixVersion}"`
  if [[ "${hotFixVersion}" != "${hotFixMajor}.${hotFixMinor}.${hotFixRevision}" ]]
  then
    echo 'The <hotFixVersion> is invalid. Must contain 3 numbers separated by dots.'
    exit 1
  fi

  # ServicePack version
  servicePackMajor="${hotFixMajor}"
  servicePackMinor="${hotFixMinor}"
  servicePackRevision=`increment "${hotFixRevision}"`
  servicePackVersion="${servicePackMajor}.${servicePackMinor}.${servicePackRevision}"

  # Master version
  if [ -z "${masterVersion}" ]
  then
    masterMajor="${hotFixMajor}"
    masterMinor=`increment "${hotFixMinor}"`
    masterRevision="0"
    masterVersion="${masterMajor}.${masterMinor}.${masterRevision}"
  else
    masterMajor=`getVersionMajor "${masterVersion}"`
    masterMinor=`getVersionMinor "${masterVersion}"`
    masterRevision=`getVersionRevision "${masterVersion}"`
    if [[ "${masterVersion}" != "${masterMajor}.${masterMinor}.${masterRevision}" ]]
    then
      echo 'The <masterVersion> is invalid. Must contain 3 numbers separated by dots.'
      exit 1
    fi
  fi

  # Branches
  if [[ -n "${doTest}" ]]
  then
    masterBranch="master-test"
    hotFixBranch="release/test/${hotFixVersion}"
    servicePackBranch="release/test/${hotFixMajor}.${hotFixMinor}.N"
  else
    masterBranch="master"
    hotFixBranch="release/${hotFixVersion}"
    servicePackBranch="release/${hotFixMajor}.${hotFixMinor}.N"
  fi
}

createHotFixProjectBranches() {
  local hotFixVersion="${1}"
  local hotFixBranch="${2}"

  echo
  echo "${prefix}Create the HotFix branches"
  createProjectBranchesFromAcsVersion "${hotFixVersion}" "${hotFixBranch}" \
    "${hotFixVersion}" HotFix 0 \
    "Create HotFix branch for ${hotFixVersion}"
}

createServicePackProjectBranches() {
  local hotFixVersion="${1}"
  local servicePackBranch="${2}"
  local servicePackVersion="${3}"

  echo
  echo "${prefix}Create the ServicePack branches"
  createProjectBranchesFromAcsVersion "${hotFixVersion}" "${servicePackBranch}" \
    "${servicePackVersion}" ServicePack 100 \
    "Create ServicePack branch ${servicePackBranch}"
}

modifyOriginalProjectBranches() {
  local branchType="${1}"
  local branch="${2}"
  local version="${3}"
  local schemaMultiple="${4}"

  echo
  echo "${prefix}Modify the ${branchType} branches"
  modifyOriginalProjectBranchesForNextRelease "${branch}" \
    "${version}" "${branchType}" "${schemaMultiple}" \
    "Update ${branchType} branch to ${version}"
}

createAndModifyProjectBranches() {
  createHotFixProjectBranches "${hotFixVersion}" "${hotFixBranch}"
  if [[ "${hotFixRevision}" == "0" ]]
  then
    createServicePackProjectBranches "${hotFixVersion}" "${servicePackBranch}" "${servicePackVersion}"
    modifyOriginalProjectBranches Master "${masterBranch}" "${masterVersion}" 1000
  else
    modifyOriginalProjectBranches ServicePack "${servicePackBranch}" "${servicePackVersion}" 100
  fi
}

cleanUpTestBranches() {
  local project="${1}"

  echo "${prefix}Clean up ${hotFixBranch} and ${servicePackBranch} on ${project}"
  cd "${ROOT_DIR}/${project}/"
  git checkout .                       &>${loggingOut}
  git checkout "${masterBranch}"       &>${loggingOut}
  git branch -D "${hotFixBranch}"      &>${loggingOut}
  git branch -D "${servicePackBranch}" &>${loggingOut}
}

cleanUpTestProjectBranches() {
  if [[ -n "${doCleanup}" ]]
  then
    echo
    set +o errexit
    cleanUpTestBranches alfresco-community-repo
    cleanUpTestBranches alfresco-enterprise-repo
    cleanUpTestBranches alfresco-enterprise-share
    cleanUpTestBranches acs-community-packaging
    cleanUpTestBranches acs-packaging
    set -o errexit
  fi
}

readCommandLineArgs "$@"
calculateBranchVersions
cleanUpTestProjectBranches
createAndModifyProjectBranches
echo
echo Done
