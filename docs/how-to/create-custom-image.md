## How to create customized Docker images using this project

**WARNING**

The recommended way of building extensions is to use [Rest APIs](https://api-explorer.alfresco.com/api-explorer). The AMP extension mechanism may be replaced in the future.

### Applying a custom AMP

This project uses [fabric8](https://github.com/fabric8io/fabric8-maven-plugin) maven plugin to build and push the Alfresco Docker image. The plugin's configuration is stored in this [pom.xml](../../docker-alfresco/pom.xml). The following example will illustrate how an additional AMP can be added in the configuration.

#### Prerequisites

* The extension is packaged inside an AMP and can be accessed by Maven
* Java
* Maven
* Docker
* Access to Docker registry where the new Docker image will be pushed

#### Modifications to [pom.xml](../../docker-alfresco/pom.xml)

* Add a dependency:
```xml
<dependency>
    <groupId>org.alfresco.integrations</groupId>
    <artifactId>alfresco-s3-connector</artifactId>
    <version>2.2.0</version>
    <type>amp</type>
</dependency>
```

* Extend the _copy-resources-amps_ execution of the _maven-dependency-plugin_ in the same file by adding an artifact:
```xml
<artifactItem>
    <groupId>org.alfresco.integrations</groupId>
    <artifactId>alfresco-s3-connector</artifactId>
    <type>amp</type>
    <overWrite>false</overWrite>
    <outputDirectory>${project.build.directory}/amps</outputDirectory>
</artifactItem>
```
Maven will use _maven-dependency-plugin_ to download the AMP from Maven repository and copy it into _target/amps_ folder.

* Trigger a Maven build and specify the image name and it's storage location:
```bash
mvn clean install -Pinternal -Dimage.name=mycompany/alfresco-with-s3 -Dimage.tag=latest -Dimage.registry=quay.io
```
The _internal_ profile configures _fabric8-maven-plugin_ to build and push an image. It will use this [Dockerfile](../../docker-alfresco/Dockerfile). All the AMPs from the _target/amps_ folder are copied inside the image and then the amps are applied to the standard _alfresco.war_ using [MMT tool](https://github.com/Alfresco/alfresco-mmt). After that the image is pushed to the specified Docker registry  

## How to create customized Docker images using an existing docker image.

**WARNING**

The recommended way of building extensions is to use [Rest APIs](https://api-explorer.alfresco.com/api-explorer). The AMP extension mechanism may be replaced in the future.

#### Prerequisites

* The extension is packaged inside an AMP
* Some magic
* Docker
* Access to Docker registry where the new Docker image will be pushed

### Amps that don`t require additional configurations (easy).

***Note:*** We are going to use a fork of the [alfresco-bulk-import tool](https://github.com/Epurashu/alfresco-bulk-import), this is compatible with Alfresco 6.0.X(enterprise)/6.0.3(community) and above.
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
* Clone and download the project.

* Trigger a Maven build from the root folder of the project (SOMEPATH/alfresco-bulk-import).

```bash
mvn clean install
```

* Copy SOMEPATH/alfresco-bulk-import/amp/target/alfresco-bulk-import-*.amp under alfresco-custom-image/amps/.

* Create Dockerfile under alfresco-custom-image/

* Add the following lines to the Dockerfile:

```bash
### We are going to apply amp(s) on the latest community repository image.
FROM alfresco/alfresco-content-repository-community

### Copy the amps from build context to the appropriate location for your application server
COPY amps /usr/local/tomcat/amps

### Install amps on alfresco
RUN java -jar /usr/local/tomcat/alfresco-mmt/alfresco-mmt*.jar install \
              /usr/local/tomcat/amps/alfresco-bulk-import-*.amp /usr/local/tomcat/webapps/alfresco -nobackup -force
```

* Open Terminal and run the following command to build the custom docker image.

```bash
docker build . -t customrepository/alfresco-custom-image:customTag
```

* The new docker image was build locally, now it can be pushed to customrepository by running the following command in Terminat/CMD:

```bash
docker push customrepository/alfresco-custom-image:customTag
```

*** Note: *** It is not necessary to build/have amps locally and copy them to the new docker image, if the .amp can be downloaded via network (FTP/HTTP/HTTPS), COPY can be replaced by CURL/WGET commands.


### Amps that require additional configurations (advanced).

***Note:*** We are going to use alfresco-saml-distribution in order to install Alfresco-SAML-Module(testing purposes only! it is recommended to use a proxy for SSL).
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

* Append the content of "share-config-custom.xml" to the one used in the alfresco-share docker image (the content that needs to be added is to big for the use of a SED command).

* Add files to match the above hierarchy.

* Create Dockerfile for custom-alfresco-repository image:

```
### We are going to apply amp(s) on the latest enterprise repository image.
FROM alfresco/alfresco-content-repository

### Copy the keystore build context to the appropriate location for your application server
COPY keystore /usr/local/tomcat/keystore

### Copy the amps from build context to the appropriate location for your application server
COPY amps /usr/local/tomcat/amps


### Install amps on alfresco
RUN java -jar /usr/local/tomcat/alfresco-mmt/alfresco-mmt*.jar install \
              /usr/local/tomcat/amps/alfresco-saml-repo-1.1.0-*.amp /usr/local/tomcat/webapps/alfresco -nobackup -force

### Enable SSL by adding the proper Connector to server.xml (using a very hard to understand SED command)
RUN sed -i "s/\    <\/Engine>/\n\    <\/Engine>\n\    <Connector\ port=\"8443\"\ URIEncoding=\"UTF-8\"\ protocol=\"org.apache.coyote.http11.Http11Protocol\"\ SSLEnabled=\"true\"\n\               maxThreads=\"150\"\ scheme=\"https\"\ keystoreFile=\"\/usr\/local\/tomcat\/keystore\/ssl.keystore\"\ keystorePass=\"keystorePass\"\ keystoreType=\"JCEKS\"\n\ secure=\"true\"\ connectionTimeout=\"240000\"\ truststoreFile=\"\/usr\/local\/tomcat\/keystore\/ssl.truststore\"\ truststorePass=\"truststorePass\"\ truststoreType=\"JCEKS\"\n\               clientAuth=\"want\"\ sslProtocol=\"TLS\"\ allowUnsafeLegacyRenegotiation=\"true\"\ maxHttpHeaderSize=\"32768\"\ maxSavePostSize=\"-1\" \/>/g" /usr/local/tomcat/conf/server.xml
```

* Open Terminal and run the following command to build the custom docker repository image.

```bash
docker build . -t customrepository/custom-alfresco-repository:customTag
```

* The new docker image was build locally, now it can be pushed to customrepository by running the following command in Terminat/CMD:

```bash
docker push customrepository/custom-alfresco-repository:customTag
```

* Create Dockerfile for custom-alfresco-share image:

```
### We are going to apply amp(s) on the latest share image.
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

### Enable SSL by adding the proper Connector to server.xml (using a very hard to understand SED command)
RUN sed -i "s/\    <\/Engine>/\n\    <\/Engine>\n\    <Connector\ port=\"8443\"\ URIEncoding=\"UTF-8\"\ protocol=\"org.apache.coyote.http11.Http11Protocol\"\ SSLEnabled=\"true\"\n\               maxThreads=\"150\"\ scheme=\"https\"\ keystoreFile=\"\/usr\/local\/tomcat\/keystore\/ssl.keystore\"\ keystorePass=\"keystorePass\"\ keystoreType=\"JCEKS\"\n\ secure=\"true\"\ connectionTimeout=\"240000\"\ truststoreFile=\"\/usr\/local\/tomcat\/keystore\/ssl.truststore\"\ truststorePass=\"truststorePass\"\ truststoreType=\"JCEKS\"\n\               clientAuth=\"want\"\ sslProtocol=\"TLS\"\ allowUnsafeLegacyRenegotiation=\"true\"\ maxHttpHeaderSize=\"32768\"\ maxSavePostSize=\"-1\" \/>/g" /usr/local/tomcat/conf/server.xml
```

* Open Terminal and run the following command to build the custom docker repository image.

```bash
docker build . -t customrepository/custom-share-repository:customTag
```

* The new docker image was build locally, now it can be pushed to customrepository by running the following command in Terminat/CMD:

```bash
docker push customrepository/custom-share-repository:customTag
```