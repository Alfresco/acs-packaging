FROM alfresco/alfresco-content-repository:${image.tag}

# Set default docker_context.
ARG resource_path=target

# Set default user information
ARG GROUPNAME=Alfresco
ARG IMAGEUSERNAME=alfresco

# Set default environment args
ARG TOMCAT_DIR=/usr/local/tomcat

USER root

# Remove wars that are not required
RUN rm -rf ${TOMCAT_DIR}/webapps/_vti_bin \
           ${TOMCAT_DIR}/webapps/ROOT

# Copy in the alfresco-s3-connector and alfresco-ai-repo amps
COPY ${resource_path}/amps/* ${TOMCAT_DIR}/amps/

# Copy in the extra mariadb connector
COPY ${resource_path}/connector/* ${TOMCAT_DIR}/lib/

# Install amps on alfresco.war
RUN java -jar ${TOMCAT_DIR}/alfresco-mmt/alfresco-mmt*.jar install \
              ${TOMCAT_DIR}/amps \
              ${TOMCAT_DIR}/webapps/alfresco -directory -nobackup

# The standard configuration is to have all Tomcat files owned by root with group GROUPNAME and whilst owner has read/write privileges,
# group only has restricted permissions and world has no permissions.
RUN chgrp -R ${GROUPNAME} ${TOMCAT_DIR}/webapps ${TOMCAT_DIR}/amps ${TOMCAT_DIR}/lib && \
    find ${TOMCAT_DIR}/webapps -type d -exec chmod 0750 {} \; && \
    find ${TOMCAT_DIR}/webapps -type f -exec chmod 0640 {} \; && \
    chmod -R g+r ${TOMCAT_DIR}/webapps && \
    chmod 664 ${TOMCAT_DIR}/amps/*

USER ${IMAGEUSERNAME}