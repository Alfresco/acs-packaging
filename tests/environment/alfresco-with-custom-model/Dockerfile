FROM alfresco/alfresco-content-repository:latest

ARG TOMCAT_DIR=/usr/local/tomcat
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG USERNAME=alfresco
ARG USERID=33000

USER root

COPY custom-workflow ${TOMCAT_DIR}/shared/classes/alfresco/extension

RUN chgrp -R ${GROUPNAME} ${TOMCAT_DIR}/shared/classes/alfresco/extension && \
    find ${TOMCAT_DIR}/shared -type d -exec chmod 0750 {} \; && \
    find ${TOMCAT_DIR}/shared -type f -exec chmod 0640 {} \;

USER ${USERID}