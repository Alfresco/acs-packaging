FROM alfresco/alfresco-content-repository-aws:${repo.image.tag}

USER root

ARG TOMCAT_DIR=/usr/local/tomcat

# Copy the amps from build context to the appropriate location for your application server
COPY target/amps/*.amp /usr/local/tomcat/amps/

# Install amps on alfresco.war
RUN java -jar ${TOMCAT_DIR}/alfresco-mmt/alfresco-mmt*.jar install \
              ${TOMCAT_DIR}/amps ${TOMCAT_DIR}/webapps/alfresco -directory -nobackup -force

### Copy gs-api-explorer war into webapps folder
COPY target/wars/rm-api-explorer.war /usr/local/tomcat/webapps/

### Unpack rm-api-explorer.war
RUN mkdir /usr/local/tomcat/webapps/rm-api-explorer && cd /usr/local/tomcat/webapps/rm-api-explorer && \
  jar -xvf /usr/local/tomcat/webapps/rm-api-explorer.war && rm -f /usr/local/tomcat/webapps/rm-api-explorer.war

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

