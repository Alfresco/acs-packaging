#BUIDING CONTAINER FOR TAS TESTING WITH DIFFERENT JDBC CONNECTORS
FROM quay.io/alfresco/alfresco-elasticsearch-live-indexing:feature_ACS-3634-additional-jdbc-driver-loading-2034

COPY *.jar /opt/db-drivers/
