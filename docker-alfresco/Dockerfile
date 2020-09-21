FROM alfresco/alfresco-enterprise-repo-base:${repo.image.tag}

# Set default docker_context.
ARG resource_path=target

# Set default environment args
ARG TOMCAT_DIR=/usr/local/tomcat

USER root

# Copy api-explorer war
COPY ${resource_path}/war ${TOMCAT_DIR}/webapps/

# Copy in the share-services amp
COPY ${resource_path}/amps/* ${TOMCAT_DIR}/amps/

# Install amps on alfresco.war
RUN java -jar ${TOMCAT_DIR}/alfresco-mmt/alfresco-mmt*.jar install \
              ${TOMCAT_DIR}/amps \
              ${TOMCAT_DIR}/webapps/alfresco -directory -nobackup

# Make webapps folder read-only.
RUN chmod -R =r /usr/local/tomcat/webapps && \
    chmod 664 /usr/local/tomcat/amps/*

