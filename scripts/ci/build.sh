#!/usr/bin/env bash
echo "=========================== Starting Build Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

source "$(dirname "${BASH_SOURCE[0]}")/build_functions.sh"

usage() {
    echo "Builds the upstream projects first, then the current one." 1>&2;
    echo 1>&2;
    echo "Usage: $0 [-m]" 1>&2;
    echo "  -m: Flag to build Docker images with multi-architecture" 1>&2;
    echo "  -h: Display the usage information" 1>&2;
    exit 1;
}

while getopts "mh" option; do
   case $option in
      m)
        DOCKER_BUILD_PROFILE=build-multiarch-docker-images
        ;;
      h)
        usage
        ;;
   esac
done

BUILD_PROFILE=${DOCKER_BUILD_PROFILE:-build-docker-images}

ENT_DEPENDENCY_VERSION="$(retrievePomProperty "dependency.alfresco-enterprise-repo.version")"
REPO_IMAGE=$([[ "${ENT_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] && echo "-Drepo.image.tag=latest" || echo)

# Either both the parent and the upstream dependency are the same, or else fail the build
if [ "${ENT_DEPENDENCY_VERSION}" != "$(retrievePomParentVersion)" ]; then
  printf "Upstream dependency version (%s) is different then the project parent version!\n" "${ENT_DEPENDENCY_VERSION}"
  exit 1
fi

# Prevent merging of any SNAPSHOT dependencies into the master or the release/* branches
if [[ $(isPullRequestBuild) && "${ENT_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ && "${BRANCH_NAME}" =~ ^master$|^release/.+$ ]] ; then
  printf "PRs with SNAPSHOT dependencies are not allowed into master or release branches\n"
  exit 1
fi

# Prevent release jobs from starting when there are SNAPSHOT upstream dependencies
if [[ "${ENT_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] && [ "${JOB_NAME,,}" = "release" ] ; then
  printf "Cannot release project with SNAPSHOT dependencies!\n"
  exit 1
fi

ENT_UPSTREAM_REPO="github.com/Alfresco/alfresco-enterprise-repo.git"

# Checkout the upstream alfresco-enterprise-repo project (tag or branch)
if [[ "${ENT_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] ; then
  pullSameBranch "${ENT_UPSTREAM_REPO}"
else
  pullUpstreamTag "${ENT_UPSTREAM_REPO}" "${ENT_DEPENDENCY_VERSION}"
fi

COM_DEPENDENCY_VERSION="$(retrievePomProperty "dependency.alfresco-community-repo.version" "${ENT_UPSTREAM_REPO}")"

# Either both the parent and the upstream dependency are the same, or else fail the build
if [ "${COM_DEPENDENCY_VERSION}" != "$(retrievePomParentVersion "${ENT_UPSTREAM_REPO}")" ]; then
  printf "Upstream dependency version (%s) is different then the project parent version!\n" "${COM_DEPENDENCY_VERSION}"
  exit 1
fi

# Prevent merging of any SNAPSHOT dependencies into the master or the release/* branches
if [[ $(isPullRequestBuild) && "${COM_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ && "${BRANCH_NAME}" =~ ^master$|^release/.+$ ]] ; then
  printf "PRs with SNAPSHOT dependencies are not allowed into master or release branches\n"
  exit 1
fi

# Prevent release jobs from starting when there are SNAPSHOT upstream dependencies
if [[ "${COM_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] && [ "${JOB_NAME,,}" = "release" ] ; then
  printf "Cannot release project with SNAPSHOT dependencies!\n"
  exit 1
fi

COM_UPSTREAM_REPO="github.com/Alfresco/alfresco-community-repo.git"

# Checkout the upstream alfresco-community-repo project (tag or branch; + build if the latter)
if [[ "${COM_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] ; then
  pullAndBuildSameBranchOnUpstream "${COM_UPSTREAM_REPO}" "-Pags -Pall-tas-tests -Dlicense.failOnNotUptodateHeader=true"
else
  pullUpstreamTag "${COM_UPSTREAM_REPO}" "${COM_DEPENDENCY_VERSION}"
fi

# Build the upstream alfresco-enterprise-repo project with its docker image
if [[ "${ENT_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] ; then
  buildSameBranchOnUpstream "${ENT_UPSTREAM_REPO}" "-P$BUILD_PROFILE -Pags -Pall-tas-tests -Dlicense.failOnNotUptodateHeader=true"
else
  buildUpstreamTag "${ENT_UPSTREAM_REPO}" "${ENT_DEPENDENCY_VERSION}" "-P$BUILD_PROFILE -Pags -Pall-tas-tests -Dlicense.failOnNotUptodateHeader=true"
fi

SHARE_DEPENDENCY_VERSION="$(retrievePomProperty "dependency.alfresco-enterprise-share.version")"
SHARE_IMAGE=$([[ "${SHARE_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] && echo "-Dshare.image.tag=latest" || echo)

# Prevent merging of any SNAPSHOT dependencies into the master or the release/* branches
if [[ $(isPullRequestBuild) && "${SHARE_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ && "${BRANCH_NAME}" =~ ^master$|^release/.+$ ]] ; then
  printf "PRs with SNAPSHOT dependencies are not allowed into master or release branches\n"
  exit 1
fi

# Prevent release jobs from starting when there are SNAPSHOT upstream dependencies
if [[ "${SHARE_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] && [ "${JOB_NAME,,}" = "release" ] ; then
  printf "Cannot release project with SNAPSHOT dependencies!\n"
  exit 1
fi

SHARE_UPSTREAM_REPO="github.com/Alfresco/alfresco-enterprise-share.git"
# Checkout the upstream alfresco-enterprise-share project (tag or branch; + build if the latter)
if [[ "${SHARE_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] ; then
  pullAndBuildSameBranchOnUpstream "${SHARE_UPSTREAM_REPO}" "-P$BUILD_PROFILE -Pags -Dlicense.failOnNotUptodateHeader=true -Ddocker.quay-expires.value=NEVER ${REPO_IMAGE} -Ddependency.alfresco-community-repo.version=${COM_DEPENDENCY_VERSION} -Ddependency.alfresco-enterprise-repo.version=${ENT_DEPENDENCY_VERSION}"
else
  pullUpstreamTagAndBuildDockerImage "${SHARE_UPSTREAM_REPO}" "${SHARE_DEPENDENCY_VERSION}" "-P$BUILD_PROFILE -Pags -Dlicense.failOnNotUptodateHeader=true -Ddocker.quay-expires.value=NEVER -Ddependency.alfresco-community-repo.version=${COM_DEPENDENCY_VERSION} -Ddependency.alfresco-enterprise-repo.version=${ENT_DEPENDENCY_VERSION}"
fi

# Build the current project
mvn -B -ntp -V -q install -DskipTests -Dmaven.javadoc.skip=true -P${BUILD_PROFILE} -Pags ${REPO_IMAGE} ${SHARE_IMAGE}

popd
set +vex
echo "=========================== Finishing Build Script =========================="

