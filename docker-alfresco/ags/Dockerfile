# BUILD STAGE AGS
FROM debian:11-slim AS AGSBUILDER

RUN DEBIAN_FRONTEND=noninteractive apt-get -yqq install unzip && \
    mkdir build

COPY target/gs-api-explorer-*.war

RUN unzip gs-api-explorer-*.war -d build/ && \
    chmod -R g-w,o= build
    
# AcTUAL IMAGE
FROM alfresco/alfresco-content-repository:${image.tag}

# Set default docker_context.
ARG resource_path=target

#Use the root user
USER root

### Copy the AMP from build context to amps
COPY target/alfresco-governance-services-enterprise-repo-*.amp /usr/local/tomcat/amps/
### Copy gs-api-explorer war into webapps folder
COPY --chown=root:Alfresco --from AGSBUILDER build/ /usr/local/tomcat/webapps/gs-api-explorer

# Install amps on alfresco.war & set all security permissions to jolokia and share in order to work properly.
RUN java -jar /usr/local/tomcat/alfresco-mmt/alfresco-mmt*.jar install \
              /usr/local/tomcat/amps \
              /usr/local/tomcat/webapps/alfresco -directory -nobackup; \
              sed -i -e "\$a\grant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/jolokia\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};\ngrant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/share\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};" /usr/local/tomcat/conf/catalina.policy

#Use the alfresco user
USER alfresco
