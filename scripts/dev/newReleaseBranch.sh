#!/usr/bin/env bash
set -o errexit
#set -x

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="${SCRIPT_DIR}/../../.."
RELEASE_BRANCH_PREFIX=release

usage() {
    cat << ???  1>&2;

Creates HotFix or ServicePack branches for repo, share and packaging projects or just updates the master
or ServicePack branches ready for the next release.

Usage: $0 <branch> [-s <source_branch>] [-v <version>] [-p <pom_version>] [-h] [-l]
  -s <source_branch>: Source branch when creating a new branch
  -v <version>: An override for the ACS version - must contain 3 numbers separated by dots.
                In 'release/M.m.r' branches, defaults to the value in the branch. An N becomes a 1 or a 2.
  -p <pom_version>: An override for the base pom version to be used in repo and share projects.
                Only needed in ACS versions < 23.1 where it is not the same as the <version>.
                The initial pom versions have .1-SNAPSHOT appended.

  -h: Display this help
  -l: Output extra logging

Examples:

  1. After the release of 23.1.0 from the master branch:
     Create the HotFix branch for 23.1.0
     Create the 23.1.N ServicePack branch. The initial version will be 23.1.1
     Modify the master branch for use by the next major or minor version 23.2.0.
     $ $0 release/23.1.0 -s master
     $ $0 release/23.1.N -s master -v 23.1.1
     $ $0 master -v 23.2

  2. After the release of 23.1.1 from the ServicePack branch release/23.1.N:
     Create the HotFix branch for 23.1.1
     Modify the ServicePack branch for use by the next version 23.1.2
     $ $0 release/23.1.1 -s release/23.1.N
     $ $0 release/23.1.N -v 23.1.2

  3. After the release of 23.1.2 from the ServicePack branch release/23.1.N
     $ $0 release/23.1.2 -s release/23.1.N
     $ $0 release/23.1.N -v 23.1.3

  4. After the release of 7.2.1 from the release/7.2.N ServicePack branch.

     Create the HotFix branch for 7.2.1
     Modify the ServicePack branch for use by the next version 7.2.2. Needs the -p flag as the pom and ACS version do not match before ACS 23.1
     $ $0 release/7.2.1 -s release/7.2.N
     $ $0 release/7.2.N -v 7.2.2 -p 16
???
}

getVersionMajor() {
  echo "${1}" | sed -n "s/^${RELEASE_BRANCH_PREFIX}\/\([0-9]*\)\.[0-9]*\.[^.]*$/\1/p"
}

getVersionMinor() {
  echo "${1}" | sed -n "s/^${RELEASE_BRANCH_PREFIX}\/[0-9]*\.\([0-9]*\)\.[^.]*$/\1/p"
}

getVersionRevision() {
  echo "${1}" | sed -n "s/^${RELEASE_BRANCH_PREFIX}\/[0-9]*\.[0-9]*\.\([^.]*\)$/\1/p"
}

branch="$1"
source_branch=""
version=""
LOGGING_OUT="/dev/null"
while getopts "s:v:p:lh" arg; do
    case $arg in
        s)
            source_branch=${OPTARG}
            ;;
        v)
            version=${OPTARG}
            ;;
        p)
            pom_version=${OPTARG}
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

branch_major=`getVersionMajor "${branch}"`
branch_minor=`getVersionMinor "${branch}"`
branch_revision=`getVersionRevision "${branch}"`


#if [ ${branch_revision} -eq "N" ]
#then
#  if [ ${branch_major} >= 23]
#  then
#     acs_revision=1
#   else
#     acs_revision=2
#   fi
#else
#  acs_revision=${branch_revision}
#fi
version=${branch_major}.${branch_minor}.${acs_revision}


schema_multiple=111

echo branch=$branch
echo source_branch=$source_branch
echo version=$version

echo branch_major=$branch_major
echo branch_minor=$branch_minor
echo branch_revision=$branch_revision

echo END