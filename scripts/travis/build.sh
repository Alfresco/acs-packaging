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
  pullAndBuildSameBranchOnUpstream "${COM_UPSTREAM_REPO}" "-Pags -Pall-tas-tests -Dlicense.failOnNotUptodateHeader=true"
else
  pullUpstreamTag "${COM_UPSTREAM_REPO}" "${COM_DEPENDENCY_VERSION}"
fi

# Build the upstream alfresco-enterprise-repo project with its docker image
if [[ "${ENT_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] ; then
  buildSameBranchOnUpstream "${ENT_UPSTREAM_REPO}" "-Pbuild-docker-images -Pags -Pall-tas-tests -Dlicense.failOnNotUptodateHeader=true"
else
  buildUpstreamTag "${ENT_UPSTREAM_REPO}" "${ENT_DEPENDENCY_VERSION}" "-Pbuild-docker-images -Pags -Pall-tas-tests -Dlicense.failOnNotUptodateHeader=true"
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

# Build the current project
mvn -B -ntp -V -q install -DskipTests -Dmaven.javadoc.skip=true -Pbuild-docker-images -Pags ${REPO_IMAGE} ${SHARE_IMAGE}

#Build alfresco image with jdbc drivers
MYSQL_JDBC_TAG=$(mvn help:evaluate -Dexpression=dependency.mysql.version -q -DforceStdout)
mvn dependency:copy -Dartifact=mysql:mysql-connector-java:${MYSQL_JDBC_TAG}:jar -DoutputDirectory=tests/environment/alfresco-with-jdbc-drivers

MARIADB_JDBC_TAG=$(mvn help:evaluate -Dexpression=dependency.mariadb.version -q -DforceStdout)
mvn dependency:copy -Dartifact=org.mariadb.jdbc:mariadb-java-client:${MARIADB_JDBC_TAG}:jar -DoutputDirectory=tests/environment/alfresco-with-jdbc-drivers

MSSQL_JDBC_TAG=$(mvn help:evaluate -Dexpression=dependency.mssql-jdbc.version -q -DforceStdout)
mvn dependency:copy -Dartifact=com.microsoft.sqlserver:mssql-jdbc:${MSSQL_JDBC_TAG}:jar -DoutputDirectory=tests/environment/alfresco-with-jdbc-drivers

ORACLE_JDBC_TAG=$(mvn help:evaluate -Dexpression=dependency.ojdbc8.version -q -DforceStdout)
mvn dependency:copy -Dartifact=com.oracle.database.jdbc:ojdbc8:${ORACLE_JDBC_TAG}:jar -DoutputDirectory=tests/environment/alfresco-with-jdbc-drivers

docker build -t alfresco-repository-databases:latest -f tests/environment/alfresco-with-jdbc-drivers/alfresco.Dockerfile .

source tests/environment/.env

if [[ "${ES_CONNECTOR_TAG}" = [[:cntrl:]] ]]
then
  ES_CONNECTOR_TAG=$(mvn help:evaluate -Dexpression=dependency.elasticsearch-shared.version -q -DforceStdout)
  export ES_CONNECTOR_TAG
  echo "$ES_CONNECTOR_TAG"
fi

docker build -t alfresco-es-indexing-jdbc:latest -f tests/environment/alfresco-with-jdbc-drivers/es-connector.Dockerfile . --build-arg IMAGE_NAME="quay.io/alfresco/alfresco-elasticsearch-live-indexing:${ES_CONNECTOR_TAG%%[[:cntrl:]]}"
docker build -t alfresco-es-reindexing-jdbc:latest -f tests/environment/alfresco-with-jdbc-drivers/es-connector.Dockerfile . --build-arg IMAGE_NAME="quay.io/alfresco/alfresco-elasticsearch-reindexing:${ES_CONNECTOR_TAG%%[[:cntrl:]]}"


popd
set +vex
echo "=========================== Finishing Build Script =========================="

