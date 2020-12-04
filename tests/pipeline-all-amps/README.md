# ACS Pipeline-all-amps Repo & Share Docker Image

The [pipeline-all-amps](../pipeline-all-amps) module pushes repo and share images with extra amps applied on top for use by the CI/CD single pipeline process.

## Travis job to test and push docker images

The acs-packaging repo [travis](../../.travis.yml) job `"Pipeline All AMPs tests and push docker images"` publishes internally the new docker images for repo and share
with travis build number

Example: alfresco/alfresco-pipeline-all-amps-repo:3102:7.0.0.

The images have been tested using docker-compose deployment.

Repo and share images are pushed to [Quay.io](https://quay.io):
* repo: [alfresco/alfresco-pipeline-all-amps-repo](https://quay.io/repository/alfresco/alfresco-pipeline-all-amps-repo?tab=info)
* share: [alfresco/alfresco-pipeline-all-amps-share](https://quay.io/repository/alfresco/alfresco-pipeline-all-amps-share?tab=info)

Both images support a variable called `ALFRESCO_AMPS` that allows selecting which AMPs should be enabled, by default its value is `ALL`.

## Test locally in development

To test locally in development, configure your maven settings as in the main project, then run the following provided scripts:

```shell
BUILD_ENABLED=true
(cd repo && BUILD_ENABLED=$BUILD_ENABLED ./run-repo.sh)
(cd share && BUILD_ENABLED=$BUILD_ENABLED ./run-share.sh)
```

where `BUILD_ENABLED` can be set to false in following run to avoid executing maven. 

In order to test just the AMP install a CMD argument can be provided when running the image, for example:

```shell
cd repo && BUILD_ENABLED=true ./run-repo.sh bash
```
