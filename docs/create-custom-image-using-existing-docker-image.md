## Creating customized Docker images using an existing Docker image

**WARNING**

The recommended way of building extensions is to use [Rest APIs](https://api-explorer.alfresco.com/api-explorer). The AMP extension mechanism may be replaced in the future.

#### Prerequisites

* The extension is packaged inside an AMP
* Docker
* Access to Docker registry where the new Docker image will be pushed

### Applying AMPs that don't require additional configuration (easy)

**Note:** We are going to use a fork of the [alfresco-bulk-import tool](https://github.com/Epurashu/alfresco-bulk-import), this is compatible with Alfresco 6.0.X(enterprise)/6.0.3(community) and above.
#### Steps:
* We are going to use the following folder and file structure:

```bash
alfresco-custom-image                          (Dir)
│
└───Dockerfile                                (File)
│
└───amps                                       (Dir)
    │  
    └───alfresco-bulk-import-*.amp            (File)
```
* Clone and download alfresco-bulk-import project.

* Trigger a Maven build from the root folder of the project (SOMEPATH/alfresco-bulk-import).

```bash
mvn clean install
```

* Copy SOMEPATH/alfresco-bulk-import/amp/target/alfresco-bulk-import-*.amp under alfresco-custom-image/amps/.

* Create a Dockerfile under alfresco-custom-image/

* Add the following lines to the Dockerfile:

```bash
# Apply AMPs to the latest Community repository image.
ARG BASE_IMAGE=alfresco/alfresco-content-repository-community
ARG VERSION=latest

FROM ${BASE_IMAGE}:${VERSION}

# Need these viariables to give permissions to tomcat (refer Dockerfile below)
ARG GROUPNAME=Alfresco
ARG USERNAME=alfresco
ARG TOMCAT_DIR=/usr/local/tomcat

# Need to switch to root to add the new files
USER root

### Copy the AMPs from build context to the appropriate location for your application server
COPY amps ${TOMCAT_DIR}/amps

### Install AMPs on alfresco
RUN java -jar ${TOMCAT_DIR}/alfresco-mmt/alfresco-mmt*.jar install \
              ${TOMCAT_DIR}/amps ${TOMCAT_DIR}/webapps/alfresco -directory -nobackup -force

# Need to have all Tomcat files owned by root with group GROUPNAME as mentionned (refer Dockerfile below)
RUN chgrp -R ${GROUPNAME} ${TOMCAT_DIR}/webapps && \
    find ${TOMCAT_DIR}/webapps -type d -exec chmod 0750 {} \; && \
    find ${TOMCAT_DIR}/webapps -type f -exec chmod 0640 {} \; && \
    chmod -R g+r ${TOMCAT_DIR}/webapps && \
    chgrp -R ${GROUPNAME} ${TOMCAT_DIR}

# Switching back to alfresco user after having added amps files to run the container as non-root
USER ${USERNAME}

```

** Note: ** [Alfresco/acs-community-packaging/docker-alfresco/Dockerfile](https://github.com/Alfresco/acs-community-packaging/blob/master/docker-alfresco/Dockerfile).

** Note: ** It's not necessary to build the .amp file if you can download it via a network (FTP/HTTP/HTTPS), since the COPY command can be replaced by CURL/WGET.

* Open a Terminal and run the following command to build the custom docker image.

```bash
docker build -t <repository>:<tag> .
```

* The new docker image was built locally, now it can be pushed to the custom repository by running the following command in a terminal:

```bash
docker push <repository>:<tag>
```

### Applying AMPs that require additional configuration (advanced)

**Note:** We are going to use the alfresco-saml-distribution in order to install Alfresco-SAML-Module. This example is for testing purposes only! we are going to configure ssl using Tomcat on Linux. The recommended ways for a production environment are:
- usage of a proxy see [Official Alfresco Documentation](https://docs.alfresco.com/5.2/tasks/configure-ssl-prod.html). 
- using SSL termination at the K8s Ingress. 

### Steps:
* We are going to use the following folder and file structure:

```bash
alfresco-custom-image                            (Dir)
│
└───Repository                                   (Dir)
│   │
│   └───amp                                      (Dir)
│   │   │
│   │   └alfresco-saml-repo-*.amp               (File)
│   │
│   └───keystore                                 (Dir)
│   │    │
│   │    └ssl.keystore                          (File)
│   │    └ssl.truststore                        (File)
│   │    └saml.keystore                         (File)
│   │    └saml-keystore-password.properties     (File)
│   │
│   └───Dockerfile                              (File)
│
└───Share                                        (Dir)
    │
    └───amp_share                                (Dir)
    │   │
    │   └alfresco-saml-share-*.amp              (File)
    │
    └───keystore                                 (Dir)
    │    │
    │    └ssl.keystore                          (File)
    │    └ssl.truststore                        (File)
    └share-config-custom.xml                    (File)
    │
    └───Dockerfile                              (File)
```
* Generate keystore and truststore required for ssl and SAML. Additional information can be found under [alfresco documentation](https://docs.alfresco.com/).

* Append the content of "share-config-custom.xml" from saml-distribution to the one used in the alfresco-share docker image (We append content manually because it is to big for a SED command).

* Add files to match the above hierarchy.

* Create a Dockerfile for custom-alfresco-repository image:

```bash
# Apply AMPs to the latest Community repository image.
ARG BASE_IMAGE=alfresco/alfresco-content-repository-community
ARG VERSION=latest

FROM ${BASE_IMAGE}:${VERSION}

# Need these viariables to give permissions to tomcat (refer Dockerfile below)
ARG GROUPNAME=Alfresco
ARG USERNAME=alfresco
ARG TOMCAT_DIR=/usr/local/tomcat

# Need to switch to root to add the new files
USER root

### Copy the keystore from build context to the appropriate location for your application server
COPY keystore ${TOMCAT_DIR}/keystore

### Copy the AMPs from build context to the appropriate location for your application server
COPY amps ${TOMCAT_DIR}/amps

### Install AMPs on alfresco
RUN java -jar ${TOMCAT_DIR}/alfresco-mmt/alfresco-mmt*.jar install \
              ${TOMCAT_DIR}/amps ${TOMCAT_DIR}/webapps/alfresco -directory -nobackup -force

### NOTE: This is not the recommanded way to enable SSL, it is only for testing purposes !
### We are going to add a new <Connector> to server.xml by replacing
###    </Engine>
### With
###    </Engine>
###    <Connector port="8443" 
###               protocol="org.apache.coyote.http11.Http11Nio2Protocol"
###               sslImplementationName="org.apache.tomcat.util.net.jsse.JSSEImplementation"
###               maxThreads="150"
###               SSLEnabled="true">
###        <SSLHostConfig certificateVerification="required" 
###                       truststoreFile="/usr/local/tomcat/keystore/ssl.keystore/ssl.truststore" 
###                       truststorePassword="truststorePass" 
###                       truststoreType="JCEKS" >
###            <Certificate certificateKeystoreFile="/usr/local/tomcat/keystore/ssl.keystore/ssl.keystore"
###                         certificateKeystorePassword="keystorePass"
###                         certificateKeystoreType="JCEKS" />
###        </SSLHostConfig>
###    </Connector> 

### Enable SSL by adding the proper Connector to server.xml (using a very hard to understand SED command)
RUN sed -i "s/\    <\/Engine>/\n\    <\/Engine>\n\    <Connector\ port=\"8443\"\n\               protocol=\"org.apache.coyote.http11.Http11Nio2Protocol\"\n\               sslImplementationName=\"org.apache.tomcat.util.net.jsse.JSSEImplementation\"\n\               maxThreads=\"150\"\n\               SSLEnabled=\"true\">\n\        <SSLHostConfig certificateVerification=\"required\" \n\                       truststoreFile=\"\/usr\/local\/tomcat\/keystore\/ssl.keystore\/ssl.truststore\"\n\                       truststorePassword=\"truststorePass\"\n\                       truststoreType=\"JCEKS\" >\n\            <Certificate certificateKeystoreFile=\"\/usr\/local\/tomcat\/keystore\/ssl.keystore\/ssl.keystore\"\n\                         certificateKeystorePassword=\"keystorePass\"\n\                         certificateKeystoreType=\"JCEKS\" \/>\n\        <\/SSLHostConfig>\n\    <\/Connector>/g" ${TOMCAT_DIR}/conf/server.xml


# Need to have all Tomcat files owned by root with group GROUPNAME as mentionned (refer Dockerfile below)
RUN chgrp -R ${GROUPNAME} ${TOMCAT_DIR}/webapps && \
    find ${TOMCAT_DIR}/webapps -type d -exec chmod 0750 {} \; && \
    find ${TOMCAT_DIR}/webapps -type f -exec chmod 0640 {} \; && \
    find ${TOMCAT_DIR}/keystore -type d -exec chmod 0750 {} \; && \
    find ${TOMCAT_DIR}/keystore -type f -exec chmod 0640 {} \; && \
    chmod g+rx ${TOMCAT_DIR}/conf && \
    chmod -R g+r ${TOMCAT_DIR}/conf && \
    chmod -R g+r ${TOMCAT_DIR}/webapps && \
    chgrp -R ${GROUPNAME} ${TOMCAT_DIR}

# Switching back to alfresco user after having added amps files to run the container as non-root
USER ${USERNAME}

```

* Open a Terminal and run the following command to build the custom docker repository image.

```bash
docker build -t <repository>:<tag> .
```

* The new docker image was built locally and can be pushed to the repository by running the following command in a terminal:

```bash
docker push <repository>:<tag>
```

* Create a Dockerfile for custom-alfresco-share image:

```bash
### Apply AMPs to the latest Share image (at this moment).
FROM alfresco/alfresco-share:6.0.0-rc2

# Need these viariables to give permissions to tomcat (refer Dockerfile below)
ARG GROUPNAME=Alfresco
ARG USERNAME=alfresco
ARG TOMCAT_DIR=/usr/local/tomcat

# Need to switch to root to add the new files
USER root

### Copy the keystore from build context to the appropriate location for your application server
COPY keystore ${TOMCAT_DIR}/keystore

### Copy the AMPs from build context to the appropriate location for your application server
COPY amps_share ${TOMCAT_DIR}/amps_share

### Overwrite share-config-custom.xml with the one containing additional rules required for CSRF protection. 
COPY share-config-custom.xml ${TOMCAT_DIR}/shared/classes/alfresco/web-extension

### Install AMPs on share
RUN java -jar ${TOMCAT_DIR}/alfresco-mmt/alfresco-mmt*.jar install \
              ${TOMCAT_DIR}/amps_share/alfresco-saml-share-*.amp ${TOMCAT_DIR}/webapps/share -nobackup -force

### NOTE: This is not the recommanded way to enable SSL, it is only for testing purposes !
### We are going to add a new <Connector> to server.xml by replacing
###    </Engine>
### With
###    </Engine>
###    <Connector port="8443" 
###               protocol="org.apache.coyote.http11.Http11Nio2Protocol"
###               sslImplementationName="org.apache.tomcat.util.net.jsse.JSSEImplementation"
###               maxThreads="150"
###               SSLEnabled="true">
###        <SSLHostConfig certificateVerification="required" 
###                       truststoreFile="/usr/local/tomcat/keystore/ssl.keystore/ssl.truststore" 
###                       truststorePassword="truststorePass" 
###                       truststoreType="JCEKS" >
###            <Certificate certificateKeystoreFile="/usr/local/tomcat/keystore/ssl.keystore/ssl.keystore"
###                         certificateKeystorePassword="keystorePass"
###                         certificateKeystoreType="JCEKS" />
###        </SSLHostConfig>
###    </Connector> 

### Enable SSL by adding the proper Connector to server.xml (using a very hard to understand SED command)
RUN sed -i "s/\    <\/Engine>/\n\    <\/Engine>\n\    <Connector\ port=\"8443\"\n\               protocol=\"org.apache.coyote.http11.Http11Nio2Protocol\"\n\               sslImplementationName=\"org.apache.tomcat.util.net.jsse.JSSEImplementation\"\n\               maxThreads=\"150\"\n\               SSLEnabled=\"true\">\n\        <SSLHostConfig certificateVerification=\"required\" \n\                       truststoreFile=\"\/usr\/local\/tomcat\/keystore\/ssl.keystore\/ssl.truststore\"\n\                       truststorePassword=\"truststorePass\"\n\                       truststoreType=\"JCEKS\" >\n\            <Certificate certificateKeystoreFile=\"\/usr\/local\/tomcat\/keystore\/ssl.keystore\/ssl.keystore\"\n\                         certificateKeystorePassword=\"keystorePass\"\n\                         certificateKeystoreType=\"JCEKS\" \/>\n\        <\/SSLHostConfig>\n\    <\/Connector>/g" ${TOMCAT_DIR}/conf/server.xml


# Need to have all Tomcat files owned by root with group GROUPNAME as mentionned (refer Dockerfile below)
RUN chgrp -R ${GROUPNAME} ${TOMCAT_DIR}/webapps && \
    find ${TOMCAT_DIR}/webapps -type d -exec chmod 0750 {} \; && \
    find ${TOMCAT_DIR}/webapps -type f -exec chmod 0640 {} \; && \
    find ${TOMCAT_DIR}/keystore -type d -exec chmod 0750 {} \; && \
    find ${TOMCAT_DIR}/keystore -type f -exec chmod 0640 {} \; && \
    find ${TOMCAT_DIR}/shared -type d -exec chmod 0750 {} \; && \
    find ${TOMCAT_DIR}/shared -type f -exec chmod 0640 {} \; && \
    chmod -R g+r ${TOMCAT_DIR}/webapps && \
    chgrp -R ${GROUPNAME} ${TOMCAT_DIR}

# Switching back to alfresco user after having added amps files to run the container as non-root
USER ${USERNAME}


```

* Open a Terminal and run the following command to build the custom docker repository image.

```bash
docker build -t <repository>:<tag> .
```

* The new docker image was built locally and can be pushed to the repository by running the following command in a terminal:

```bash
docker push <repository>:<tag>
```
