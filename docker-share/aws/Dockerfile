FROM alfresco/alfresco-share:${image.tag}

ARG TOMCAT_DIR=/usr/local/tomcat

# Set default user information
ARG IMAGEUSERNAME=alfresco

# Copy Dockerfile to avoid an error if no AMPs exist
COPY target/dependency/*.amp $TOMCAT_DIR/amps_share/
RUN java -jar $TOMCAT_DIR/alfresco-mmt/alfresco-mmt*.jar install \
              $TOMCAT_DIR/amps_share $TOMCAT_DIR/webapps/share -directory -nobackup

USER ${IMAGEUSERNAME}

EXPOSE 8000

