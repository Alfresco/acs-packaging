For context see [Custom Transforms and Renditions](custom-transforms-and-renditions.md).

# Creating a T-Engine

This page will describe how to develop, configure and run a custom
T-Engine. We will use an example Hello World T-Engine as a reference
throughout this section. The Hello World T-Engine project can be found
[here](https://github.com/Alfresco/alfresco-helloworld-transformer).

It is assumed that the reader has some familiarity with the following
technologies:
* Spring Boot
* Maven
* Docker

## Developing and Debugging T-Engines

T-Engines are Dockerized Spring Boot applications.
New T-Engines are set up as Maven projects built on top of
[Alfresco Transform Core](https://github.com/Alfresco/alfresco-transform-core).
The Alfresco Transform Core project brings in Spring Boot capabilities
as well as base classes which allow us to easily develop new T-Engines.
Using the provided example T-Engine, we are going to take a look at:
 * How to [set up](#project-setup) a T-Engine as a Dockerized Spring
 application.
 * How to specify [T-Engine configuration](#t-engine-configuration).
 * How to implement a [T-Engine controller](#custom-transform-api).

### Project setup

In order to configure a custom T-Engine to be built as a Spring Boot
application
in a Docker image, we need to add some configuration.
The quickest way to get started is to clone the example T-Engine from
[here](https://github.com/Alfresco/alfresco-helloworld-transformer),
this example is fully configured and ready to be built and run.
Alternatively, we can create a blank Maven project with the same folder
structure as in the example project.

The following configuration files are required:

[pom.xml](https://github.com/Alfresco/alfresco-helloworld-transformer/blob/master/pom.xml)

The POM file defines Alfresco Transform Core as the parent and adds
required dependencies. It also configures plugins for building
the Spring Boot application and generating the Docker image.

[Dockerfile](https://github.com/Alfresco/alfresco-helloworld-transformer/blob/master/Dockerfile)

The Dockerfile is needed by the **docker-maven-plugin** configured in
the **pom.xml** to generate a docker image.
It defines a simple Docker image with our Spring Boot application fat
jar copied in, specifies default user information and exposes port 8090.

[Application.java](https://github.com/Alfresco/alfresco-helloworld-transformer/blob/master/src/main/java/org/alfresco/transformer/Application.java)

The Application class defines an entry point for the Spring Boot
application.

### T-Engine configuration

See ACS side of the configuration
[here](custom-transforms-and-renditions.md#configure-a-t-engine-as-a-local-transform).

Each T-Engine has to provide engine configuration which is read by
T-Engine's clients to determine supported transformations.
The configuration is specified in terms of source and target Media Types
and transformOptions.

Keywords:
* **sourceMediaType** - Media Type of the file sent to the T-Engine in
a transform request.
* **targetMediaType** - Media Type of the file returned by the T-Engine
in a transform response.
* **transformOptions** - Custom list of (key, value) pairs supplied to
the T-Engine in a transform request.

The following engine configuration is from the example T-Engine.
The T-Engine provides a simple content transform which takes
a source text file and transforms it into a target HTML file.
The target HTML file will say
"Hello World!, Hello {name from the source file}!" in the language
requested via transformOptions.

[engine_config.json](https://github.com/Alfresco/alfresco-helloworld-transformer/blob/master/src/main/resources/engine_config.json)
```yaml
{
  "transformOptions":
  {
    "exampleOptions":
    [
      {"value": {"name": "language"}}
    ]
  },
  "transformers":
  [
    {
      "transformerName": "helloWorldTransformer",
      "supportedSourceAndTargetList":
      [
        {"sourceMediaType": "text/plain",  "targetMediaType": "text/html"  }
      ],
      "transformOptions":
      [
        "exampleOptions"
      ]
    }
  ]
}
```

* **transformOptions** - A custom list of transformer options required
by the T-Engine to perform the transform. The options have a unique name
such as *exampleOptions* and a list of option names, in this case just
*language*.
    > In addition to the transform options defined in the engine
    configuration and their static values supplied by [rendition definitions](#configure-a-custom-rendition),
    ACS appends the following dynamic options with each transform request:
    * sourceMimetype
    * targetMimetype
    * sourceExtension
    * targetExtension

* **transformers** - A list of transformer definitions.
Each transformer definition has a unique **transformerName**,
**supportedSourceAndTargetList** of supported **sourceMediaTypes** and **targetMediaTypes**
and a reference to **transformOptions**.

The **engine_config.json** file has to be provided at the top level
of the `resources` folder (same as in the example) with the same name
(engine_config.json).
No additional wiring is required for this file to be served by the
T-Engine.

> ACS uses transformOptions in its transformer selection strategy.
For this reason it is recommended to prefix option names with
a namespace to prevent clashes.

### Custom transform API

T-Engines define their endpoints through an annotated Spring controller
provided by Alfresco Transform Core.
The [HelloWorldController.java](https://github.com/Alfresco/alfresco-helloworld-transformer/blob/master/src/main/java/org/alfresco/transformer/HelloWorldController.java)
in the example T-Engine illustrates how to implement such controller.


```java
@PostMapping(value = "/transform", consumes = MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<Resource> transform(HttpServletRequest request,
                                              @RequestParam("file") MultipartFile sourceMultipartFile,
                                              @RequestParam(value = "targetExtension") String targetExtension,
                                              @RequestParam(value = "language") String language)
```

The handler method for the `/transform` endpoint, it serves HTTP
requests for transforms. ACS will make requests to this endpoint when
configured to use Local Transforms.

Method parameters:

* **sourceMultipartFile** - The file to be transformed from the
transform request. This is always provided in ACS requests.
* **targetExtension** - The target extension of the transformed file
to be returned in the response.
This is always provided in ACS requests.
* **language** - This is the custom transform option defined for
the example T-Engine.

The `transform` method's signature will vary depending on
the [T-Engine's configuration](#t-engine-configuration).
The example T-Engine is configured to take a single `language`
transform option, but the number of the `transform` method's
parameters will have to match the transform options defined in [engine_config.json]((https://github.com/Alfresco/alfresco-helloworld-transformer/blob/master/src/main/resources/engine_config.json)).

```java
public void processTransform(File sourceFile, File targetFile, Map<String, String> transformOptions, Long timeout)
```

This method is called by requests which come in through a message queue
used by the Transform Service. It performs the same transform as
the `/transform` endpoint.

```java
public ProbeTestTransform getProbeTestTransform()
```

This method provides a way to define a test transform for
[T-Engine Probes](https://github.com/Alfresco/alfresco-transform-core/blob/master/docs/Probes.md).
For example, a test transform on a file included in the same Docker image.

### Running and Debugging

#### Hello World T-Engine

This section will describe how to run and debug the example [Hello World T-Engine](https://github.com/Alfresco/alfresco-helloworld-transformer).
Instructions on how to build and run the T-Engine are described in
the project's **README.md** and also specified below.
The Hello World T-Engine transform takes an input text file
and a `language` **transformOption** and returns a html file.
See [T-Engine configuration](#t-engine-configuration) for details.

1. Clone the **Hello World T-Engine** project.
2. Build the T-Engine
    ```bash
    mvn clean install -Plocal
    ```
3. Start the T-Engine
    ```bash
    docker run -d -p 8090:8090 --name alfresco-helloworld-transformer alfresco/alfresco-helloworld-transformer:latest
    ```
4. Create a **source_file.txt** file with the following content:
    ```text
    T-Engines
    ```

4. Send a HTTP POST request to the /transform. The Hello World T-Engine
provides a convenience HTML form to do this.
Once the T-Engine is running, the form can be accessed at: http://localhost:8090/

5. In the HTML Form, choose the **source_file.txt**.
Specify a language, supported languages are: English, Spanish, German.
6. Click **Transform**
7. Verify that the returned HTML contains a Hello World greeting in the
specified language.

##### Logs

T-Engines provide a `/log` endpoint out of the box which shows
information about transformations performed by the T-Engine.
In addition, the T-Engine server logs can be accessed using
the Docker `logs` command.
For more information see [Docker documentation](https://docs.docker.com/engine/reference/commandline/logs/).
```bash
docker logs alfresco-helloworld-transformer
```

#### Hello World T-Engine with ACS

This section will describe how to configure a development environment
and run Alfresco Content Services with the new Hello World T-Engine.

> The example in this section uses Docker Compose for simplicity;
however, it is not recommended to run ACS in Docker Compose in production.

1. Clone the [Hello World T-Engine](https://github.com/Alfresco/alfresco-helloworld-transformer)
project (if not done already).
2. Build the Hello World T-Engine, (if not done already).
Check that the local Docker image repository contains
**alfresco/alfresco-helloworld-transformer:latest**
3. Clone the [ACS](https://github.com/Alfresco/acs-deployment) project.
4. Modify the ACS [docker-compose]https://github.com/Alfresco/acs-deployment/blob/master/docker-compose/docker-compose.yml
file by adding the Hello World T-Engine as one of the services.
    ```yaml
    transform-helloworld:
        image: alfresco/alfresco-helloworld-transformer:latest
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
    in the URL http://**transform-helloworld**:8090/ has the match
    the service name defined above.
    ```
    -DlocalTransform.helloworld.url=http://transform-helloworld:8090/
    ```

5. Create a custom **helloWorld** rendition which will use the new
Hello World T-Engine. See [here](custom-transforms-and-renditions.md#configure-a-custom-rendition) for details.
    ```json
    {
        "renditionName": "helloWorld",
        "targetMediaType": "text/html",
        "options": [
            {"name": "language", "value": "German"}
        ]
    }
    ```

6. Start ACS using the modified docker-compose file.
    ```bash
    docker-compose up
    ```

##### Test custom rendition

This section walks through an end to end example of using
the Hello World T-Engine with ACS by requesting the **helloWorld** rendition.

1. Create a **source_file.txt** file with the following content:
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

##### Logs and Debugging

Log4j `DEBUG` level logging for the transformations code can be enabled
in ACS log4j properties on the following packages:
```properties
log4j.logger.org.alfresco.repo.rendition2=debug
log4j.logger.org.alfresco.enterprise.repo.rendition2=debug
log4j.logger.org.alfresco.repo.content.transform.TransformerDebug=debug
log4j.logger.org.alfresco.repo.content.transform.LocalTransformServiceRegistry=debug
log4j.logger.org.alfresco.enterprise.repo.rendition2.RemoteTransformServiceRegistry=debug
log4j.logger.org.alfresco.repo.content.transform.LocalTransform=debug
```
* `log4j.logger.org.alfresco.repo.rendition2` - The package associated
with the core functionality and Local Transforms.
* `log4j.logger.org.alfresco.enterprise.repo.rendition2` - The package
associated with Transform Service transforms in the Enterprise Edition of ACS.

In addition, the `Alfresco Admin Tool` provides a transformers debugging
tool called `Test Transform` under the `Support Tools` section.

**Get Transformer Names**

**Get Transformations By Extension**

**Get Transformations By Transformer**

**Get Transformation Log**

**Get Transformation Debug Log**

TODO

* Talk about the bits of the Support Tools section of the Alfresco
  Admin Tool that have not been deprecated, and how to use it to
  work out if your transforms have been created.
  This functionality does not work at the moment,
  compete the section when fixed.