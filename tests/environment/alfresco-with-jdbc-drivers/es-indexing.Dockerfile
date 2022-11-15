#BUIDING CONTAINER FOR TAS TESTING WITH DIFFERENT JDBC CONNECTORS
FROM quay.io/alfresco/alfresco-elasticsearch-live-indexing:$ES_CONNECTOR_TAG

COPY tests/environment/alfresco-with-jdbc-drivers/*.jar /opt/db-drivers/
