# Fetch image based on Tomcat 8.5.43 and Java 11
# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-tomcat
FROM alfresco/alfresco-content-repository:latest

ARG TOMCAT_DIR=/usr/local/tomcat

USER root

RUN rm -rf ${TOMCAT_DIR}/webapps/alfresco

COPY target/war ${TOMCAT_DIR}/webapps

COPY custom-content-service-context.xml ${TOMCAT_DIR}/shared/classes/alfresco/extension/

# set the correct path for alfresco.log
RUN sed -i -e "s_log4j.appender.File.File\=alfresco.log_log4j.appender.File.File\=${TOMCAT_DIR}/logs\/alfresco.log_" \
        ${TOMCAT_DIR}/webapps/alfresco/WEB-INF/classes/log4j.properties && \
    # Grant all security permissions to jolokia and share in order to work properly.
    sed -i -e "\$a\grant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/jolokia\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};\ngrant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/share\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};" ${TOMCAT_DIR}/conf/catalina.policy && \
    # Restore permissions
    find ${TOMCAT_DIR}/webapps -type d -exec chmod 0750 {} \; && \
    find ${TOMCAT_DIR}/webapps -type f -exec chmod 0640 {} \; && \
    find ${TOMCAT_DIR}/shared -type d -exec chmod 0750 {} \; && \
    find ${TOMCAT_DIR}/shared -type f -exec chmod 0640 {} \; && \
    chmod -R g+r ${TOMCAT_DIR}/webapps
