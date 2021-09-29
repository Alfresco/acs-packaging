FROM alfresco/alfresco-enterprise-repo-base:${repo.image.tag}

USER root

ARG TOMCAT_DIR=/usr/local/tomcat

# Copy the amps from build context to the appropriate location for your application server
COPY target/amps/*.amp ${TOMCAT_DIR}/amps/

# Copy api-explorer war into webapps folder
COPY target/wars/api-explorer.war ${TOMCAT_DIR}/webapps/

# Unpack  api-explorer
RUN mkdir ${TOMCAT_DIR}/webapps/api-explorer && cd ${TOMCAT_DIR}/webapps/api-explorer && \
  jar -xvf ${TOMCAT_DIR}/webapps/api-explorer.war && rm -f ${TOMCAT_DIR}/webapps/api-explorer.war

# Copy gs-api-explorer war into webapps folder
COPY target/wars/gs-api-explorer.war ${TOMCAT_DIR}/webapps/

# Unpack gs-api-explorer.war
RUN mkdir ${TOMCAT_DIR}/webapps/gs-api-explorer && cd ${TOMCAT_DIR}/webapps/gs-api-explorer && \
  jar -xvf ${TOMCAT_DIR}/webapps/gs-api-explorer.war && rm -f ${TOMCAT_DIR}/webapps/gs-api-explorer.war

# Turn on log4j debug frequently needed in the single pipeline
RUN echo -e '\n\
log4j.logger.org.alfresco.repo.content.transform.TransformerDebug=debug\n\
' >> ${TOMCAT_DIR}/shared/classes/alfresco/extension/custom-log4j.properties


### Download and unpack jolokia
RUN mkdir ${TOMCAT_DIR}/webapps/jolokia && \
    curl -o /tmp/jolokia.war https://repo1.maven.org/maven2/org/jolokia/jolokia-war-unsecured/1.6.2/jolokia-war-unsecured-1.6.2.war && \
    cd ${TOMCAT_DIR}/webapps/jolokia && \
    jar xvf /tmp/jolokia.war && \
    cd ${TOMCAT_DIR} && \
    rm /tmp/jolokia.war

# Grant all security permissions to jolokia and share in order to work properly.
RUN sed -i -e "\$a\grant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/jolokia\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};\ngrant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/share\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};" ${TOMCAT_DIR}/conf/catalina.policy

## All files in the tomcat folder must be owned by root user and Alfresco group as mentioned in the parent Dockerfile
RUN chgrp -R Alfresco ${TOMCAT_DIR} && \
  find ${TOMCAT_DIR}/webapps -type d -exec chmod 0750 {} \; && \
  find ${TOMCAT_DIR}/webapps -type f -exec chmod 0640 {} \; && \
  find ${TOMCAT_DIR}/shared -type d -exec chmod 0750 {} \; && \
  find ${TOMCAT_DIR}/shared -type f -exec chmod 0640 {} \; && \
  chmod -R g+r ${TOMCAT_DIR}/webapps

WORKDIR /usr/src/alfresco
COPY idp.jks .

#Use the alfresco user
#USER alfresco

ENV TOMCAT_DIR=${TOMCAT_DIR}

ENV ALFRESCO_AMPS=ALL
ENV ALFRESCO_WEBAPP=alfresco
ENV ALFRESCO_AMPS_DIR=$TOMCAT_DIR/amps

COPY docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["catalina.sh", "run", "-security"]
