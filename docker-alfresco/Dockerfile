FROM alfresco/alfresco-enterprise-repo-base:${upstream.image.tag}

# Set default docker_context.
ARG resource_path=target

# Set default environment args
ARG TOMCAT_DIR=/usr/local/tomcat

USER root

# Copy in the share-services amp
COPY ${resource_path}/amps/* ${TOMCAT_DIR}/amps/

# Install amps on alfresco.war
RUN java -jar ${TOMCAT_DIR}/alfresco-mmt/alfresco-mmt*.jar install \
              ${TOMCAT_DIR}/amps \
              ${TOMCAT_DIR}/webapps/alfresco -directory -nobackup

# Make webapps folder read-only.
RUN chmod -R =r ${TOMCAT_DIR}/webapps && \
    chmod 664 ${TOMCAT_DIR}/amps/*
