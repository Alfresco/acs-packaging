
# Alfresco Content Services Enterprise Packaging
This project is producing packaging for Alfresco Content Services Repository Enterprise.

License rights for this program may be obtained from Alfresco Software, Ltd.
pursuant to a written agreement and any use of this program without such an
agreement is prohibited.

https://www.alfresco.com/legal/agreements and https://www.alfresco.com/terms-use

The SNAPSHOT version of the artifacts is **never** published.

### Contributing guide
Please use [this guide](CONTRIBUTING.md) to make a contribution to the project.

This produces the docker images for alfresco-content-repository and the distribution zip for the entire Alfresco Content Services product.

# General

### Build:
* ```mvn clean install``` in the root of the project will build everything.
* This project is the Enterprise equivalent of the [Community Packaging Project](https://github.com/Alfresco/acs-community-packaging).  Please ensure that you have the correct agreements in place and access to the Enterprise Maven artifacts.

## Docker Alfresco
On official releases, the image is published:
https://hub.docker.com/r/alfresco/alfresco-content-repository/tags/ 

For testing locally:
1. Go to docker-alfresco folder
2. Run ```mvn clean install``` if you have not done so
3. Build the docker image: ```docker build . --tag acr:6.0.tag```
4. Check that the image has been created locally, with your desired name/tag: ```docker images```

## Docker Alfresco AWS
We created another ACS image for our AWS deployment. It adds the S3 Connector amp and MariaDB driver to the image.
During a release, it will be published on:
https://hub.docker.com/r/alfresco/alfresco-content-repository-aws/tags/ 

For testing locally run:
```
mvn clean install -Dskiptests -PenterpriseDocker
```
which will create an image locally named: alfresco/alfresco-content-repository-aws:latest

### Docker-compose & Kubernetes
Use the deployment project if you want the sample docker-compose or helm: https://github.com/Alfresco/acs-deployment

## Distribution zip
In this folder the distribution zip is built. It contains all the war files, libraries, certificates and settings files you need, to deploy Alfresco Content Services on the supported application servers.


## How to

* [Create a custom Docker image](docs/create-custom-image.md)