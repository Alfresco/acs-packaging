ARG BASE_IMAGE
FROM ${BASE_IMAGE}

ARG TOMCAT_DIR=/usr/local/tomcat
COPY target/amps_share/*.amp /usr/local/tomcat/amps_share/

ENV TOMCAT_DIR=${TOMCAT_DIR}

ENV ALFRESCO_AMPS=ALL
ENV ALFRESCO_WEBAPP=share
ENV ALFRESCO_AMPS_DIR=$TOMCAT_DIR/amps_share

COPY docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["/usr/local/tomcat/shared/classes/alfresco/substituter.sh", "catalina.sh run"]

LABEL quay.expires-after=${docker.quay-expires.value}
