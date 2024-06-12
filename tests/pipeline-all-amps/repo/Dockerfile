ARG BASE_IMAGE
# BUILD STAGE SWAGGERBUILDER
FROM debian:11-slim AS SWAGGERBUILDER

ARG JOLOKIA_VER=2.0.2

USER root
RUN yum install -y curl unzip && \
    mkdir -p /build/jolokia && \
    curl -o /build/jolokia-jee.war https://repo1.maven.org/maven2/org/jolokia/jolokia-war-unsecured/${JOLOKIA_VER}/jolokia-war-unsecured-${JOLOKIA_VER}.war && \
    ${TOMCAT_DIR}/bin/migrate.sh /build/jolokia-jee.war /build/jolokia.war && \
    unzip -q /build/jolokia.war -d /build/jolokia && chmod -R g+r,g-w,o= /build

COPY target/wars/api-explorer.war /build
COPY target/wars/gs-api-explorer.war /build

RUN unzip -q /build/api-explorer.war -d /build/api-explorer && \
    unzip -q /build/gs-api-explorer.war -d /build/gs-api-explorer && \
    chmod -R g+r,g-w,o= /build

# ACTUAL IMAGE
FROM ${BASE_IMAGE}

USER root

ARG TOMCAT_DIR=/usr/local/tomcat
ARG GROUPANME=Alfresco

# Copy the amps from build context to the appropriate location for your application server
COPY target/amps/*.amp ${TOMCAT_DIR}/amps/

# Copy api-explorer
COPY --chown=root:${GROUPANME} --from=SWAGGERBUILDER /build/api-explorer ${TOMCAT_DIR}/webapps/api-explorer

# Copy gs-api-explorer
COPY --chown=root:${GROUPANME} --from=SWAGGERBUILDER /build/gs-api-explorer ${TOMCAT_DIR}/webapps/gs-api-explorer

# Turn on log4j debug frequently needed in the single pipeline
RUN echo -e '\n\
logger.alfresco-repo-content-transform-TransformerDebug.name=org.alfresco.repo.content.transform.TransformerDebug\n\
logger.alfresco-repo-content-transform-TransformerDebug.level=debug\n\
' >> ${TOMCAT_DIR}/shared/classes/alfresco/extension/custom-log4j2.properties

# Grant all security permissions to jolokia and share in order to work properly.
RUN sed -i -e "\$a\grant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/jolokia\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};\ngrant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/share\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};" ${TOMCAT_DIR}/conf/catalina.policy

#Use the alfresco user
#USER alfresco

ENV TOMCAT_DIR=${TOMCAT_DIR}

ENV ALFRESCO_AMPS=ALL
ENV ALFRESCO_WEBAPP=alfresco
ENV ALFRESCO_AMPS_DIR=$TOMCAT_DIR/amps

COPY docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["catalina.sh", "run", "-security"]

LABEL quay.expires-after=${docker.quay-expires.value}
