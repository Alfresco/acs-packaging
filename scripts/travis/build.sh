#!/usr/bin/env bash
set -ev
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

# get the source branch name
[ "${TRAVIS_PULL_REQUEST}" = "false" ] && BRANCH="${TRAVIS_BRANCH}" || BRANCH="${TRAVIS_PULL_REQUEST_BRANCH}"

# if BRANCH is not 'master' or 'release/'
if ! [[ "${BRANCH}" =~ ^master$\|^release/.+$ ]] ; then
  # if BRANCH exists in the upstream project

  if git ls-remote --exit-code --heads \
  https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/Alfresco/alfresco-community-repo.git \
  "${BRANCH}" ; then

    # clone and build the upstream repository
    pushd ..

    rm -rf alfresco-community-repo
    git clone -b ${BRANCH} https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/Alfresco/alfresco-community-repo.git
    cd alfresco-community-repo
    # todo set a custom branch version before the SNAPSHOT build?
    mvn -B -V clean install -DskipTests -PcommunityDocker

    popd
    # todo update the parent dependency dependency so that it uses the locally-build SNAPSHOT version?
  fi

  if git ls-remote --exit-code --heads \
  https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/Alfresco/alfresco-enterprise-repo.git \
  "${BRANCH}" ; then

    # clone and build the upstream repository
    pushd ..

    rm -rf alfresco-enterprise-repo
    git clone -b ${BRANCH} https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/Alfresco/alfresco-enterprise-repo.git
    cd alfresco-enterprise-repo
    # todo set a custom branch version before the SNAPSHOT build?
    mvn -B -V clean install -DskipTests -PenterpriseDocker

    popd
    # todo update the parent dependency dependency so that it uses the locally-build SNAPSHOT version?
  fi
fi

mvn -B -V -q install -DskipTests -PenterpriseDocker

