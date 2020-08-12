FROM alfresco/alfresco-share:${image.tag}

COPY target/amps/*.amp /usr/local/tomcat/amps_share/

RUN java -jar /usr/local/tomcat/alfresco-mmt/alfresco-mmt*.jar install \
  /usr/local/tomcat/amps_share /usr/local/tomcat/webapps/share -directory -nobackup -force
