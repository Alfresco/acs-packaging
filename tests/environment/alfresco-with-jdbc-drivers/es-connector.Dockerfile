#BUIDING CONTAINER FOR TAS TESTING WITH DIFFERENT JDBC CONNECTORS
ARG IMAGE_NAME
FROM $IMAGE_NAME

COPY tests/environment/alfresco-with-jdbc-drivers/*.jar /opt/db-drivers/
