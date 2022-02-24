#!/usr/bin/env bash
echo "=========================== Starting Build Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

source "$(dirname "${BASH_SOURCE[0]}")/build_functions.sh"

ENT_DEPENDENCY_VERSION="$(retrievePomProperty "dependency.alfresco-enterprise-repo.version")"
REPO_IMAGE=$([[ "${ENT_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] && echo "-Drepo.image.tag=latest" || echo)

# Either both the parent and the upstream dependency are the same, or else fail the build
if [ "${ENT_DEPENDENCY_VERSION}" != "$(retrievePomParentVersion)" ]; then
  printf "Upstream dependency version (%s) is different then the project parent version!\n" "${ENT_DEPENDENCY_VERSION}"
  exit 1
fi

# Prevent merging of any SNAPSHOT dependencies into the master or the release/* branches
if [[ $(isPullRequestBuild) && "${ENT_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ && "${TRAVIS_BRANCH}" =~ ^master$|^release/.+$ ]] ; then
  printf "PRs with SNAPSHOT dependencies are not allowed into master or release branches\n"
  exit 1
fi

# Prevent release jobs from starting when there are SNAPSHOT upstream dependencies
if [[ "${ENT_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] && [ "${TRAVIS_BUILD_STAGE_NAME,,}" = "release" ] ; then
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
if [[ $(isPullRequestBuild) && "${COM_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ && "${TRAVIS_BRANCH}" =~ ^master$|^release/.+$ ]] ; then
  printf "PRs with SNAPSHOT dependencies are not allowed into master or release branches\n"
  exit 1
fi

# Prevent release jobs from starting when there are SNAPSHOT upstream dependencies
if [[ "${COM_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] && [ "${TRAVIS_BUILD_STAGE_NAME,,}" = "release" ] ; then
  printf "Cannot release project with SNAPSHOT dependencies!\n"
  exit 1
fi

COM_UPSTREAM_REPO="github.com/Alfresco/alfresco-community-repo.git"

# Checkout the upstream alfresco-community-repo project (tag or branch; + build if the latter)
if [[ "${COM_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] ; then
  pullAndBuildSameBranchOnUpstream "${COM_UPSTREAM_REPO}" "-Pags -Dlicense.failOnNotUptodateHeader=true"
else
  pullUpstreamTag "${COM_UPSTREAM_REPO}" "${COM_DEPENDENCY_VERSION}"
fi

# Ensure that the repository is targeting this version of ACS.
pushd "$(dirname "${BASH_SOURCE[0]}")/../../../alfresco-community-repo"
ACS_VERSION_IN_COMMUNITY_REPO="$(retrievePomProperty "acs.version.major").$(retrievePomProperty "acs.version.minor").$(retrievePomProperty "acs.version.revision")"
popd
# Get the version properties from enterprise repo and compare with community repo.
pushd "$(dirname "${BASH_SOURCE[0]}")/../../../alfresco-enterprise-repo"
MAJOR_FROM_ENTERPRISE=$(mvn -q -Dexec.executable=echo -Dexec.args='${acs.version.major}' --non-recursive exec:exec 2>/dev/null)
MINOR_FROM_ENTERPRISE=$(mvn -q -Dexec.executable=echo -Dexec.args='${acs.version.minor}' --non-recursive exec:exec 2>/dev/null)
REVISION_FROM_ENTERPRISE=$(mvn -q -Dexec.executable=echo -Dexec.args='${acs.version.revision}' --non-recursive exec:exec 2>/dev/null)
if [[ "${MAJOR_FROM_ENTERPRISE}.${MINOR_FROM_ENTERPRISE}.${REVISION_FROM_ENTERPRISE}" != ${ACS_VERSION_IN_COMMUNITY_REPO} ]]
then
    printf "Referenced version of community repo specifies \"${ACS_VERSION_IN_COMMUNITY_REPO}\" in pom.xml but enterprise repo specifies \"${MAJOR_FROM_ENTERPRISE}.${MINOR_FROM_ENTERPRISE}.${REVISION_FROM_ENTERPRISE}\"."
    exit 1
fi
# Get the hotfix label (including a dot) from alfresco-enterprise-repo, or an empty string if it's not set.
HOTFIX_LABEL_IN_ENTERPRISE_REPO="$(retrievePomProperty "acs.version.label")"
popd
ACS_VERSION=$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec 2>/dev/null)
# Create a regular expression from the repo properties.
VERSION_REGEX="^${ACS_VERSION_IN_COMMUNITY_REPO}${HOTFIX_LABEL_IN_ENTERPRISE_REPO}([^.0-9].*)?$"
if ! [[ ${ACS_VERSION} =~ ${VERSION_REGEX} ]]
then
    printf "Referenced version of community repo specifies \"${ACS_VERSION_IN_COMMUNITY_REPO}\" in pom.xml and enterprise repo specifies \"${HOTFIX_LABEL_IN_ENTERPRISE_REPO}\", but this is ${ACS_VERSION}."
    exit 1
fi

# Build the upstream alfresco-enterprise-repo project with its docker image
if [[ "${ENT_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] ; then
  buildSameBranchOnUpstream "${ENT_UPSTREAM_REPO}" "-Pbuild-docker-images -Pags -Dlicense.failOnNotUptodateHeader=true"
else
  buildUpstreamTag "${ENT_UPSTREAM_REPO}" "${ENT_DEPENDENCY_VERSION}" "-Pbuild-docker-images -Pags -Dlicense.failOnNotUptodateHeader=true"
fi

SHARE_DEPENDENCY_VERSION="$(retrievePomProperty "dependency.alfresco-enterprise-share.version")"
SHARE_IMAGE=$([[ "${SHARE_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] && echo "-Dshare.image.tag=latest" || echo)

# Prevent merging of any SNAPSHOT dependencies into the master or the release/* branches
if [[ $(isPullRequestBuild) && "${SHARE_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ && "${TRAVIS_BRANCH}" =~ ^master$|^release/.+$ ]] ; then
  printf "PRs with SNAPSHOT dependencies are not allowed into master or release branches\n"
  exit 1
fi

# Prevent release jobs from starting when there are SNAPSHOT upstream dependencies
if [[ "${SHARE_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] && [ "${TRAVIS_BUILD_STAGE_NAME,,}" = "release" ] ; then
  printf "Cannot release project with SNAPSHOT dependencies!\n"
  exit 1
fi

SHARE_UPSTREAM_REPO="github.com/Alfresco/alfresco-enterprise-share.git"

# Checkout the upstream alfresco-enterprise-share project (tag or branch; + build if the latter)
if [[ "${SHARE_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] ; then
  pullAndBuildSameBranchOnUpstream "${SHARE_UPSTREAM_REPO}" "-Pbuild-docker-images -Pags -Dlicense.failOnNotUptodateHeader=true -Ddocker.quay-expires.value=NEVER ${REPO_IMAGE} -Ddependency.alfresco-community-repo.version=${COM_DEPENDENCY_VERSION} -Ddependency.alfresco-enterprise-repo.version=${ENT_DEPENDENCY_VERSION}"
else
  pullUpstreamTagAndBuildDockerImage "${SHARE_UPSTREAM_REPO}" "${SHARE_DEPENDENCY_VERSION}" "-Pbuild-docker-images -Pags -Dlicense.failOnNotUptodateHeader=true -Ddocker.quay-expires.value=NEVER -Ddependency.alfresco-community-repo.version=${COM_DEPENDENCY_VERSION} -Ddependency.alfresco-enterprise-repo.version=${ENT_DEPENDENCY_VERSION}"
fi

pushd "$(dirname "${BASH_SOURCE[0]}")/../../../alfresco-enterprise-share"
MAJOR_FROM_SHARE=$(mvn -q -Dexec.executable=echo -Dexec.args='${version.major}' --non-recursive exec:exec 2>/dev/null)
MINOR_FROM_SHARE=$(mvn -q -Dexec.executable=echo -Dexec.args='${version.minor}' --non-recursive exec:exec 2>/dev/null)
REVISION_FROM_SHARE=$(mvn -q -Dexec.executable=echo -Dexec.args='${version.revision}' --non-recursive exec:exec 2>/dev/null)
if [[ "${MAJOR_FROM_SHARE}.${MINOR_FROM_SHARE}.${REVISION_FROM_SHARE}" != ${ACS_VERSION_IN_COMMUNITY_REPO} ]]
then
    printf "Referenced version of community repo specifies \"${ACS_VERSION_IN_COMMUNITY_REPO}\" in pom.xml but enterprise share specifies \"${MAJOR_FROM_SHARE}.${MINOR_FROM_SHARE}.${REVISION_FROM_SHARE}\"."
    exit 1
fi
# Check the major part of the Share dependency versions match those from acs-packaging.
COM_DEP_VERSION_FROM_SHARE="$(retrievePomProperty "dependency.alfresco-enterprise-repo.version")"
if [[ $(echo ${COM_DEP_VERSION_FROM_SHARE} | cut -d "." -f 1) != $(echo ${COM_DEPENDENCY_VERSION} | cut -d "." -f 1) ]]
then
    printf "Community repo version from Share (${COM_DEP_VERSION_FROM_SHARE}) isn't similar to version from acs-packaging (${COM_DEPENDENCY_VERSION})."
    exit 1
fi
ENT_DEP_VERSION_FROM_SHARE="$(retrievePomProperty "dependency.alfresco-community-repo.version")"
if [[ $(echo ${ENT_DEP_VERSION_FROM_SHARE} | cut -d "." -f 1) != $(echo ${ENT_DEPENDENCY_VERSION} | cut -d "." -f 1) ]]
then
    printf "Enterprise repo version from Share (${ENT_DEP_VERSION_FROM_SHARE}) isn't similar to version from acs-packaging (${ENT_DEPENDENCY_VERSION})."
    exit 1
fi
popd

# Build the current project
mvn -B -ntp -V -q install -DskipTests -Dmaven.javadoc.skip=true -Pbuild-docker-images -Pags ${REPO_IMAGE} ${SHARE_IMAGE}


popd
set +vex
echo "=========================== Finishing Build Script =========================="

