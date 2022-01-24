# BUILD STAGE
FROM debian:11-slim AS JOLOKIABUILDER

ARG JOLOKIA_VER=1.6.2

RUN DEBIAN_FRONTEND=noninteractive apt-get -yqq install unzip && \
    mkdir build && \
    curl -o /tmp/jolokia.war https://repo1.maven.org/maven2/org/jolokia/jolokia-war-unsecured/${JOLOKIA_VER}/jolokia-war-unsecured-${JOLOKIA_VER}.war && \
    unzip /tmp/jolokia.war -d build && chmod -R g-w,o= build

# ACTUAL IMAGE
FROM alfresco/alfresco-content-repository:latest

ARG TOMCAT_DIR=/usr/local/tomcat
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG USERNAME=alfresco
ARG USERID=33000

USER root

COPY --chown=root:Alfresco --from JOLOKIABUILDER build ${TOMCAT_DIR}/webapps/jolokia

# Grant all security permissions to jolokia in order to work properly.
RUN sed -i -e "\$a\grant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/jolokia\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};" ${TOMCAT_DIR}/conf/catalina.policy && \

USER ${USERID}
