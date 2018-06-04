## How to create customized Docker images using an existing docker image.

**WARNING**

The recommended way of building extensions is to use [Rest APIs](https://api-explorer.alfresco.com/api-explorer). The AMP extension mechanism may be replaced in the future.

#### Prerequisites

* The extension is packaged inside an AMP
* Docker
* Access to Docker registry where the new Docker image will be pushed

### Amps that don`t require additional configurations (easy).

**Note:** We are going to use a fork of the [alfresco-bulk-import tool](https://github.com/Epurashu/alfresco-bulk-import), this is compatible with Alfresco 6.0.X(enterprise)/6.0.3(community) and above.
### Steps:
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

* Create Dockerfile under alfresco-custom-image/

* Add the following lines to the Dockerfile:

```bash
### We are going to apply amp(s) on the latest community repository image.
FROM alfresco/alfresco-content-repository-community:latest

### Copy the amps from build context to the appropriate location for your application server
COPY amps /usr/local/tomcat/amps

### Install amps on alfresco
RUN java -jar /usr/local/tomcat/alfresco-mmt/alfresco-mmt*.jar install \
              /usr/local/tomcat/amps/alfresco-bulk-import-*.amp /usr/local/tomcat/webapps/alfresco -nobackup -force
```

* Open a Terminal and run the following command to build the custom docker image.

```bash
docker build . -t customrepository/alfresco-custom-image:customTag
```

* The new docker image was build locally, now it can be pushed to customrepository by running the following command in Terminat/CMD:

```bash
docker push customrepository/alfresco-custom-image:customTag
```

** Note: ** It is not necessary to build/have amps locally and copy them to the new docker image, if the .amp can be downloaded via network (FTP/HTTP/HTTPS), the COPY command can be replaced by CURL/WGET command(s).


### Amps that require additional configurations (advanced).

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

* Create Dockerfile for custom-alfresco-repository image:

```bash
### We are going to apply amp(s) on the latest enterprise repository image.
FROM alfresco/alfresco-content-repository:latest

### Copy the keystore build context to the appropriate location for your application server
COPY keystore /usr/local/tomcat/keystore

### Copy the amps from build context to the appropriate location for your application server
COPY amps /usr/local/tomcat/amps


### Install amps on alfresco
RUN java -jar /usr/local/tomcat/alfresco-mmt/alfresco-mmt*.jar install \
              /usr/local/tomcat/amps/alfresco-saml-repo-1.1.0-*.amp /usr/local/tomcat/webapps/alfresco -nobackup -force

### NOTE: This is not the recommanded way to enable SSL, it is only for testing purposes !
### We are going to add a new <Connector> to server.xml by replacing
###    </Engine>
### With
###    </Engine>
###    <Connector port="8443" URIEncoding="UTF-8" protocol="org.apache.coyote.http11.Http11Protocol" SSLEnabled="true"
###    maxThreads="150" scheme="https" keystoreFile="/usr/local/tomcat/keystore/ssl.keystore" keystorePass="keystorePass" keystoreType="JCEKS"
###    secure="true" connectionTimeout="240000" truststoreFile="/usr/local/tomcat/keystore/ssl.truststore" truststorePass="truststorePass" truststoreType="JCEKS"
###    clientAuth="want" sslProtocol="TLS" allowUnsafeLegacyRenegotiation="true" maxHttpHeaderSize="32768" maxSavePostSize="-1" /

### Enable SSL by adding the proper Connector to server.xml (using a very hard to understand SED command)
RUN sed -i "s/\    <\/Engine>/\n\    <\/Engine>\n\    <Connector\ port=\"8443\"\ URIEncoding=\"UTF-8\"\ protocol=\"org.apache.coyote.http11.Http11Protocol\"\ SSLEnabled=\"true\"\n\               maxThreads=\"150\"\ scheme=\"https\"\ keystoreFile=\"\/usr\/local\/tomcat\/keystore\/ssl.keystore\"\ keystorePass=\"keystorePass\"\ keystoreType=\"JCEKS\"\n\ secure=\"true\"\ connectionTimeout=\"240000\"\ truststoreFile=\"\/usr\/local\/tomcat\/keystore\/ssl.truststore\"\ truststorePass=\"truststorePass\"\ truststoreType=\"JCEKS\"\n\               clientAuth=\"want\"\ sslProtocol=\"TLS\"\ allowUnsafeLegacyRenegotiation=\"true\"\ maxHttpHeaderSize=\"32768\"\ maxSavePostSize=\"-1\" \/>/g" /usr/local/tomcat/conf/server.xml
```

* Open a Terminal and run the following command to build the custom docker repository image.

```bash
docker build . -t customrepository/custom-alfresco-repository:customTag
```

* The new docker image was build locally, now it can be pushed to customrepository by running the following command in Terminat/CMD:

```bash
docker push customrepository/custom-alfresco-repository:customTag
```

* Create Dockerfile for custom-alfresco-share image:

```bash
### We are going to apply amp(s) on the latest (at this moment,share does not have image with latest tag) share image.
FROM alfresco/alfresco-share:6.0.0-rc2

### Copy the keystore build context to the appropriate location for your application server
COPY keystore /usr/local/tomcat/keystore

### Copy the amps from build context to the appropriate location for your application server
COPY amps_share /usr/local/tomcat/amps_share

### Overwrite share-config-custom.xml with the one containing additional rules required for CSRF protection. 
COPY share-config-custom.xml /usr/local/tomcat/shared/classes/alfresco/web-extension

### Install amps on share
RUN java -jar /usr/local/tomcat/alfresco-mmt/alfresco-mmt*.jar install \
              /usr/local/tomcat/amps_share/alfresco-saml-share-*.amp /usr/local/tomcat/webapps/share -nobackup -force

### NOTE: This is not the recommanded way to enable SSL, it is only for testing purposes !
### We are going to add a new <Connector> to server.xml by replacing
###    </Engine>
### With
###    </Engine>
###    <Connector port="8443" URIEncoding="UTF-8" protocol="org.apache.coyote.http11.Http11Protocol" SSLEnabled="true"
###    maxThreads="150" scheme="https" keystoreFile="/usr/local/tomcat/keystore/ssl.keystore" keystorePass="keystorePass" keystoreType="JCEKS"
###    secure="true" connectionTimeout="240000" truststoreFile="/usr/local/tomcat/keystore/ssl.truststore" truststorePass="truststorePass" truststoreType="JCEKS"
###    clientAuth="want" sslProtocol="TLS" allowUnsafeLegacyRenegotiation="true" maxHttpHeaderSize="32768" maxSavePostSize="-1" /

### Enable SSL by adding the proper Connector to server.xml (using a very hard to understand SED command)
RUN sed -i "s/\    <\/Engine>/\n\    <\/Engine>\n\    <Connector\ port=\"8443\"\ URIEncoding=\"UTF-8\"\ protocol=\"org.apache.coyote.http11.Http11Protocol\"\ SSLEnabled=\"true\"\n\               maxThreads=\"150\"\ scheme=\"https\"\ keystoreFile=\"\/usr\/local\/tomcat\/keystore\/ssl.keystore\"\ keystorePass=\"keystorePass\"\ keystoreType=\"JCEKS\"\n\ secure=\"true\"\ connectionTimeout=\"240000\"\ truststoreFile=\"\/usr\/local\/tomcat\/keystore\/ssl.truststore\"\ truststorePass=\"truststorePass\"\ truststoreType=\"JCEKS\"\n\               clientAuth=\"want\"\ sslProtocol=\"TLS\"\ allowUnsafeLegacyRenegotiation=\"true\"\ maxHttpHeaderSize=\"32768\"\ maxSavePostSize=\"-1\" \/>/g" /usr/local/tomcat/conf/server.xml
```

* Open a Terminal and run the following command to build the custom docker repository image.

```bash
docker build . -t customrepository/custom-share-repository:customTag
```

* The new docker image was build locally, now it can be pushed to customrepository by running the following command in Terminat/CMD:

```bash
docker push customrepository/custom-share-repository:customTag
```
