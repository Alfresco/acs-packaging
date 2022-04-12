#!/usr/bin/env bash
set -o errexit
#set -x

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="${SCRIPT_DIR}/../../.."
RELEASE_BRANCH_PREFIX=release

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

getVersionMajor() {
  echo "${1}" | sed -n "s/^\([0-9]*\)\.[0-9]*\.[0-9N]*$/\1/p"
}

getVersionMinor() {
  echo "${1}" | sed -n "s/^[0-9]*\.\([0-9]*\)\.[0-9N]*$/\1/p"
}

getVersionRevision() {
  echo "${1}" | sed -n "s/^[0-9]*\.[0-9]*\.\([0-9N]*\)$/\1/p"
}

increment() {
  local number="${1}"
  echo "${number} + 1" | bc
}

getVersionSchema() {
  # TODO
  echo 17002
}

setVersionSchema() {
  echo TODO setVersionSchema
}

incrementVersionSchema() {
  local version_multiple="${1}"
  local version_schema=`getVersionSchema`
  version_schema=`echo "(${version_schema} / ${schema_multiple} + 1) * ${schema_multiple}" | bc`
  setVersionSchema "${version_schema}"
}

# Read the command line arguments
hotfix_version="${1}"
if [ -z "${hotfix_version}" ]
then
  usage
  exit 0
fi

master_version=""
LOGGING_OUT="/dev/null"
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

# HotFix branch version
hotfix_major=`getVersionMajor "${hotfix_version}"`
hotfix_minor=`getVersionMinor "${hotfix_version}"`
hotfix_revision=`getVersionRevision "${hotfix_version}"`
if [[ "${hotfix_version}" != "${hotfix_major}.${hotfix_minor}.${hotfix_revision}" ]]
then
  echo 'The <hotfix_version> is invalid. Must contain 3 numbers separated by dots.'
  exit 1
fi

# ServicePack branch version
if [[ "${release_branch}" == "master"  ]]
then
  servicepack_revision="0"
else
  servicepack_revision=`increment "${hotfix_revision}"`
fi
servicepack_version="${hotfix_major}.${hotfix_minor}.${servicepack_revision}"

# Master branch version
if [ -z "${master_version}" ]
then
  master_major=`increment "${hotfix_minor}"`
  master_version="${hotfix_major}.${master_major}.0"
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

echo branch=$branch
echo hotfix_major=$hotfix_major
echo minor=$minor
echo hotfix_revision=$hotfix_revision
echo source_branch=$source_branch
echo version=$version
echo pom_version=$pom_version
echo schema_multiple=$schema_multiple
echo version_schema=$version_schema

#hotfix_version_schema=`getVersionSchema`
#servicepack_version_schema=`incrementVersionSchema "${hotfix_version_schema}" 100`

release_branch=master
hotfix_branch=release/23.1.0
servicepack_branch=release/23.1.N
servicepack_version=23.1.1
master_version=23.2.0

# Create the HotFix branch
forkBranch "${release_branch}" "${hotfix_version}" "${hotfix_branch}"
buildBranch

# Create or update the ServicePack branch
if [[ "${release_branch}" == "master"  ]]
then
  forkBranch "${hotfix_branch}" "${hotfix_version}" "${servicepack_branch}"
else
  checkoutBranch "${servicepack_branch}"
fi
setVersion "${servicepack_version}"
incrementVersionSchema 100
buildBranch

# Update the master branch
if [[ "${release_branch}" == "master"  ]]
then
  checkoutBranch master
  setVersion "${master_version}"
  incrementVersionSchema 1000
  buildBranch
fi

echo DONE