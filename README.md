
# Alfresco Content Services Enterprise Packaging
This project is producing packaging for Alfresco Content Services Enterprise.

License rights for this program may be obtained from Alfresco Software, Ltd.
pursuant to a written agreement and any use of this program without such an
agreement is prohibited.

https://www.alfresco.com/legal/agreements and https://www.alfresco.com/terms-use

The SNAPSHOT version of artifacts are **never** published.

### Contributing guide
Please use [this guide](CONTRIBUTING.md) to make a contribution to the project.

# General

This project is the Enterprise equivalent of the [Community Packaging Project](https://github.com/Alfresco/acs-community-packaging).
Please ensure that you have the correct agreements in place and access to the Enterprise Maven artifacts.

This project creates the `alfresco/alfresco-content-repository` and `alfresco/alfresco-share` docker images. It also
creates and the distribution zips for the Alfresco Content Services product.

The `alfresco/alfresco-content-repository` image extends the `alfresco-enterprise-repo-base` created by the
`alfresco-enterprise-repo` project to add additional ACS components.

The `alfresco/alfresco-share` image extends the `alfresco-share-base` created by the `alfresco-enterprise-share`
project.

# Build:
For more detailed build instructions, see the [Development Tomcat Environment](https://github.com/Alfresco/acs-packaging/tree/master/dev/README.md)
page.

To build the project, including the distribution zip, but not the Docker images, issue the following commands:
```
$ # The entP alias includes the following:
$ cd acs-packaging
$ mvn clean install
$ cd ..
```
## Docker Alfresco
Releases are published to https://quay.io/repository/alfresco/alfresco-content-repository?tab=tags

To build the Docker images, you will need to build the `alfresco-community-repo`, `alfresco-enterprise-repo` and
`acs-packaging` projects. The simplest way is to use the `comR`, `entRD` and `entPD` aliases.
For more information, see [build aliases](dev/aliases).  `latest` images are created locally.
```
comR && entRD && entPD
```

## Docker-compose & Kubernetes
Use the https://github.com/Alfresco/acs-deployment project as the basis for your own docker-compose or helm chart deployments.


## Distribution zip
The distribution zip contains the war files, libraries, certificates and settings files you need, to deploy
Alfresco Content Services on the supported application servers.


# How to

* [Development Tomcat Environment](dev/README.md)
* [aliases](dev/aliases)
* [Create a custom Docker image](docs/create-custom-image.md)
