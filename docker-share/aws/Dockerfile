ARG BASE_IMAGE=alfresco/alfresco-base-java:jre21-rockylinux9@sha256:a5bc7626cc86656f88b7f9a84926a407a6bdb34ffef4c26d323f6b047b85258a
FROM ${BASE_IMAGE}

ARG TOMCAT_DIR=/usr/local/tomcat

# Copy Dockerfile to avoid an error if no AMPs exist
COPY target/dependency/*.amp $TOMCAT_DIR/amps_share/
RUN java -jar $TOMCAT_DIR/alfresco-mmt/alfresco-mmt*.jar install \
              $TOMCAT_DIR/amps_share $TOMCAT_DIR/webapps/share -directory -nobackup

EXPOSE 8000

