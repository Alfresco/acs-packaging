#BUIDING CONTAINER FOR TAS TESTING WITH DIFFERENT JDBC CONNECTORS
ARG IMAGE_NAME
FROM quay.io/alfresco/alfresco-elasticsearch-live-indexing:latest

COPY tests/environment/alfresco-with-jdbc-drivers/*.jar /opt/db-drivers/
