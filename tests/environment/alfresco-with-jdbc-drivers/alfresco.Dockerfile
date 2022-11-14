#BUIDING CONTAINER FOR TAS TESTING WITH DIFFERENT JDBC CONNECTORS
FROM alfresco/alfresco-content-repository:latest

COPY *.jar /usr/local/tomcat/lib/
