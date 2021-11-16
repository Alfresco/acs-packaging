#!/usr/bin/env bash
echo "=========================== Starting Update Single Pipeline Reference ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

source "$(dirname "${BASH_SOURCE[0]}")/build_functions.sh"

#Fetch the latest changes, as Travis will only checkout the PR commit
git fetch origin "${TRAVIS_BRANCH}"
git checkout "${TRAVIS_BRANCH}"
git pull

# Retrieve the ACS VERSION version
MAJOR="$(evaluatePomProperty "acs.version.major")"
MINOR="$(evaluatePomProperty "acs.version.minor")"
REVISION="$(evaluatePomProperty "acs.version.revision")"

DOWNSTREAM_REPO="github.com/Alfresco/terraform-alfresco-pipeline.git"

cloneRepo "${DOWNSTREAM_REPO}" develop

cd "$(dirname "${BASH_SOURCE[0]}")/../../../$(basename "${DOWNSTREAM_REPO%.git}")"

CONFIG_FILE=flux-configuration/develop/flux_configuration.yaml
sed -i "s/\(.*repository: regex:\).*\((.*).*\)/\1^${MAJOR}\.${MINOR}\.${REVISION}\2/" "${CONFIG_FILE}"
sed -i "s/\(.*share: regex:\).*\((.*).*\)/\1^${MAJOR}\.${MINOR}\.${REVISION}\2/" "${CONFIG_FILE}"

# Commit changes
git status
git --no-pager diff "${CONFIG_FILE}"
git add "${CONFIG_FILE}"

if git status --untracked-files=no --porcelain | grep -q '^' ; then
  git commit -m "Auto-update repository/share: ${MAJOR}.${MINOR}.${REVISION}

   [skip ci]"
  git push
else
  echo "Dependencies are already up to date."
  git status
fi

popd
set +vex
echo "=========================== Finishing Update Single Pipeline Reference =========================="

