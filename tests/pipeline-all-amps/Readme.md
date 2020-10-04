# ACS Pipeline-all-amps Repo & Share Docker Image

The [pipeline-all-amps](../pipeline-all-amps) module pushes repo and share images with extra amps applied on top for use by the CI/CD single pipeline process.

## Travis job to test and push docker images

acs-packaging repo [travis](../../.travis.yml) job `"Pipeline All AMPs tests and push docker images"`  publishes internally the new docker images for repo and share with travis build number Example: alfresco/alfresco-pipeline-all-amps-repo:3102:6.2.3. The images has been tested using docker-compose deployment.

Repo and share images are pushed [Quay docker repo](https://quay.io/repository/alfresco/alfresco-pipeline-all-amps-repo?tab=info) and [Quay docker share](https://quay.io/repository/alfresco/alfresco-pipeline-all-amps-share?tab=info)
