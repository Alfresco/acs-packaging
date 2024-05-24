#!/usr/bin/env bash

set -o errexit

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="${SCRIPT_DIR}/../../.."

usage() {
    cat << ???  1>&2;

Creates HotFix and ServicePack (.N) branches for repo, share and packaging projects, and updates the master
or ServicePack branches ready for the next release.

Usage: $0 <releasedVersion> [-v <masterVersion>] [-h] [-l] [-t] [-c] [-s]
  <releasedVersion>:  The version just released. Used in the new HotFix Branch name release/<releasedVersion>.
                      Must contain 3 integers separated by dots.
  -v <masterVersion>: Overrides the next development version on master so that the major version may be changed.
                      Ignored if the master branch is not going to be modified. Must contain 3 integers separated by dots.
  -h: Display this help
  -l: Output extra logging
  -t: use test branches: release/test/master and release/test/X.x.x
  -c: cleanup (delete) local test release/test/X.x.x branches that are about to be created or modified.
  -s: skip the push to the remote git repository

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

  4. Switching to the next major version:
     $ $0 23.2.0 -v 25.1.0
     Creates the HotFix branch for 23.2.0, creates the 23.2.N ServicePack branch and modifies the master branch for
     use by the next Hot Fix major version 25.1.0.
???
}

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
  while getopts "v:stclh" arg; do
      case $arg in
          v)
              masterVersion=${OPTARG}
              ;;
          l)
              loggingOut=`tty`
              prefix="== "
              ;;
          t)
              doTest="true"
              ;;
          c)
              doCleanup="true"
              ;;
          s)
              doPush=""
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

  echo "${prefix}    set pom version ${pomVersion} using profiles: ${profiles}"
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
    echo "${prefix}    set <scm><tag> ${tag}"
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

setAcsVersionLabelInEnterpriseRepo() {
  local version="${1}"
  local branchType="${2}"

  if [[ `getCurrentProject` == "alfresco-enterprise-repo" ]]
  then
    if [[ "${branchType}" == "HotFix" ]]
    then
      echo "${prefix}    set <acs.version.label>"
      ed -s pom.xml &>${loggingOut} << EOF
/.*acs.version.label.*$/s//        <acs.version.label>.1<\/acs.version.label> <!-- ${version}.<acs.version.label> -->/
wq
EOF
    else
      echo "${prefix}    set <acs.version.label>"
      ed -s pom.xml &>${loggingOut} << EOF
/.*acs.version.label.*$/s//        <acs.version.label \/> <!-- ${version}.<acs.version.label> -->/
wq
EOF
    fi
  fi
}

setAcsVersionInCommunityRepo() {
  local version="${1}"

  if [[ `getCurrentProject` == "alfresco-community-repo" ]]
  then
    local versionMajor=`getVersionMajor "${version}"`
    local versionMinor=`getVersionMinor "${version}"`
    local versionRevision=`getVersionRevision "${version}"`
    echo "${prefix}    set <acs.version.major> <acs.version.minor> <acs.version.revision> ${versionMajor} ${versionMinor} ${versionRevision}"
    ed -s pom.xml &>${loggingOut} << EOF
/.*acs.version.major.*$/s//        <acs.version.major>${versionMajor}<\/acs.version.major>/
/.*acs.version.minor.*$/s//        <acs.version.minor>${versionMinor}<\/acs.version.minor>/
/.*acs.version.revision.*$/s//        <acs.version.revision>${versionRevision}<\/acs.version.revision>/
wq
EOF
  fi
}

# Tried using maven properties in this test file, but they are not currently being replaced by the build, so need to
# edit them in this script for now.
setAgsTestVersionPropertiesInCommunityRepo() {
  local version="${1}"

  if [[ `getCurrentProject` == "alfresco-community-repo" ]]
  then
    local versionMajor=`getVersionMajor "${version}"`
    local versionMinor=`getVersionMinor "${version}"`
    local versionRevision=`getVersionRevision "${version}"`
    echo "${prefix}    set AGS test version.properties: version.major version.minor version.revision ${versionMajor} ${versionMinor} ${versionRevision}"
    ed -s amps/ags/rm-community/rm-community-repo/test/resources/alfresco/version.properties &>${loggingOut} << EOF
/.*version.major.*$/s//version.major=${versionMajor}/
/.*version.minor.*$/s//version.minor=${versionMinor}/
/.*version.revision.*$/s//version.revision=${versionRevision}/
wq
EOF
  fi
}

setAcsVersionInShare() {
  local version="${1}"

  if [[ `getCurrentProject` == "alfresco-enterprise-share" ]]
  then
    local versionMajor=`getVersionMajor "${version}"`
    local versionMinor=`getVersionMinor "${version}"`
    local versionRevision=`getVersionRevision "${version}"`
    echo "${prefix}    set <version.major> <version.minor> <version.revision> ${versionMajor} ${versionMinor} ${versionRevision}"
    ed -s pom.xml &>${loggingOut} << EOF
/.*version.major.*$/s//        <version.major>${versionMajor}<\/version.major>/
/.*version.minor.*$/s//        <version.minor>${versionMinor}<\/version.minor>/
/.*version.revision.*$/s//        <version.revision>${versionRevision}<\/version.revision>/
wq
EOF
  fi
}

setNextReleaseVersion() {
  local version="${1}"

  FILE_NAME=".github/workflows/master_release.yml"
  PWD_BASENAME=$(basename $PWD)
  if [ "$PWD_BASENAME" == "acs-community-packaging" ]; then
    FILE_NAME=".github/workflows/ci.yml"
  fi
  echo FILE_NAME: "$FILE_NAME"

  echo "${prefix}    set  RELEASE_VERSION=${version}"
  ed -s $FILE_NAME &>${loggingOut} << EOF
/.* RELEASE_VERSION.*$/s//  RELEASE_VERSION:${version}/
wq
EOF
}

setStartWithRealVersion() {
  local version="${1}"

  echo "${prefix}    set # ... start with real version ${version}"
  ed -s .github/workflows/master_release.yml &>${loggingOut} << EOF
/.*start with real version.*$/s//    # Release version has to start with real version (${version}-....) for the docker image to build successfully./
wq
EOF
}

setNextDevelopmentVersion() {
  local version="${1}"

  FILE_NAME=".github/workflows/master_release.yml"
  PWD_BASENAME=$(basename $PWD)
  if [ "$PWD_BASENAME" == "acs-community-packaging" ]; then
    FILE_NAME=".github/workflows/ci.yml"
  fi
  echo FILE_NAME: "$FILE_NAME"

  echo "${prefix}    set  DEVELOPMENT_VERSION=${version}"
  ed -s $FILE_NAME &>${loggingOut} << EOF
/.* DEVELOPMENT_VERSION.*$/s//  DEVELOPMENT_VERSION:${version}/
wq
EOF
}

setVersionInPackaging() {
  local version="${1}"
  local branchType="${2}"
  local projectType="${3}"

  if [[ "${projectType}" == "Packaging" ]]
  then
    if [[ "${branchType}" == "HotFix" ]]
    then
      setNextReleaseVersion     "${version}.1"
      setNextDevelopmentVersion "${version}.2-SNAPSHOT"
    else
      setStartWithRealVersion   "${version}"
      setNextReleaseVersion     "${version}-A1"
      setNextDevelopmentVersion "${version}-SNAPSHOT"
    fi
  fi
}

setUpstreamVersionsInCommunityPackaging() {
  local nextCommunityRepoVersion="${1}"
  local nextEnterpriseShareVersion="${2}"

  if [[ `getCurrentProject` == "acs-community-packaging" ]]
  then
    echo "${prefix}    set parent pom <version> ${nextCommunityRepoVersion}"
    echo "${prefix}    set <dependency.alfresco-community-repo.version> ${nextCommunityRepoVersion}"
    echo "${prefix}    set <dependency.alfresco-community-share.version> ${nextEnterpriseShareVersion}"
    # Do not change the <dependency.acs-packaging.version> as we will need a version of the share distribution that
    # exists, if we want to do a test build. acs-packaging will update it when a release is made.
    ed -s pom.xml &>${loggingOut} << EOF
/        <version>.*$/s//        <version>${nextCommunityRepoVersion}<\/version>/
/.*dependency.alfresco-community-repo.version.*$/s//        <dependency.alfresco-community-repo.version>${nextCommunityRepoVersion}<\/dependency.alfresco-community-repo.version>/
/.*dependency.alfresco-community-share.version.*$/s//        <dependency.alfresco-community-share.version>${nextEnterpriseShareVersion}<\/dependency.alfresco-community-share.version>/
wq
EOF
  fi
}

# Each version of the Repo has a schema number stored in it. When we need to patch something, generally the database,
# we increment the schema by one and create a patch with that number. As a result it is possible for the repo to do the
# patching on startup. So that we have enough space between versions each major or minor version uses the next multiple
# of 1000 and each service pack uses the next multiple of 100. Actually patches don't have to have unique numbers, but
# it does make it simpler to understand what is going to happen. The following code does this calculation with 1000 or
# 100 being passed in as an argument.
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

  echo "${prefix}  Create ${project} branch ${branch} from tag ${tag}"

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
    if [[ "${branchType}" == "HotFix" ]]
    then
      pomVersion="${version}.1"
    else
      pomVersion="${version}"
    fi
  else
    local versionMajor=`getVersionMajor "${version}"`
    if [[ "${versionMajor}" -lt 23 ]]
    then
      pomMajor=`getPomMajor`
      if [[ "${branchType}" == "HotFix" ]]
      then
        pomMinor=`getPomMinor`
        pomMinor=`increment "${pomMinor}"`
        pomVersion="${pomMajor}.${pomMinor}"
      else
        if [[ "${branchType}" == "Master" ]]
        then
          # Allow for two service packs before the next release off master.
          # Prior to 23.1, the pom version of repo and share projects had a fixed number followed by a dot and an
          # incrementing number. For example ACS 7.0.0 used 8.N, ACS 7.1.0 used 11.N, ACS 7.2.0 used 14.N. To allow
          # for a couple of service packs between these it would jump two numbers. ACS 7.1.1 used 12.N. Had there been
          # an ACS 7.1.2 it would have used 13.N. Not that we have done it for years, if there was a need for a third
          # service pack we would have had to do something special and go down another level. For example ACS 7.1.3
          # might have used 13.300.N
          pomMajor=`increment "${pomMajor}"`
          pomMajor=`increment "${pomMajor}"`
        fi
        pomMajor=`increment "${pomMajor}"`
        pomVersion="${pomMajor}.1"
      fi
    else
      if [[ "${branchType}" == "HotFix" ]]
      then
        pomLabel=`getPomLabel`
        pomLabel=`increment "${pomLabel}"`
      else
        pomLabel="1"
      fi
      pomVersion="${version}.${pomLabel}"
    fi
  fi

  if [[ `getCurrentProject` == "alfresco-community-repo" ]]
  then
    nextCommunityRepoVersion="${pomVersion}"
  elif [[ `getCurrentProject` == "alfresco-enterprise-share" ]]
  then
    nextEnterpriseShareVersion="${pomVersion}"
  fi

  pomVersion="${pomVersion}-SNAPSHOT"
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
  incrementSchema "${schemaMultiple}"
  setAcsVersionInCommunityRepo "${version}"
  setAgsTestVersionPropertiesInCommunityRepo "${version}"
  setAcsVersionLabelInEnterpriseRepo "${version}" "${branchType}"
  setAcsVersionInShare "${version}"
  setVersionInPackaging "${version}" "${branchType}" "${projectType}"
  setUpstreamVersionsInCommunityPackaging "${nextCommunityRepoVersion}" "${nextEnterpriseShareVersion}"
}

commitAndPush() {
  local message="${1}"

  echo "${prefix}    git commit"
  git commit --all -m "${message}" &>${loggingOut}
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

  createBranchFromTag acs-community-packaging   "${hotFixVersion}"        "${branch}"
  modifyProject "${version}"                    "${branchType}"           "${schemaMultiple}"       dev Packaging NewBranch
  commitAndPush "${message} [skip ci]"
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

  checkout alfresco-enterprise-share "${branch}"
  modifyProject "${version}" "${branchType}" "${schemaMultiple}" ags Library   OriginalBranch
  commitAndPush "${message} [skip ci]"

  checkout alfresco-enterprise-repo  "${branch}"
  modifyProject "${version}" "${branchType}" "${schemaMultiple}" ags Library   OriginalBranch
  commitAndPush "${message} [skip ci]"

  checkout alfresco-community-repo   "${branch}"
  modifyProject "${version}" "${branchType}" "${schemaMultiple}" ags Library   OriginalBranch
  commitAndPush "${message} [skip ci]"

  checkout acs-community-packaging   "${branch}"
  modifyProject "${version}" "${branchType}" "${schemaMultiple}" dev Packaging OriginalBranch
  commitAndPush "${message} [skip ci]"
}

calculateBranchVersions() {

  # HotFix version
  hotFixMajor=`getVersionMajor "${hotFixVersion}"`
  hotFixMinor=`getVersionMinor "${hotFixVersion}"`
  hotFixRevision=`getVersionRevision "${hotFixVersion}"`
  if [[ "${hotFixVersion}" != "${hotFixMajor}.${hotFixMinor}.${hotFixRevision}" ]]
  then
    echo 'The <releasedVersion> is invalid. Must contain 3 integers separated by dots.'
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
      echo 'The <masterVersion> is invalid. Must contain 3 integers separated by dots.'
      exit 1
    fi
  fi

  # Branches
  if [[ -n "${doTest}" ]]
  then
    masterBranch="release/test/master"
    hotFixBranch="release/test/${hotFixVersion}"
    servicePackBranch="release/test/${hotFixMajor}.${hotFixMinor}.N"
  else
    masterBranch="master"
    hotFixBranch="release/${hotFixVersion}"
    servicePackBranch="release/${hotFixMajor}.${hotFixMinor}.N"
  fi
}

cleanUpTestBranches() {
  local project="${1}"

  if [[ "${hotFixRevision}" == "0" ]]
  then
    echo "${prefix}Clean up ${hotFixBranch} and ${servicePackBranch} on ${project}"
    cd "${ROOT_DIR}/${project}/"
    git checkout .                         &>${loggingOut}
    git checkout  "${masterBranch}"      &>${loggingOut}
    git branch -D "${hotFixBranch}"      &>${loggingOut}
    git branch -D "${servicePackBranch}" &>${loggingOut}
  else
    echo "${prefix}Clean up ${hotFixBranch} on ${project}"
    cd "${ROOT_DIR}/${project}/"
    git checkout .                         &>${loggingOut}
    git checkout  "${servicePackBranch}" &>${loggingOut}
    git branch -D "${hotFixBranch}"      &>${loggingOut}
  fi
}

cleanUpTestProjectBranches() {

  if [[ -n "${doCleanup}" && -n "${doTest}" ]]
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
#  createHotFixProjectBranches "${hotFixVersion}" "${hotFixBranch}"
  if [[ "${hotFixRevision}" == "0" ]]
  then
    createServicePackProjectBranches "${hotFixVersion}" "${servicePackBranch}" "${servicePackVersion}"
    modifyOriginalProjectBranches Master "${masterBranch}" "${masterVersion}" 1000
  else
    modifyOriginalProjectBranches ServicePack "${servicePackBranch}" "${servicePackVersion}" 100
  fi
}

main() {
  readCommandLineArgs "$@"
  calculateBranchVersions

  cleanUpTestProjectBranches

  createAndModifyProjectBranches

  echo
  echo Done
}

main "$@"