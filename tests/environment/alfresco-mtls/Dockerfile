FROM quay.io/alfresco/alfresco-content-repository:latest

ENV TOMCAT_DIR=/usr/local/tomcat
ENV ALF_DATA_DIR=${TOMCAT_DIR}/alf_data

# SSL
ARG ALIAS
ARG TRUSTSTORE_NAME
ARG TRUSTSTORE_TYPE
ARG TRUSTSTORE_PASS
ARG KEYSTORE_NAME
ARG KEYSTORE_TYPE
ARG KEYSTORE_PASS

ENV ALIAS=$ALIAS \
    TRUSTSTORE_NAME=$TRUSTSTORE_NAME \
    TRUSTSTORE_TYPE=$TRUSTSTORE_TYPE \
    TRUSTSTORE_PASS=$TRUSTSTORE_PASS \
    KEYSTORE_NAME=$KEYSTORE_NAME \
    KEYSTORE_TYPE=$KEYSTORE_TYPE \
    KEYSTORE_PASS=$KEYSTORE_PASS

USER root

# Default value in 'repository.properties' is 'dir.keystore=classpath:alfresco/keystore'
RUN echo -e "\n\
    dir.keystore=${ALF_DATA_DIR}/keystore\n\
    alfresco.encryption.ssl.keystore.type=${TRUSTSTORE_TYPE}\n\
    alfresco.encryption.ssl.truststore.type=${KEYSTORE_TYPE}\n\
    " >> ${TOMCAT_DIR}/shared/classes/alfresco-global.properties;

# Enable SSL by adding the proper Connector to server.xml
RUN sed -i "s/\
[[:space:]]\+<\/Engine>/\n\
    <\/Engine>\n\
    <Connector port=\"8443\" protocol=\"org.apache.coyote.http11.Http11NioProtocol\"\n\
        connectionTimeout=\"20000\" SSLEnabled=\"true\" maxThreads=\"150\" scheme=\"https\" secure=\"true\">\n\
        <SSLHostConfig\n\
        protocols=\"TLSv1.2,TLSv1.3\"\n\
        truststoreFile=\"\/usr\/local\/tomcat\/alf_data\/keystore\/${TRUSTSTORE_NAME}\"\n\
        truststorePassword=\"${KEYSTORE_PASS}\"\n\
        truststoreType=\"${TRUSTSTORE_TYPE}\"\n\
        certificateVerification=\"required\">\n\
          <Certificate\n\
          certificateKeystoreFile=\"\/usr\/local\/tomcat\/alf_data\/keystore\/${KEYSTORE_NAME}\"\n\
          certificateKeystorePassword=\"${TRUSTSTORE_PASS}\"\n\
          certificateKeystoreType=\"${KEYSTORE_TYPE}\"\n\
          certificateKeyAlias=\"${ALIAS}\"\n\
          type=\"RSA\"\n\
          \/>\n\
        <\/SSLHostConfig>\n\
    <\/Connector>/g" ${TOMCAT_DIR}/conf/server.xml;
