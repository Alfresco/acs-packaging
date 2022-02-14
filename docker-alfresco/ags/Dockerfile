# BUILD STAGE AGS
FROM debian:11-slim AS AGSBUILDER

RUN export DEBIAN_FRONTEND=noninteractive; \
    apt-get update -qqy && apt-get -yqq install unzip && \
    mkdir -p /build/gs-api-explorer

### Copy the AGS war from the local context
COPY target/gs-api-explorer-*.war /build

RUN unzip -q /build/gs-api-explorer-*.war -d /build/gs-api-explorer && \
    chmod -R g-w,o= /build

# AcTUAL IMAGE
FROM alfresco/alfresco-content-repository:${image.tag}

# Set docker variables
ARG GROUPNAME=Alfresco
ARG USERNAME=alfresco
ARG resource_path=target

#Use the root user
USER root

### Copy the AMP from local context to amps
COPY --chown=root:${GROUPNAME} target/alfresco-governance-services-enterprise-repo-*.amp /usr/local/tomcat/amps/
### Copy gs-api-explorer webapp from AGS build stage
COPY --chown=root:${GROUPNAME} --from=AGSBUILDER /build/gs-api-explorer /usr/local/tomcat/webapps/gs-api-explorer

# Install amps on alfresco.war & set all security permissions to jolokia and share in order to work properly.
## All files in the tomcat folder must be owned by root user and Alfresco group as mentioned in the parent Dockerfile
RUN java -jar /usr/local/tomcat/alfresco-mmt/alfresco-mmt*.jar install \
              /usr/local/tomcat/amps \
              /usr/local/tomcat/webapps/alfresco -directory -nobackup && \
    sed -i -e "\$a\grant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/jolokia\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};\ngrant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/share\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};" /usr/local/tomcat/conf/catalina.policy && \
    chgrp -R Alfresco /usr/local/tomcat && \
    find /usr/local/tomcat/webapps -type d -exec chmod 0750 {} \; && \
    find /usr/local/tomcat/webapps -type f -exec chmod 0640 {} \; && \
    find /usr/local/tomcat/shared -type d -exec chmod 0750 {} \; && \
    find /usr/local/tomcat/shared -type f -exec chmod 0640 {} \; && \
    chmod -R g+r /usr/local/tomcat/webapps

#Use the alfresco user
USER ${USERNAME}
