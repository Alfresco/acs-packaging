#!/usr/bin/env bash
echo "=========================== Starting Build Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

source "$(dirname "${BASH_SOURCE[0]}")/build_functions.sh"

COM_DEPENDENCY_VERSION="$(retrievePomProperty "dependency.alfresco-community-repo.version")"
ENT_DEPENDENCY_VERSION="$(retrievePomProperty "dependency.alfresco-enterprise-repo.version")"

# Either both the parent and the upstream dependency are the same, or else fail the build
if [ "${ENT_DEPENDENCY_VERSION}" != "$(retrievePomParentVersion)" ]; then
  printf "Upstream dependency version (%s) is different then the project parent version!\n" "${ENT_DEPENDENCY_VERSION}"
  exit 1
fi

# Prevent merging of any SNAPSHOT dependencies into the master or the release/* branches
if [[ $(isPullRequestBuild) && ( "${COM_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ || "${ENT_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ) && "${TRAVIS_BRANCH}" =~ ^master$|^release/.+$ ]] ; then
  printf "PRs with SNAPSHOT dependencies are not allowed into master or release branches\n"
  exit 1
fi

# Prevent release jobs from starting when there are SNAPSHOT upstream dependencies
if [[ "${COM_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ || "${ENT_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] && [ "${TRAVIS_BUILD_STAGE_NAME,,}" = "release" ] ; then
  printf "Cannot release project with SNAPSHOT dependencies!\n"
  exit 1
fi

COM_UPSTREAM_REPO="github.com/Alfresco/alfresco-community-repo.git"
pullUpstreamTag "${COM_UPSTREAM_REPO}" "${COM_DEPENDENCY_VERSION}"

ENT_UPSTREAM_REPO="github.com/Alfresco/alfresco-enterprise-repo.git"
pullUpstreamTagAndBuildDockerImage "${ENT_UPSTREAM_REPO}" "${ENT_DEPENDENCY_VERSION}" "-PenterpriseDocker"

# Either both the parent and the upstream dependency are the same, or else fail the build
if [ "${COM_DEPENDENCY_VERSION}" != "$(evaluatePomProperty "project.parent.parent.version")" ]; then
  printf "Upstream dependency version (%s) is different then the project parent version!\n" "${COM_DEPENDENCY_VERSION}"
  exit 1
fi

# Build the current project
mvn -B -V -q install -DskipTests -Dmaven.javadoc.skip=true -PenterpriseDocker \
  $([[ "${ENT_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] && echo "-Drepo.image.tag=latest")


popd
set +vex
echo "=========================== Finishing Build Script =========================="

