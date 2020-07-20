# ACS Pipeline-all-amps Repo & Share Docker Image

The [pipeline-all-amps](../pipeline-all-amps) directory config has repo and share module with set of required amps. 
The new repo and share docker images has been released with top of some extra amps settings for CI/CD single pipeline process.

## Travis job to test and push docker images

acs-packaging repo [travis](../../.travis.yml) job `"Pipeline All AMPs tests and push docker images"` runs the tests and publishes the new docker image for repo and share with travis build number `Example: alfresco/alfresco-pipeline-all-amps-repo:3102:6.2.2`.
The pushed images has been tested using [docker-compose](../tests/environment/docker-compose-pipeline-all-amps.yml) deployment.

ACS repo and share Docker images are released to [Quay docker repo](https://quay.io/repository/alfresco/alfresco-pipeline-all-amps-repo?tab=info) and [Quay docker share](https://quay.io/repository/alfresco/alfresco-pipeline-all-amps-share?tab=info)
