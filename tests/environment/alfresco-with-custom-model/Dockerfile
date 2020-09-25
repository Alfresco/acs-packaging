FROM alfresco/alfresco-content-repository:latest

ARG TOMCAT_DIR=/usr/local/tomcat
ARG USERID=33000

USER root

COPY custom-workflow ${TOMCAT_DIR}/shared/classes/alfresco/extension

RUN find ${TOMCAT_DIR}/shared -type d -exec chmod 0750 {} \; && \
    find ${TOMCAT_DIR}/shared -type f -exec chmod 0640 {} \;

USER ${USERID}