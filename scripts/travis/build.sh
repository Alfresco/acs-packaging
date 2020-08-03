#!/usr/bin/env bash
set -ev
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

# reset variables to blank values
unset UPSTREAM_VERSION_COMM
unset UPSTREAM_VERSION_ENT

# get the source branch name
[ "${TRAVIS_PULL_REQUEST}" = "false" ] && BRANCH="${TRAVIS_BRANCH}" || BRANCH="${TRAVIS_PULL_REQUEST_BRANCH}"

# if BRANCH is 'master' or 'release/'
UPSTREAM_REPO="github.com/Alfresco/alfresco-community-repo.git"
if [[ "${BRANCH}" =~ ^master$\|^release/.+$ ]] ; then
  # clone the upstream repository tag
  pushd ..

  TAG=$(mvn -B -q help:evaluate -Dexpression=project.parent.parent.version -DforceStdout)
  git clone -b "${TAG}" --depth=1 "https://${GIT_USERNAME}:${GIT_PASSWORD}@${UPSTREAM_REPO}"

  popd
else
  # if BRANCH is a feature branch AND if it exists in the upstream project
  if git ls-remote --exit-code --heads "https://${GIT_USERNAME}:${GIT_PASSWORD}@${UPSTREAM_REPO}" "${BRANCH}" ; then
    # clone and build the upstream repository
    pushd ..

    rm -rf alfresco-community-repo
    git clone -b "${BRANCH}" --depth=1 "https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/Alfresco/alfresco-community-repo.git"
    cd alfresco-community-repo
    mvn -B -V -q clean install -DskipTests -Dmaven.javadoc.skip=true -PcommunityDocker
    mvn -B -V install -f packaging/tests/pom.xml -DskipTests
    UPSTREAM_VERSION_COMM="$(mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout)"

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
    mvn -B -V -q clean install -DskipTests -Dmaven.javadoc.skip=true -PenterpriseDocker \
      $(test -n "${UPSTREAM_VERSION_COMM}" && echo "-Ddependency.alfresco-community-repo.version=${UPSTREAM_VERSION_COMM}")
    mvn -B -V install -f packaging/tests/pom.xml -DskipTests \
      $(test -n "${UPSTREAM_VERSION_COMM}" && echo "-Ddependency.alfresco-community-repo.version=${UPSTREAM_VERSION_COMM}")

    UPSTREAM_VERSION_ENT="$(mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout)"

    popd
  fi
fi

# update the parent dependency if needed
if [ -n "${UPSTREAM_VERSION_ENT}" ]; then
  mvn -B versions:update-parent "-DparentVersion=(0,${UPSTREAM_VERSION_ENT}]" versions:commit
fi

# Build the current project also
mvn -B -V -q install -DskipTests -PenterpriseDocker \
  $(test -n "${UPSTREAM_VERSION_COMM}" && echo "-Ddependency.alfresco-community-repo.version=${UPSTREAM_VERSION_COMM}") \
  $(test -n "${UPSTREAM_VERSION_ENT}"  && echo "-Ddependency.alfresco-enterprise-repo.version=${UPSTREAM_VERSION_ENT}") \
  $(test -n "${UPSTREAM_VERSION_ENT}"  && echo "-Dupstream.image.tag=latest")



