For context see [Custom Transforms and Renditions](custom-transforms-and-renditions.md).

# Creating a T-Engine

This page will describe how to develop, configure and run custom transformers as T-Engines. You may also find the [alfresco-base-t-engine README](https://github.com/Alfresco/alfresco-transform-core/blob/master/engines/base/README.md) useful.
 
We will use code from an example [helloworld-t-engine](https://github.com/Alfresco/alfresco-helloworld-transformer/tree/master/helloworld-t-engine)
project in GitHub. This T-Engine contains a single transformer but there
may be many in a single T-Engine. The transformer takes a source text file
containing a name and produces an HTML file with the message: Hello &lt;name>.
To show how to use Renditions it also takes a transform option that specifies
which language to use.

It is assumed that the reader has some familiarity with the following
technologies:
* Spring Boot
* Maven
* Docker

Custom T-Engines may also be used to extract or embed Metadata, as they are just a specialist form of transform.
For more information see [metadata-extract-embed.md](metadata-extract-embed.md).

## Developing and Debugging T-Engines

T-Engines are Dockerized Spring Boot applications. They are set up as
Maven projects built on top of
[alfresco-base-t-engine](https://github.com/Alfresco/alfresco-transform-core/tree/master/engines/base),
which is a sub project of Alfresco Transform Core.
The Alfresco Transformer Base brings in Spring Boot capabilities
as well as base classes which assist in the creation of new T-Engines.
We are going to take a look at:
 * How to set up a project.
 * How to specify T-Engine configuration, which declares what the T-Engine can transform.
 * How to implement a Custom T-Engine, which includes the code that performs the transform.
 * Running and testing the T-Engine.
 * Useful debug options.

### Project setup

In order to configure a custom T-Engine as a Spring Boot
application in a Docker image, we need to add some configuration.
The quickest way to get started is to base your project on
[helloworld-t-engine](https://github.com/Alfresco/alfresco-helloworld-transformer/tree/master/helloworld-t-engine),
as it is fully configured, ready to be built and run and contains
relatively little extra code. It is also possible to start from a blank
Maven project with the same folder structure. Key files:

* [pom.xml](https://github.com/Alfresco/alfresco-helloworld-transformer/blob/master/helloworld-t-engine/pom.xml)
  The POM file defines Alfresco Transform Core as the parent and adds
  required dependencies. It also configures plugins for building
  the Spring Boot application and generating the Docker image. It is
  likely you will need to change the artifact name and add extra dependencies.
* [HelloTransformEngine.java](https://github.com/Alfresco/alfresco-helloworld-transformer/blob/master/helloworld-t-engine/src/main/java/org/alfresco/transform/HelloTransformEngine.java)
  Implements an interface that defines the T-Engine.
* [HelloTransformer.java](https://github.com/Alfresco/alfresco-helloworld-transformer/blob/master/helloworld-t-engine/src/main/java/org/alfresco/transform/HelloTransformEngine.java)
  Implements an interface to provide the transformation.
* [Dockerfile](https://github.com/Alfresco/alfresco-helloworld-transformer/blob/master/helloworld-t-engine/Dockerfile)
  The Dockerfile is needed by the **docker-maven-plugin** configured in
  the **pom.xml** to generate a docker image.
  It defines a simple Docker image with our Spring Boot application fat
  jar copied in, specifies default user information and exposes port 8090.

### Custom T-Engine

T-Engines need to implement a [TransformEngine](https://github.com/Alfresco/alfresco-transform-core/blob/master/engines/base/src/main/java/org/alfresco/transform/base/TransformEngine.java)
and one or more [CustomTransformer](https://github.com/Alfresco/alfresco-transform-core/blob/master/engines/base/src/main/java/org/alfresco/transform/base/CustomTransformer.java)s.
The alfresco-base-t-engine takes care of the rest. Take a look at the
[HelloWorldTransformEngine.java](https://github.com/Alfresco/alfresco-helloworld-transformer/blob/master/helloworld-t-engine/src/main/java/org/alfresco/transform/HelloWorldTransformEngine.java)
and [HelloWorldTransformer.java](https://github.com/Alfresco/alfresco-helloworld-transformer/blob/master/helloworld-t-engine/src/main/java/org/alfresco/transform/HelloWorldTransformer.java).

The TransformEngine and CustomTransformers implementations **MUST** be Spring components (have a
`@Component` annotation) and be in or under `org.alfresco.transform`. To use additional packages see
[Using custom package names](https://github.com/Alfresco/alfresco-helloworld-transformer/blob/master/helloworld-t-engine/README.md#using-custom-package-names)

#### CustomTransformer transform

```java
    void transform(String sourceMimetype, InputStream inputStream,
        String targetMimetype, OutputStream outputStream,
        Map<String, String> transformOptions, TransformManager transformManager) throws Exception;
```

Method parameters:

* **sourceMimetype** mimetype of the source
* **inputStream** of the source
* **targetMimetype** mimetype of the target
* **outputStream** to the target
* **transformOptions** transform options from the client
* **transforManager** provides methods to interact with the base t-engine including using Files rather than Streams

The helloworld example does all the actual transform processing in this method for simplicity, but if you look at
the core T-Engines, you will see they frequently offload the work to other classes.


##### TransformEngine getTransformConfig

```java
    @Override
    public TransformConfig getTransformConfig()
    {
        return transformConfigResourceReader.read("classpath:engine_config.json");
    }

```

This method normally returns config read from a JSON file called `engine_config.json`.

The following [engine_config.json](https://github.com/Alfresco/alfresco-helloworld-transformer/blob/master/helloworld-t-engine/src/main/resources/engine_config.json)
is taken from the Hello World example, but there are other examples such
as the one used by the [Tika T-Engine](https://github.com/Alfresco/alfresco-transform-core/blob/master/engines/tika/src/main/resources/tika_engine_config.json).


```json
{
  "transformOptions":
  {
    "helloWorldOptions":
    [
      {"value": {"name": "language"}}
    ]
  },
  "transformers":
  [
    {
      "transformerName": "helloWorld",
      "supportedSourceAndTargetList":
      [
        {"sourceMediaType": "text/plain",  "maxSourceSizeBytes": 50, "targetMediaType": "text/html"  }
      ],
      "transformOptions":
      [
        "helloWorldOptions"
      ]
    }
  ]
}
```

* **transformOptions** provides a list of transform options that may be
  referenced for use in different transformers. This way common options
  don't need to be repeated for each transformer. They can even be
  shared between T-Engines. In this example there is only one group of
  options called **helloWorldOptions**, which has just one option the
  **language**. Unless an option has a **"required": true** field it is
  considered to be optional. If you look at the [Tika T-Engine](https://github.com/Alfresco/alfresco-transform-core/blob/master/engines/tika/src/main/resources/tika_engine_config.json)
  file, you can see options may also be grouped. You don't need to
  specify *sourceMimetype*, *targetMimetype* or *timeout* as options as
  these are available to all transformers.

* **transformers** - A list of transformer definitions.
  Each transformer definition should have a unique **transformerName**,
  specify a **supportedSourceAndTargetList** and indicate which
  options it supports. In this case there is only one transformer called
  *Hello World* and it accepts *helloWorldOptions*. A transformer may
  specify references to 0 or more transformOptions.

* **supportedSourceAndTargetList** is simply a list of source and target
  Media Types that may be transformed, optionally specifying
  **maxSourceSizeBytes** and **priority** values. In this case there is only one from
  text to HTML and we have limited the source file size, to avoid
  transforming files that clearly don't contain names.


##### TransformEngine getProbeTransform

```java
public ProbeTransform getProbeTransform()
```

This method provides a way to define a test transform for
[T-Engine Probes](https://github.com/Alfresco/alfresco-transform-core/blob/master/docs/Probes.md).
For example, a test transform of a small file included in the Docker image.

### Running and Debugging

#### Hello World T-Engine Standalone

This section will describe how to run and debug the example Hello World T-Engine
which can be found in the [helloworld-t-engine](https://github.com/Alfresco/alfresco-helloworld-transformer/tree/master/helloworld-t-engine).

1. Clone the **alfresco-helloworld-transformer** project and navigate to the **helloworld-t-engine** folder.
2. Build the T-Engine
    ```bash
    mvn clean install -Pbuild-docker-images
    ```
3. Start the T-Engine
    ```bash
    docker run -d -p 8090:8090 --name helloworld-t-engine alfresco/helloworld-t-engine:latest
    ```
4. Create a test file named **source_file.txt** with the following content:
    ```text
    T-Engines
    ```
5. In a browser go to http://localhost:8090/. For convenience the
Hello World T-Engine, provides a test page to POST requests to the **/transform** endpoint.
6. In the HTML Form, choose the **source_file.txt**.
Specify a language, supported languages are: English, Spanish, German.
7. Enter `text/plain` as the source mimetypes
8. Select `html` from the target mimetypes choice dropdown.
9. Click **Transform** and then view the downloaded file.

#### Logs

T-Engines provide a `/log` endpoint out of the box which shows
information about transformations performed by the T-Engine.
In addition, the T-Engine server logs can be accessed using
the Docker `logs` command.
For more information see [Docker documentation](https://docs.docker.com/engine/reference/commandline/logs/).
```bash
docker logs helloworld-t-engine
```

#### Hello World T-Engine with the ACS repository

This section will describe how to configure a development environment
and run Alfresco Content Services with the new Hello World T-Engine.

> The example in this section uses Docker Compose for simplicity;
however, it is not recommended to run ACS in Docker Compose in production.

1. If not done already, clone the **alfresco-helloworld-transformer** project and navigate to
the **helloworld-t-engine** folder.
2. If not done already, build the Hello World T-Engine from the **helloworld-t-engine** folder.
Check that the local Docker image repository contains **alfresco/helloworld-t-engine:latest**.
3. Clone [Alfresco/acs-deployment](https://github.com/Alfresco/acs-deployment) project.
4. Modify asc-deployment [docker-compose](https://github.com/Alfresco/acs-deployment/blob/master/docker-compose/docker-compose.yml)
file by adding the Hello World T-Engine as one of the services.
    ```yaml
    transform-helloworld:
        image: alfresco/helloworld-t-engine:latest
        environment:
            JAVA_OPTS: " -Xms256m -Xmx256m"
            ACTIVEMQ_URL: "nio://activemq:61616"
            ACTIVEMQ_USER: "admin"
            ACTIVEMQ_PASSWORD: "admin"
            FILE_STORE_URL: "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file"
        ports:
            - 8096:8090
    ```

    Add a `localTransform.{transformer}.url` property to the Alfresco
    service JAVA_OPTS. See [here](custom-transforms-and-renditions.md#configure-a-t-engine-as-a-local-transform)
    for details. For Docker Compose, the **transform-helloworld**
    in the URL http://**transform-helloworld**:8090/ has to match
    the service name defined above.
    ```
    -DlocalTransform.helloworld.url=http://transform-helloworld:8090/
    ```

5. Create a custom **helloWorld** rendition which will use the new
Hello World T-Engine. See [here](custom-transforms-and-renditions.md#configure-a-custom-rendition) for details.
    ```json
    {
      "renditions": [
        {
          "renditionName": "helloWorld",
          "targetMediaType": "text/html",
          "options": [
            {"name": "language", "value": "German"}
          ]
        }
      ]
    }
    ```

6. Start ACS using the modified docker-compose file.
    ```bash
    docker-compose up
    ```

#### Test custom rendition

This section walks through an end to end example of using
the Hello World T-Engine with ACS by requesting the **helloWorld** rendition.

1. Create a test file named **source_file.txt** with the following content:
   ```text
   T-Engines
   ```
2. Upload the file using [REST API](https://api-explorer.alfresco.com/api-explorer/#!/nodes/createNode)
and write down the **id** in the response, this is the **nodeId**
used in following requests.
    ```bash
    curl -u admin:admin -X POST localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/nodes/-my-/children -F filedata=@source_file.txt
    ```
3. Request a list of available renditions using [REST API](https://api-explorer.alfresco.com/api-explorer/#!/renditions/listRenditions)
on the uploaded file.
Notice that the custom **helloWorld** is in the list of available
renditions.
    ```bash
    curl -u admin:admin -X GET localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/nodes/{nodeId}/renditions
    ```
4. Request the **helloWorld** rendition to be created using [REST API](https://api-explorer.alfresco.com/api-explorer/#!/renditions/createRendition).
    ```bash
    curl -u admin:admin -X POST localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/nodes/{nodeId}/renditions -d '{"id":"helloWorld"}' -H "Content-Type: application/json"
    ```
5. Request the rendered file using [REST API](https://api-explorer.alfresco.com/api-explorer/#!/renditions/getRenditionContent)
and save it to **hello_world_rendition.html**.
    ```bash
    curl -u admin:admin -X GET localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/nodes/{nodeId}/renditions/helloWorld/content -o hello_world_rendition.html
    ```
6. Verify that the returned HTML file contains a Hello World greeting
in the language specified in the **helloWorld** rendition transform options.

#### Logs and Debugging

Log4j `DEBUG` level logging for the transformations code can be enabled
in ACS log4j properties on the following packages:
```properties
# For normal transform debug
logger.alfresco-repo-content-transform-TransformerDebug.name=org.alfresco.repo.content.transform.TransformerDebug
logger.alfresco-repo-content-transform-TransformerDebug.level=debug

# For loading of custom JSON files
logger.alfresco-enterprise-repo-rendition2-RemoteTransformServiceRegistry.name=org.alfresco.enterprise.repo.rendition2.RemoteTransformServiceRegistry
logger.alfresco-enterprise-repo-rendition2-RemoteTransformServiceRegistry.level=debug

logger.alfresco-repo-content-transform-LocalTransformServiceRegistry.name=org.alfresco.repo.content.transform.LocalTransformServiceRegistry
logger.alfresco-repo-content-transform-LocalTransformServiceRegistry.level=debug

logger.alfresco-repo-rendition2-RenditionDefinitionRegistry2Impl.name=org.alfresco.repo.rendition2.RenditionDefinitionRegistry2Impl
logger.alfresco-repo-rendition2-RenditionDefinitionRegistry2Impl.level=debug

logger.alfresco-repo-content-MimetypeMap.name=org.alfresco.repo.content.MimetypeMap
logger.alfresco-repo-content-MimetypeMap.level=debug

# For even more detailed debug
logger.alfresco-enterprise-repo-rendition2-RemoteTransformClient.name=org.alfresco.enterprise.repo.rendition2.RemoteTransformClient
logger.alfresco-enterprise-repo-rendition2-RemoteTransformClient.level=debug

logger.alfresco-repo-rendition2-LocalTransformClient.name=org.alfresco.repo.rendition2.LocalTransformClient
logger.alfresco-repo-rendition2-LocalTransformClient.level=debug

logger.alfresco-repo-content-transform-LocalTransform.name=org.alfresco.repo.content.transform.LocalTransform
logger.alfresco-repo-content-transform-LocalTransform.level=debug

logger.alfresco-repo-rendition2.name=org.alfresco.repo.rendition2
logger.alfresco-repo-rendition2.level=debug

logger.alfresco-enterprise-repo-rendition2.name=org.alfresco.enterprise.repo.rendition2
logger.alfresco-enterprise-repo-rendition2.level=debug
```

In addition, the `Alfresco Admin Tool` provides a transformers debugging
tool called `Test Transform` under the `Support Tools` section.