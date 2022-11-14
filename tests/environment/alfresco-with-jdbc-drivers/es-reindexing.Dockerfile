#BUIDING CONTAINER FOR TAS TESTING WITH DIFFERENT JDBC CONNECTORS
FROM quay.io/alfresco/alfresco-elasticsearch-live-reindexing:feature_ACS-3634-additional-jdbc-driver-loading-2034

COPY tests/environment/alfresco-with-jdbc-drivers/*.jar /opt/db-drivers/
