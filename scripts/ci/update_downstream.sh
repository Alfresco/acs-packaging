#!/usr/bin/env bash
echo "=========================== Starting Update Downstream Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

source "$(dirname "${BASH_SOURCE[0]}")/build_functions.sh"

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

# Retrieve the release and development versions as they are normally the same in community packaging
RELEASE_VERSION=$(grep RELEASE_VERSION: .github/workflows/master_release.yml | sed 's/.*RELEASE_VERSION: \(.*\)/\1/')
DEVELOPMENT_VERSION=$(grep DEVELOPMENT_VERSION: .github/workflows/master_release.yml | sed 's/.*DEVELOPMENT_VERSION: \(.*\)/\1/')

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

sed -i "s/.*RELEASE_VERSION: .*/  RELEASE_VERSION: $RELEASE_VERSION/" .github/workflows/ci.yml
sed -i "s/.*DEVELOPMENT_VERSION: .*/  DEVELOPMENT_VERSION: $DEVELOPMENT_VERSION/" .github/workflows/ci.yml

set +e
echo "${COMMIT_MESSAGE}" | grep '\[publish\]'
if [ "$?" -eq 0 ]
then
  COMMIT_DIRECTIVES="[release][publish]"
else
  COMMIT_DIRECTIVES="[release]"
fi
set -e
# Commit changes
git status
git --no-pager diff pom.xml
git add pom.xml
git --no-pager diff pom.xml
git add .github/workflows/ci.yml

if git status --untracked-files=no --porcelain | grep -q '^' ; then
  git commit -m "${COMMIT_DIRECTIVES} ${VERSION}

  Update upstream versions
    - alfresco-community-repo:   ${COM_VERSION}
    - alfresco-enterprise-share: ${SHA_VERSION}
    - acs-packaging:             ${VERSION}
    - RELEASE_VERSION:           ${RELEASE_VERSION}
    - DEVELOPMENT_VERSION:       ${DEVELOPMENT_VERSION}"
  git push
else
  echo "Dependencies are already up to date."
  git status
fi


popd
set +vex
echo "=========================== Finishing Update Downstream Script =========================="

