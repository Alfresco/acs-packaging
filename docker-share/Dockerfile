FROM alfresco/alfresco-share-base:${share.image.tag}

LABEL quay.expires-after=${docker.quay-expires.value}

ENTRYPOINT ["/usr/local/tomcat/shared/classes/alfresco/substituter.sh", "catalina.sh run"]

EXPOSE 8000
