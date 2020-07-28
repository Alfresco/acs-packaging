#!/usr/bin/env bash
set -ev
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

# get the source branch name
[ "${TRAVIS_PULL_REQUEST}" = "false" ] && BRANCH="${TRAVIS_BRANCH}" || BRANCH="${TRAVIS_PULL_REQUEST_BRANCH}"

# if BRANCH is not 'master' or 'release/'
if ! [[ "${BRANCH}" =~ ^master$\|^release/.+$ ]] ; then
  # if BRANCH exists in the upstream project

  UPSTREAM_REPO="github.com/Alfresco/alfresco-community-repo.git"
  if git ls-remote --exit-code --heads https://${GIT_USERNAME}:${GIT_PASSWORD}@${UPSTREAM_REPO} "${BRANCH}" ; then
    # clone and build the upstream repository
    pushd ..

    rm -rf alfresco-community-repo
    git clone -b ${BRANCH} https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/Alfresco/alfresco-community-repo.git
    cd alfresco-community-repo
    mvn -B -V -q clean install -DskipTests -PcommunityDocker
    UPSTREAM_VERSION=$(mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout)

    popd
  fi

  UPSTREAM_REPO="github.com/Alfresco/alfresco-enterprise-repo.git"
  if [ ! -z ${UPSTREAM_VERSION} ] || \
    git ls-remote --exit-code --heads https://${GIT_USERNAME}:${GIT_PASSWORD}@${UPSTREAM_REPO} "${BRANCH}" ; then
    # clone and build the upstream repository
    pushd ..

    rm -rf alfresco-enterprise-repo
    git clone -b ${BRANCH} https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/Alfresco/alfresco-enterprise-repo.git
    cd alfresco-enterprise-repo
    # update the parent dependency if needed
    [ ! -z ${UPSTREAM_VERSION} ] && mvn -B versions:update-parent "-DparentVersion=(0,${UPSTREAM_VERSION}]" versions:commit
    mvn -B -V -q clean install -DskipTests -PenterpriseDocker
    UPSTREAM_VERSION=$(mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout)

    popd
  fi
fi

# update the parent dependency if needed
[ ! -z ${UPSTREAM_VERSION} ] && mvn -B versions:update-parent "-DparentVersion=(0,${UPSTREAM_VERSION}]" versions:commit

# Build the current project also
mvn -B -V -q install -DskipTests -PenterpriseDocker

