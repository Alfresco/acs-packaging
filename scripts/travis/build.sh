#!/usr/bin/env bash
set -ev
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

# get the source branch name
[ "${TRAVIS_PULL_REQUEST}" = "false" ] && BRANCH="${TRAVIS_BRANCH}" || BRANCH="${TRAVIS_PULL_REQUEST_BRANCH}"

# if BRANCH is not 'master' or 'release/'
if ! [[ "${BRANCH}" =~ ^master$\|^release/.+$ ]] ; then
  # if BRANCH exists in the upstream project

  UPSTREAM_REPO="github.com/Alfresco/alfresco-community-repo.git"
  if git ls-remote --exit-code --heads "https://${GIT_USERNAME}:${GIT_PASSWORD}@${UPSTREAM_REPO}" "${BRANCH}" ; then
    # clone and build the upstream repository
    pushd ..

    rm -rf alfresco-community-repo
    git clone -b "${BRANCH}" --depth=1 "https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/Alfresco/alfresco-community-repo.git"
    cd alfresco-community-repo
    mvn -B -V -q clean install -DskipTests -Dmaven.javadoc.skip=true -PcommunityDocker
    mvn -B -V install -f packaging/tests/pom.xml -DskipTests
    UPSTREAM_VERSION_COMM=$(mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout)

    popd
  fi

  UPSTREAM_REPO="github.com/Alfresco/alfresco-enterprise-repo.git"
  if [ -n "${UPSTREAM_VERSION_COMM}" ] || \
    git ls-remote --exit-code --heads "https://${GIT_USERNAME}:${GIT_PASSWORD}@${UPSTREAM_REPO}" "${BRANCH}" ; then
    # clone and build the upstream repository
    pushd ..

    rm -rf alfresco-enterprise-repo
    git clone -b "${BRANCH}" --depth=1 "https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/Alfresco/alfresco-enterprise-repo.git"
    cd alfresco-enterprise-repo
    # update the parent dependency if needed
    [ -n "${UPSTREAM_VERSION_COMM}" ] && mvn -B versions:update-parent "-DparentVersion=(0,${UPSTREAM_VERSION_COMM}]" versions:commit
    mvn -B -V -q clean install -DskipTests -Dmaven.javadoc.skip=true -PenterpriseDocker
    mvn -B -V install -f packaging/tests/pom.xml -DskipTests
    UPSTREAM_VERSION_ENT=$(mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout)

    popd
  fi
fi

# update the parent dependency if needed
[ -n "${UPSTREAM_VERSION_ENT}" ] && mvn -B versions:update-parent "-DparentVersion=(0,${UPSTREAM_VERSION_ENT}]" versions:commit

# Build the current project also
mvn -B -V -q install -DskipTests -PenterpriseDocker \
  $(test -n "${UPSTREAM_VERSION_ENT}" && echo "-Ddependency.alfresco-enterprise-repo.version=${UPSTREAM_VERSION_ENT}") \
  $(test -n "${UPSTREAM_VERSION_COMM}" && echo "-Ddependency.alfresco-community-repo.version=${UPSTREAM_VERSION_COMM}")



