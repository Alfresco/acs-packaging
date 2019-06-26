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
RUN chgrp -R ${GROUPNAME} ${TOMCAT_DIR} && \
    find ${TOMCAT_DIR}/webapps/alfresco -type d -exec chmod 0750 {} \; && \
    find ${TOMCAT_DIR}/webapps/alfresco -type f -exec chmod 0640 {} \; && \
    chmod -R g+r ${TOMCAT_DIR}/webapps/alfresco

USER ${USERNAME}