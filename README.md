
# Alfresco Content Services Packaging
This project is producing packaging for Alfresco Content Services Repository.

The SNAPSHOT version of the artifacts is **never** published.

### Contributing guide
Please use [this guide](CONTRIBUTING.md) to make a contribution to the project.

This produces the docker images for alfresco-content-repository and the distribution zip for the entire Alfresco Content Services product

# General

### Build:
* ```mvn clean install``` in the root of the project will build everything.

## Docker Alfresco
On official releases, the image is published:
https://hub.docker.com/r/alfresco/alfresco-content-repository/tags/ 

For testing locally:
1. Go to docker-alfreco folder
2. Run *mvn clean install* if you have not done so
3. Build the docker image: ```docker build . --tag acr:6.0.tag```
4. Check that the image has been created locally with your desired name/tag: ```docker images```

### Docker-compose & Kubernetes
Use the deployment project if you want the sample docker-compose or helm: https://github.com/Alfresco/acs-deployment

## Distribution zip
In this folder the distribution zip is build. It contains all the war files, libraries, certificates and settings files you need to deploy Alfresco Content Services on the supported application servers.


