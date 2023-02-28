#!/usr/bin/env bash
# Cut down version of newReleaseBranch.sh ti just update master

set -o errexit

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="${SCRIPT_DIR}/../../.."

usage() {
    cat << ???  1>&2;

Updates the version on the master branch.

Usage: $0 <masterVersion> <pomVersion> [-h] [-l] [-t] [-s]
  <masterVersion>: The new ACS version on master. Must contain 3 integers separated by dots.
  <pomVersion>: The next pom version to tagged in repo and share projects.
  -h: Display this help
  -l: Output extra logging
  -t: use test branches: release/test/master and release/test/X.x.x
  -s: skip the push to the remote git repository

Examples:
  1. Reset master to create ACS 7.3.0 and start with 16.1-SNAPSHOT as the pom version in share and repo projects
     $ $0 7.3.0 16.1
  2. Reset master to recreate ACS 23.1.0 and start with 23.1.0.100-SNAPSHOT as the pom version in share and repo projects
     23.1.0 has been on master before, so there are already tags like 23.1.0.37
     $ $0 7.3.0 23.1.0.100
???
}

readCommandLineArgs() {
  masterVersion="${1}"
  pomVersion="${2}"
  if [[ -z "${masterVersion}" || -z "${pomVersion}" ]]
  then
    usage
    exit 1
  fi
  shift 2

  doPush="true"
  doTest=""
  doCleanup=""
  loggingOut="/dev/null"
  prefix=""
  while getopts "stlh" arg; do
      case $arg in
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

setPomVersion() {
  local pomVersion="${1}"
  local profiles="${2}"

  echo "${prefix}    set pom version ${pomVersion} using profiles: ${profiles}"
  mvn versions:set -DnewVersion="${pomVersion}" -DgenerateBackupPoms=false -P"${profiles}" &>${loggingOut}
}

getCurrentProject() {
  local pwd=`pwd`
  basename ${pwd}
}

setAcsVersionLabelInEnterpriseRepo() {
  local version="${1}"

  if [[ `getCurrentProject` == "alfresco-enterprise-repo" ]]
  then
    echo "${prefix}    set <acs.version.label>"
    ed -s pom.xml &>${loggingOut} << EOF
/.*acs.version.label.*$/s//        <acs.version.label \/> <!-- ${version}.<acs.version.label> -->/
wq
EOF
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

  echo "${prefix}    set - RELEASE_VERSION=${version}"
  ed -s .github/workflows/master_release.yml &>${loggingOut} << EOF
/.*- RELEASE_VERSION.*$/s//    - RELEASE_VERSION=${version}/
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

  echo "${prefix}    set - DEVELOPMENT_VERSION=${version}"
  ed -s .github/workflows/master_release.yml &>${loggingOut} << EOF
/.*- DEVELOPMENT_VERSION.*$/s//    - DEVELOPMENT_VERSION=${version}/
wq
EOF
}

setVersionInPackaging() {
  local version="${1}"
  local projectType="${2}"

  if [[ "${projectType}" == "Packaging" ]]
  then
    setStartWithRealVersion   "${version}"
    setNextReleaseVersion     "${version}-A1"
    setNextDevelopmentVersion "${version}-SNAPSHOT"
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

checkout() {
  local project="${1}"
  local branch="${2}"

  echo "${prefix}  Checkout ${project} ${branch}"

  cd "${ROOT_DIR}/${project}/"
  git fetch                  &>${loggingOut}
  git checkout "${branch}"   &>${loggingOut}
}

modifyPomVersion() {
  local version="${1}"
  local profiles="${2}"
  local projectType="${3}"

  if [[ "${projectType}" == "Packaging" ]]
  then
    pomVersion="${version}"
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
  local profiles="${2}"
  local projectType="${3}"
  local newBranch="${4}"

  modifyPomVersion "${version}" "${profiles}" "${projectType}"
  setAcsVersionInCommunityRepo "${version}"
  setAgsTestVersionPropertiesInCommunityRepo "${version}"
  setAcsVersionLabelInEnterpriseRepo "${version}"
  setAcsVersionInShare "${version}"
  setVersionInPackaging "${version}" "${projectType}"
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

modifyOriginalProjectBranchesForNextRelease() {
  local branch="${1}"
  local version="${2}"
  local message="${3}"

  checkout acs-packaging             "${branch}"
  modifyProject "${version}" dev Packaging OriginalBranch
  commitAndPush "${message} [skip ci]"

  checkout alfresco-enterprise-share "${branch}"
  modifyProject "${version}" ags Library   OriginalBranch
  commitAndPush "${message} [skip ci]"

  checkout alfresco-enterprise-repo  "${branch}"
  modifyProject "${version}" ags Library   OriginalBranch
  commitAndPush "${message} [skip ci]"

  checkout alfresco-community-repo   "${branch}"
  modifyProject "${version}" ags Library   OriginalBranch
  commitAndPush "${message}"

  checkout acs-community-packaging   "${branch}"
  modifyProject "${version}" dev Packaging OriginalBranch
  commitAndPush "${message} [skip ci]"
}

calculateBranchVersions() {

  # Master version
  masterMajor=`getVersionMajor "${masterVersion}"`
  masterMinor=`getVersionMinor "${masterVersion}"`
  masterRevision=`getVersionRevision "${masterVersion}"`
  if [[ "${masterVersion}" != "${masterMajor}.${masterMinor}.${masterRevision}" ]]
  then
    echo 'The <masterVersion> is invalid. Must contain 3 integers separated by dots.'
    exit 1
  fi

  # Branches
  if [[ -n "${doTest}" ]]
  then
    masterBranch="release/test/master"
  else
    masterBranch="master"
  fi
}

modifyOriginalProjectBranches() {
  local branch="${1}"
  local version="${2}"

  echo
  echo "${prefix}Modify the Master branches"
  modifyOriginalProjectBranchesForNextRelease "${branch}" \
    "${version}" \
    "Update Master branch to ${version}"
}

main() {
  readCommandLineArgs "$@"
  calculateBranchVersions

  modifyOriginalProjectBranches "${masterBranch}" "${masterVersion}"

  echo
  echo Done
}

main "$@"