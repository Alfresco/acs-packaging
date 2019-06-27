# Fetch image based on Tomcat 8.5.34 and Java 11
# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-tomcat
FROM alfresco/alfresco-content-repository:${image.tag}

ARG TOMCAT_DIR=/usr/local/tomcat
ARG GROUPNAME=Alfresco
ARG USERNAME=alfresco

USER root

RUN rm -rf ${TOMCAT_DIR}/webapps/alfresco

COPY target/war/alfresco ${TOMCAT_DIR}/webapps/alfresco

# Restore permissions
RUN chgrp -R ${GROUPNAME} ${TOMCAT_DIR}/webapps/alfresco && \
    find ${TOMCAT_DIR}/webapps/alfresco -type d -exec chmod 0750 {} \; && \
    find ${TOMCAT_DIR}/webapps/alfresco -type f -exec chmod 0640 {} \; && \
    chmod -R g+r ${TOMCAT_DIR}/webapps/alfresco && \
    chgrp -R ${GROUPNAME} ${TOMCAT_DIR} && \
    # set the correct path for alfresco.log
    sed -i -e "s_log4j.appender.File.File\=alfresco.log_log4j.appender.File.File\=${TOMCAT_DIR}/logs\/alfresco.log_" \
        ${TOMCAT_DIR}/webapps/alfresco/WEB-INF/classes/log4j.properties

USER ${USERNAME}