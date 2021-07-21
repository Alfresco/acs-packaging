### Apply AGS enterprise repo AMP to ACS image
FROM alfresco/alfresco-content-repository:${image.tag}

# Set default docker_context.
ARG resource_path=target

#Use the root user
USER root

### Copy the AMP from build context to amps
COPY target/alfresco-governance-services-enterprise-repo-*.amp /usr/local/tomcat/amps/
COPY target/amps/alfresco-share-services-*.amp /usr/local/tomcat/amps/

# Install amps on alfresco.war
RUN java -jar /usr/local/tomcat/alfresco-mmt/alfresco-mmt*.jar install \
              /usr/local/tomcat/amps \
              /usr/local/tomcat/webapps/alfresco -directory -nobackup
### Copy gs-api-explorer war into webapps folder
COPY target/amps/gs-api-explorer-*.war /usr/local/tomcat/webapps/

### Unpack gs-api-explorer.war
RUN mkdir /usr/local/tomcat/webapps/gs-api-explorer && cd /usr/local/tomcat/webapps/gs-api-explorer && \
    jar -xvf /usr/local/tomcat/webapps/gs-api-explorer-*.war && rm -f /usr/local/tomcat/webapps/gs-api-explorer-*.war

# Grant all security permissions to jolokia and share in order to work properly.
RUN sed -i -e "\$a\grant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/jolokia\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};\ngrant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/share\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};" /usr/local/tomcat/conf/catalina.policy

## All files in the tomcat folder must be owned by root user and Alfresco group as mentioned in the parent Dockerfile
RUN chgrp -R Alfresco /usr/local/tomcat && \
    find /usr/local/tomcat/webapps -type d -exec chmod 0750 {} \; && \
    find /usr/local/tomcat/webapps -type f -exec chmod 0640 {} \; && \
    find /usr/local/tomcat/shared -type d -exec chmod 0750 {} \; && \
    find /usr/local/tomcat/shared -type f -exec chmod 0640 {} \; && \
    chmod -R g+r /usr/local/tomcat/webapps

#Use the alfresco user
USER alfresco