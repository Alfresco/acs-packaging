set -x
TRANSFORMERS_TAG=$(mvn help:evaluate -Dexpression=dependency.alfresco-transform-core.version -q -DforceStdout)
TRANSFORM_ROUTER_TAG=$(mvn help:evaluate -Dexpression=dependency.alfresco-transform-service.version -q -DforceStdout)

# .env files are picked up from project directory correctly on docker-compose 1.23.0+
TRANSFORMERS_TAG=${TRANSFORMERS_TAG} TRANSFORM_ROUTER_TAG=${TRANSFORM_ROUTER_TAG} docker-compose -f acs-packaging/dev/docker-compose.yml up