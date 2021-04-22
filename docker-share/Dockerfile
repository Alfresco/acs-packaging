FROM alfresco/alfresco-share-base:${share.image.tag}

COPY target/amps_share /usr/local/tomcat/amps_share

# apply amps
RUN java -jar /usr/local/tomcat/alfresco-mmt/alfresco-mmt*.jar install \
              /usr/local/tomcat/amps_share /usr/local/tomcat/webapps/share -directory -nobackup -force

ENTRYPOINT ["/usr/local/tomcat/shared/classes/alfresco/substituter.sh", "catalina.sh run"]

EXPOSE 8000
