#BUIDING CONTAINER FOR TAS TESTING WITH DIFFERENT JDBC CONNECTORS
FROM alfresco/alfresco-content-repository:latest

COPY tests/environment/alfresco-with-jdbc-drivers/*.jar /usr/local/tomcat/lib/
