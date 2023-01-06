FROM alfresco/alfresco-share-base:${share.image.tag}

# Set default user information
ARG IMAGEUSERNAME=alfresco

LABEL quay.expires-after=${docker.quay-expires.value}

USER ${IMAGEUSERNAME}

ENTRYPOINT ["/usr/local/tomcat/shared/classes/alfresco/substituter.sh", "catalina.sh run"]

EXPOSE 8000
