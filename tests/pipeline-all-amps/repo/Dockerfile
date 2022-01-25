# BUILD STAGE SWAGGERBUILDER
FROM debian:11-slim AS SWAGGERBUILDER

ARG JOLOKIA_VER=1.6.2

RUN DEBIAN_FRONTEND=noninteractive; \
    apt-get update -yqq && apt-get -yqq install curl unzip && \
    mkdir -p /build/{api-explorer,gs-api-explorer,jolokia} && \
    curl -o /build/jolokia.war https://repo1.maven.org/maven2/org/jolokia/jolokia-war-unsecured/${JOLOKIA_VER}/jolokia-war-unsecured-${JOLOKIA_VER}.war && \
    unzip -q /build/jolokia.war -d /build/jolokia

COPY target/wars/api-explorer.war /build
COPY target/wars/gs-api-explorer.war /build

RUN unzip -q /build/api-explorer.war -d /build/api-explorer && \
    unzip -q /build/gs-api-explorer.war -d /build/gs-api-explorer && \
    chmod -R g+r,g-w,o= /build

# ACTUAL IMAGE
FROM alfresco/alfresco-enterprise-repo-base:${repo.image.tag}

USER root

ARG TOMCAT_DIR=/usr/local/tomcat
ARG GROUPANME=Alfresco

# Copy the idp.jks keystore file used to enable AOS with SAML
COPY idp.jks /usr/src/alfresco/

# Copy the amps from build context to the appropriate location for your application server
COPY target/amps/*.amp ${TOMCAT_DIR}/amps/

# Copy api-explorer
COPY --chown=root:${GROUPANME} --from=SWAGGERBUILDER /build/api-explorer ${TOMCAT_DIR}/webapps/api-explorer

# Copy gs-api-explorer
COPY --chown=root:${GROUPANME} --from=SWAGGERBUILDER /build/gs-api-explorer ${TOMCAT_DIR}/webapps/gs-api-explorer

# Turn on log4j debug frequently needed in the single pipeline
RUN echo -e '\n\
log4j.logger.org.alfresco.repo.content.transform.TransformerDebug=debug\n\
' >> ${TOMCAT_DIR}/shared/classes/alfresco/extension/custom-log4j.properties

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
