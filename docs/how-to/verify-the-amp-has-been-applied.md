## How to verify that amps have been applied.  
  
**Note:** Please follow **"Customising alfresco deployment"** section from [acs-deployment project](https://github.com/Alfresco/acs-deployment#customising-alfresco-deployment) in order to properly configure your deployment mechanism (Helm charts/ docker-compose) to use the new custom docker images.

#### Prerequisites

* Docker
* Access to the custom docker image (docker registry/local image).

### Using alfresco-mmt tool.
**Notes:**  
  -There is no need to spin up the whole deployment solution.  
  -All of the following commands should be run in a Terminal.

* Run the custom docker image (alfresco/share).

```bash
docker run alfresco-custom-image
```

* Get the id of the alfresco-custom-image container by listing containers:

```bash
docker ps
```

* Connect to the alfresco-custom-image docker container:

```bash
docker exec -it CONTAINER_ID bash
```

* Using alfresco-mmt tool list all the amps inside a webapp (e.g. alfresco):

```bash
java -jar alfresco-mmt/alfresco-mmt-6.0.jar list webapps/alfresco
```

* All installed amps are displayed. Now we can exit the container.

```bash
exit
```

* It is a good practice to kill and remove the docker container:

```bash
docker kill CONTAINER_ID
docker rm CONTAINER_ID
```

### Monitoring the logs.

* Spin up alfresco solution.

* View the logs for the custom docker container and observe that the amp module has startup:

```bash
2018-06-04 13:27:22,103  INFO  [repo.module.ModuleServiceImpl] [localhost-startStop-1] Starting module 'alfresco-aos-module' version 1.2.0.
2018-06-04 13:27:22,121  INFO  [repo.module.ModuleServiceImpl] [localhost-startStop-1] Starting module 'alfresco-saml-repo' version 1.1.0.
2018-06-04 13:27:22,154  INFO  [repo.module.ModuleServiceImpl] [localhost-startStop-1] Starting module 'org.alfresco.integrations.google.docs' version 3.1.0-RC1.
2018-06-04 13:27:22,170  INFO  [repo.module.ModuleServiceImpl] [localhost-startStop-1] Starting module 'alfresco-share-services' version 6.0.0.
2018-06-04 13:27:22,186  INFO  [repo.module.ModuleServiceImpl] [localhost-startStop-1] Starting module 'alfresco-trashcan-cleaner' version 2.3.
```