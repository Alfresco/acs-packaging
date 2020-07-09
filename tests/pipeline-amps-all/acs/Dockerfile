FROM alfresco/alfresco-content-repository:${acs.version}

USER root

# SAML files
COPY saml/keystore/saml.keystore /usr/local/tomcat/saml.keystore
COPY saml/keystore/saml.properties /usr/local/tomcat/saml.properties
COPY saml/my-custom-aos-sp.properties /usr/local/tomcat/shared/classes/alfresco/extension/subsystems/SAML/repository/aos/my-custom-aos-sp.properties
COPY saml/my-custom-share-sp.properties /usr/local/tomcat/shared/classes/alfresco/extension/subsystems/SAML/share/share/my-custom-share-sp.properties
COPY saml/my-custom-rest-api-sp.properties /usr/local/tomcat/shared/classes/alfresco/extension/subsystems/SAML/repository/rest-api/my-custom-rest-api-sp.properties

COPY target/amps/*.amp /usr/local/tomcat/amps/

RUN java -jar /usr/local/tomcat/alfresco-mmt/alfresco-mmt*.jar install \
  /usr/local/tomcat/amps /usr/local/tomcat/webapps/alfresco -directory -nobackup -force

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

