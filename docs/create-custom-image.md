## How to create customized Docker images using this project

**WARNING**

The recommended way of building extensions is to use [Rest APIs](https://api-explorer.alfresco.com/api-explorer). The AMP extension mechanism may be replaced in the future.

### Applying a custom AMP

This project uses [fabric8](https://github.com/fabric8io/docker-maven-plugin) maven plugin to build and push the Alfresco Docker image. The plugin's configuration is stored in this [pom.xml](../docker-alfresco/pom.xml). The following example will illustrate how an additional AMP can be added in the configuration.

#### Prerequisites

* The extension is packaged inside an AMP and can be accessed by Maven
* Java
* Maven
* Docker
* Access to Docker registry where the new Docker image will be pushed

#### Modifications to [pom.xml](../docker-alfresco/pom.xml)

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
mvn clean install -Ppush-docker-images -Dimage.name=mycompany/alfresco-with-s3 -Dimage.tag=latest -Dimage.registry=quay.io
```
The `internal` profile configures _docker-maven-plugin_ to build and push an image. It will use this [Dockerfile](../docker-alfresco/Dockerfile). All the AMPs from the _target/amps_ folder are copied inside the image and then the amps are applied to the standard _alfresco.war_ using [MMT tool](https://github.com/Alfresco/alfresco-mmt). After that the image is pushed to the specified Docker registry.
