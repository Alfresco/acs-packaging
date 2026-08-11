#!/usr/bin/env bash
echo "=========================== Starting Update Downstream Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

source "$(dirname "${BASH_SOURCE[0]}")/build_functions.sh"

if [ -z "${RELEASE_VERSION}" ]; then
  echo "ERROR: RELEASE_VERSION environment variable is not set."
  exit 1
fi
if [ -z "${DEVELOPMENT_VERSION}" ]; then
  echo "ERROR: DEVELOPMENT_VERSION environment variable is not set."
  exit 1
fi

#Fetch the latest changes, as GHA will only checkout the PR commit
git fetch origin "${BRANCH_NAME}"
git checkout "${BRANCH_NAME}"
git pull

# Retrieve the latest (just released) latest tag on the current branch
VERSION="$(git describe --abbrev=0 --tags)"

# Retrieve the Community Repo version
COM_VERSION="$(evaluatePomProperty "dependency.alfresco-community-repo.version")"

# Retrieve the Enterprise Share version
SHA_VERSION="$(evaluatePomProperty "dependency.alfresco-enterprise-share.version")"

DOWNSTREAM_REPO="github.com/Alfresco/acs-community-packaging.git"

cloneRepo "${DOWNSTREAM_REPO}" "${BRANCH_NAME}"

cd "$(dirname "${BASH_SOURCE[0]}")/../../../$(basename "${DOWNSTREAM_REPO%.git}")"

# Update parent version
mvn -B versions:update-parent versions:commit "-DparentVersion=[${COM_VERSION}]"

# Update dependency version
mvn -B versions:set-property versions:commit \
  -Dproperty=dependency.alfresco-community-repo.version \
  "-DnewVersion=${COM_VERSION}"

mvn -B versions:set-property versions:commit \
  -Dproperty=dependency.alfresco-community-share.version \
  "-DnewVersion=${SHA_VERSION}"

mvn -B versions:set-property versions:commit \
  -Dproperty=dependency.acs-packaging.version \
  "-DnewVersion=${VERSION}"

sed -i "s|- RELEASE_VERSION=.*|- RELEASE_VERSION=${RELEASE_VERSION}|g" .github/release-versions.yml
sed -i "s|- DEVELOPMENT_VERSION=.*|- DEVELOPMENT_VERSION=${DEVELOPMENT_VERSION}|g" .github/release-versions.yml

echo "version=${VERSION}" >> "$GITHUB_OUTPUT"


popd
set +vex
echo "=========================== Finishing Update Downstream Script =========================="
