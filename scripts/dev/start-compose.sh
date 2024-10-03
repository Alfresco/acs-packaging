set -x
export TRANSFORMERS_TAG=$(mvn -f acs-packaging/pom.xml help:evaluate -Dexpression=dependency.alfresco-transform-core.version -q -DforceStdout)
export TRANSFORM_ROUTER_TAG=$(mvn -f acs-packaging/pom.xml help:evaluate -Dexpression=dependency.alfresco-transform-service.version -q -DforceStdout)

# .env files are picked up from project directory correctly on docker-compose 1.23.0+
docker compose -f acs-packaging/dev/docker-compose.yml up
docker compose -f acs-packaging/dev/docker-compose.yml logs -f &