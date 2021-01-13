
# Alfresco Content Services Enterprise Packaging
This project is producing packaging for Alfresco Content Services Enterprise.

License rights for this program may be obtained from Alfresco Software, Ltd.
pursuant to a written agreement and any use of this program without such an
agreement is prohibited.

https://www.alfresco.com/legal/agreements and https://www.alfresco.com/terms-use

The SNAPSHOT version of artifacts are **never** published.

### Contributing guide
Please use [this guide](CONTRIBUTING.md) to make a contribution to the project.

This project creates the `alfresco-content-repository` docker image and the distribution zip
for the Alfresco Content Services product. The image extends the `alfresco-enterprise-repo-base`
image created in the `alfresco-enterprise-repo` project, in order to add ACS components that are
are not directly part of the repository. The `alfresco-enterprise-repo` project in turn builds
on the `alfresco-community-repo` project.

# General

This project is the Enterprise equivalent of the [Community Packaging Project](https://github.com/Alfresco/acs-community-packaging).  Please ensure that you have the correct agreements in place and access to the Enterprise Maven artifacts.

# Build:
To build the project, including the distribution zip, but not the Docker images, issue the following commands:
```
$ # The entP alias includes the following:
$ cd acs-packaging
$ mvn clean install
$ cd ..
```
## Docker Alfresco
On official releases, an image is published to:
https://hub.docker.com/r/alfresco/alfresco-content-repository/tags/ 

We created another image for our AWS deployment. It adds the S3 Connector amp and MariaDB driver to the image. During a release, it will be published on: https://hub.docker.com/r/alfresco/alfresco-content-repository-aws/tags/

To build the Docker images, you will need to build the `alfresco-enterprise-repo` and `acs-packaging` projects. The simplest way is to use the ```entRD``` and ```entPD``` aliases. For more information, see [build aliases](dev/aliases). A local `latest` image is created from alfresco-enterprise-repo which forms base layer for the images created in acs-packaging.
```
entRD && entPD
```

## Docker-compose & Kubernetes
Use the https://github.com/Alfresco/acs-deployment project as the basis for your own docker-compose or helm chart deployments.


## Distribution zip
The distribution zip contains the war files, libraries, certificates and settings files you need, to deploy Alfresco Content Services on the supported application servers.


# How to

* [Create a custom Docker image](docs/create-custom-image.md)
* [Development Tomcat Environment](dev/README.md)
* [aliases](dev/aliases)
