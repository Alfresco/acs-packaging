# Custom Transforms and Renditions

Alfresco Content Services (ACS) provides a number of content transforms,
but also allows custom transforms to be added. This section describes
how to create custom transforms.

From ACS 6.2 it is possible to create custom transforms that run in
separate processes known as T-Engines. The same engines may be used
in Community and Enterprise Editions. They may be directly
connected to the ACS repository as Local Transforms, but in the
Enterprise edition there is the option to include them as part of the
Transform Service which provides more balanced throughput and better
administration capabilities.

Prior to ACS 6.0 all legacy transformers ran within the same JVM as
the ACS repository. They and their supporting code has been deprecated
and will go away at some point. ACS 6.2 still uses them if a rendition
cannot be created by the Transform Service or Local Transforms. The
process of migrating custom legacy transformers is described at the end
of this document.

One of the advantages of Local Transforms is that there is no longer any
need for custom Java code, Spring bean definitions, or alfresco
properties to be applied to the ACS repository. When adding custom
transforms, it is not uncommon to add custom renditions or additional
mimetypes. This section also covers adding these with json rather than
Java or Spring beans.

## Repository Configuration

### Configure a T-Engine as a Local Transform

The configuration required for ACS to connect and talk to a T-Engine
as a Local Transform is a single URL of the T-Engine.
The URL can be added as an System property either by
specifying it in a property file or via JAVA_OPTS in Docker.
```properties
localTransform.{transformer}.url=
```

The {transformer} in the above example is a unique name of the transformer.
For example, `localTransform.helloworld.url=`.
Having set the URL to a T-Engine, ACS will update its transformer
configuration by requesting the
[T-Engine's configuration](#t-engine-configuration) on a periodic basis.
The frequency of these requests has 2 phases.
A more frequent phase used when ACS starts up and when an error occurs,
and a less frequent phase used during normal operation.

```properties
local.transform.service.cronExpression=0 0/10 * * * ?
local.transform.service.initialAndOnError.cronExpression=0/10 * * * * ?
```

#### T-Engine selection strategy
[T-Engine's configuration](#t-engine-configuration) returned by each
registered T-Engine provides the basis for selection by ACS.
ACS will use the configurations to choose the appropriate T-Engine based
on its transformer definitions.
A transformer definition contains supported source and target Media Types,
so ACS will choose the T-Engine which supports the required pair.
Furthermore, a transformer definition might include transform options.
If the required transform options are specified then the [rendition options](#configure-a-custom-rendition)
will have to be satisfied by the transformer options for the T-Engine
to be picked.

```text
For example:
Transformer T1 defines some options: Op1, Op2
Transformer T2 defines some options: Op1, Op2, Op3
Rendition   R1 defines some options: Op1, Op2
Given that T1 and T2 accept the same Media Types, T1 will be chosen for
the transform.
```

### Enabling and disabling Legacy, Local or Transform Service transforms.

Legacy, Local or Transform Service transforms can be enabled or disabled
independently of each other and all 3 can be enabled at the same time.
The following System properties control each one:


```properties
# Legacy transforms
legacy.transform.service.enabled=true

# Local transforms
local.transform.service.enabled=true

# Transform Service
transform.service.enabled=true
```

Setting the enabled state to **false** will disable all of the transforms
performed by that particular service. It is possible to disable individual
Local transforms by setting their corresponding property
[localTransform.{transformer}.url=](#configure-a-t-engine-as-a-local-transform)
value to an empty string.

TODO
* Talk about restarting pods or bouncing the repo

### Configure a pipeline of local transforms


TODO - Talk about the json file
* Talk about creating pipelines in json rather than Spring or via
   values in transformer.properties.

### Configure a custom rendition

Renditions definitions prior to ACS 6.2 were defined as Spring beans.
A rendition as a Spring bean might look like this:
```xml
<bean id="renditionDefinitionHelloWorldRendition" class="org.alfresco.repo.rendition2.RenditionDefinition2Impl">
    <constructor-arg name="renditionName" value="helloWorldRendition"/>
	<constructor-arg name="targetMimetype" value="text/html"/>
	<constructor-arg name="transformOptions">
	<map>
	    <entry key="language" value="German" />
	</map>
	</constructor-arg>
	<constructor-arg name="registry" ref="renditionDefinitionRegistry2"/>
</bean>
```

Starting from ACS 6.2, renditions can be added via a JSON file.
The new JSON equivalent of the above **helloWorldRendition** looks like this:
```json
{
    "renditionName": "helloWorldRendition",
    "targetMediaType": "text/html",
    "options": [
        {"name": "language", "value": "German"}
    ]
}
```

The default location for the rendition definitions is:
```
alfresco-repository/src/main/resources/alfresco/renditions/
```

Optionally, an additional location can be specified using the System property:
```properties
rendition.config.dir=
```

### Configure a custom mimetype

Custom MIME types can be defined using the JSON format.
A custom JSON file with MIME type definitions can be placed in

TODO
* How to add this file?

Example MIME type definition for Microsoft Word document in JSON:
```json
{
    "name": "Microsoft Word",
    "extension": "doc",
    "mediaType": "application/msword",
    "inputFamily": "TEXT",
    "storePropertiesByFamily": {"TEXT": {"FilterName": "MS Word 97"}}
  }
```

## Transform Service Configuration

### Configure a T-Engine in the Transform Service

TODO Raise an ATS ticket

### Configure a pipeline in the Transform Service

TODO Raise an ATS ticket, or make it part of the same ticket

## Creating a T-Engine

This section will describe how to develop, configure and run a custom
T-Engine. We will use an example Hello World T-Engine as a reference
throughout this section. The Hello World T-Engine project can be found
[here].

It is assumed that the reader has some familiarity with the following
technologies:
* Spring Boot
* Maven
* Docker

### Developing and Debugging T-Engines

T-Engines are Dockerized Spring Boot applications.
New T-Engines are set up as Maven projects built on top of
[Alfresco Transform Core].
The Alfresco Transform Core project brings in Spring Boot capabilities
as well as base classes which allow us to easily develop new T-Engines.
Using the provided example T-Engine, we are going to take a look at:
 * How to [set up](#project-setup) a T-Engine as a Dockerized Spring
 application.
 * How to specify [T-Engine configuration](#t-engine-configuration).
 * How to implement a [T-Engine controller](#custom-transform-api).

#### Project setup

In order to configure a custom T-Engine to be built as a Spring Boot
application
in a Docker image, we need to add some configuration.
The quickest way to get started is to clone the example T-Engine from
[here],
this example is fully configured and ready to be built and run.
Alternatively, we can create a blank Maven project with the same folder
structure as in the example project.

The following configuration files are required:

[pom.xml]

The POM file defines Alfresco Transform Core as the parent and adds
required dependencies. It also configures plugins for building
the Spring Boot application and generating the Docker image.

[Dockerfile]

The Dockerfile is needed by the [docker-maven-plugin] configured in
the pom.xml to generate a docker image.
It defines a simple Docker image with our Spring Boot application fat
jar copied in, specifies default user information and exposes port 8090.

[Application.java]

The Application class defines an entry point for the Spring Boot
application.

#### T-Engine configuration

See ACS side of the configuration
[here](#configure-a-t-engine-as-a-local-transform).

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

[engine_config.json]
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
* **transformers** - A list of transformer definitions.
Each transformer definition has a unique **transformerName**,
list of supported **sourceMediaTypes** and **targetMediaTypes**
and a reference to **transformOptions**.

The **engine_config.json** file has to be provided at the top level
of the `resources` folder (same as in the example) with the same name
(engine_config.json).
No additional wiring is required for this file to be served by the
T-Engine.

> ACS uses transformOptions in its transformer selection strategy.
For this reason it is recommended to prefix option names with
a namespace to prevent clashes.

#### Custom transform API

T-Engines define their endpoints through an annotated Spring controller
provided by Alfresco Transform Core.
The [HelloWorldController.java] in the example T-Engine illustrates how
to implement such controller.


```java
@PostMapping(value = "/transform", consumes = MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<Resource> transform(HttpServletRequest request,
                                              @RequestParam("file") MultipartFile sourceMultipartFile,
                                              @RequestParam(value = "targetExtension") String targetExtension,
                                              @RequestParam(value = "language") String language)
```

The handler method for the `/transform` endpoint, it serves HTTP
requests for transforms. ACS will make requests to this endpoint when
configured to use local transforms.

Method parameters:

* **sourceMultipartFile** - The file to be transformed from the
transform request.
* **targetExtension** - The target extension of the transformed file
to be returned in the response.
This is always provided by ACS requests.
* **language** - This is the custom transform option defined for
the example T-Engine.

The `transform` method's signature will vary depending on
the [engine configuration](#t-engine-configuration).
The example T-Engine is configured to take a single `language`
transform option, but the number of the `transform` method's
parameters will have to match the transform options defined in [engine_config.json].

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

#### Running and Debugging

##### Hello World T-Engine

This section will describe how to run and debug the example [Hello World T-Engine].
Instructions on how to build and run the T-Engine are described in
the project's [README.md] and also specified below.
The Hello World T-Engine transform takes an input text file
and a `language` **transformOption** and returns a html file.
See [engine configuration] for details.

1. Clone the [Hello World T-Engine] project.
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
provides a convenience [HTML form] to do this.
Once the T-Engine is running, the form can be accessed at: http://localhost:8090/

5. In the HTML Form, choose the **source_file.txt**.
Specify a language, supported languages are: English, Spanish, German.
6. Click **Transform**
7. Verify that the returned HTML contains a Hello World greeting in the
specified language.

###### Logs

T-Engines provide a `/log` endpoint out of the box which shows
information about transformations performed by the T-Engine.
In addition, the T-Engine server logs can be accessed using
the Docker `logs` command.
For more information see [Docker documentation](https://docs.docker.com/engine/reference/commandline/logs/).
```bash
docker logs alfresco-helloworld-transformer
```

##### Hello World T-Engine with ACS

This section will describe how to configure a development environment
and run Alfresco Content Services with the new Hello World T-Engine.

> The example in this section uses Docker Compose for simplicity;
however, it is not recommended to run ACS in Docker Compose in production.

1. Clone the [Hello World T-Engine] project (if not done already).
2. Build the Hello World T-Engine, (if not done already).
Check that the local Docker image repository contains
**alfresco/alfresco-helloworld-transformer:latest**
3. Clone the [ACS](https://github.com/Alfresco/acs-deployment) project.
4. Modify the ACS [docker-compose] file by adding the Hello World T-Engine
as one of the services.
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
    service JAVA_OPTS. See [here](#configure-a-t-engine-as-a-local-transform)
    for details. For Docker Compose, the **transform-helloworld**
    in the URL http://**transform-helloworld**:8090/ has the match
    the service name defined above.
    ```
    -DlocalTransform.helloworld.url=http://transform-helloworld:8090/
    ```

5. Create a custom **helloWorldRendition** which will use the new
Hello World T-Engine. See [here](#configure-a-custom-rendition) for details.
    ```json
    {
        "renditionName": "helloWorldRendition",
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

###### Test custom rendition

This section walks through an end to end example of using
the Hello World T-Engine with ACS by requesting the **helloWorldRendition**.

1. Create a **source_file.txt** file with the following content:
   ```text
   T-Engines
   ```
2. Upload the file using [REST API](https://api-explorer.alfresco.com/api-explorer/#!/nodes/createNode)
and write down the **id** in the response, this is the **nodeId**
used in following requests.
    ```bash
    curl -u admin:admin -X POST localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/nodes/-my-/children -F filedata=@sourceFile.txt
    ```
3. Request a list of available renditions using [REST API](https://api-explorer.alfresco.com/api-explorer/#!/renditions/listRenditions)
on the uploaded file.
Notice that the custom helloWorldRendition is in the list of available
renditions.
    ```bash
    curl -u admin:admin -X GET localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/nodes/{nodeId}/renditions
    ```
4. Request the **helloWorldRendition** to be created using [REST API](https://api-explorer.alfresco.com/api-explorer/#!/renditions/createRendition).
    ```bash
    curl -u admin:admin -X POST localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/nodes/{nodeId}/renditions -d '{"id":"helloWorldRendition"}' -H "Content-Type: application/json"
    ```
5. Request the rendered file using [REST API](https://api-explorer.alfresco.com/api-explorer/#!/renditions/getRenditionContent).
    ```bash
    curl -u admin:admin -X GET localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/nodes/{nodeId}/renditions/helloWorldRendition/content -o hello_world_rendition.html
    ```
6. Verify that the returned HTML file contains a Hello World greeting
in the language specified in the **helloWorldRendition**.

###### Logs and Debugging

Log4j `DEBUG` level logging for the transformations code can be enabled
in ACS on the following packages:

* `log4j.logger.org.alfresco.repo.rendition2` - The package associated
with the core functionality and local transforms.

* `log4j.logger.org.alfresco.enterprise.repo.rendition2` - The package
associated with Transform Service transforms in the Enterprise Edition of ACS.

In addtion, the `Alfresco Admin Tool` provides a transformers debugging
tool called `Test Transform` under the `Support Tools` section.

**Get Transformer Names**

**Get Transformations By Extension**

**Get Transformations By Transformer**

**Get Transformation Log**

**Get Transformation Debug Log**

TODO
* Identify the repo log4j settings to set. **Any other?**
* Talk about the bits of the Support Tools section of the Alfresco
  Admin Tool that have not been deprecated, and how to use it to
  work out if your transforms have been created.  **Doesn't seem to show the new transformer?**

### Migrating a Legacy Transformer into a T-Engine

> It is assumed that the reader is familiar with creating and configuring
a new T-Engine as described [here](#developing-and-debugging-t-engines).

This section will describe how to migrate custom synchronous transformers
created for Alfresco Content Repository (ACS) prior to version 6.2, to new
asynchronous out of process T-Engines.
The pre 6.2 transformers will be referred to as *Legacy Transformers*.

A custom Legacy Transformer would typically be packaged as an Alfresco
Module Package (AMP). The AMP would contain Java classes, Spring context
files with new transformer beans and any additional configuration
required by the new transformer.
The new out of process approach using T-Engines provides means to
decouple ACS and transformers. This allows for decoupled releases
as well as greater separation between the ACS codebase and custom
transformer code. This also means that there is no longer the need to use
AMPs in order to provide Spring context files or to override beans in ACS
in order to introduce new transformers. New transformers will be
added to ACS by creating and configuring new T-Engines.

Legacy Transformers are implemented by extending a now deprecated class
`org.alfresco.repo.content.transform.AbstractContentTransformer2`.
This implementation requires the Legacy Transformer to define functionality
by implementing 2 abstract methods `isTransformableMimetype` and `transformInternal`.

#### Migrating isTransformableMimetype
```java
public boolean isTransformableMimetype(String sourceMimetype, String targetMimetype, TransformationOptions options)
```
The `isTransformableMimetype` method allows ACS to determine whether
this transformer is applicable for a given transform request.
When migrating a Legacy Transformer to a T-Engine, this method is no longer
needed.

**How to migrate:**

This functionality is now defined via a JSON file served by T-Engines.
See how to define the configuration in the
[engine configuration](#t-engine-configuration) section.

#### Migrating transformInternal
```java
public void transformInternal(ContentReader reader, ContentWriter writer,  TransformationOptions options) throws Exception
```
The `transformInternal` method performs the actual transform, either directly
or via a 3rd party library or service. The **ContentReader** parameter
provides a way of accessing the content to be transformed and
the **ContentWriter** parameter provides a way to write the result.
A **TransformationOptions** parameter provides the transform options.

**How to migrate:**

>Notice how the signature of the Legacy Transformer's `transformInternal`
method is similar to the Hello World T-Engine's `transformInternal` method
in [HelloWorldController].

The [Custom transform API] section describes how to add transform logic
to a custom T-Engine. In short, the logic in the `transformInternal`
method in a Legacy Transformer can be copied into a new T-Engine and
modified to use the parameters provided by the `/transform` endpoint.
An example of this can be seen in the [HelloWorldController.java].

* Requests to a T-Engine's `/transform` endpoint contain a multipart file.
This is comparable to the **ContentReader** parameter.
* The response from the `/transform` endpoint contains the transformed file.
This is comparable to the **ContentWriter** parameter.
* Requests to a T-Engine's `/transform` endpoint contain a list of
transform options as defined by the [engine configuration](#t-engine-configuration).
These are comparable to the options in the **TransformationOptions** parameter.





TODO

* Talk about not needing to create a sub class of TransformOptions, or
  the need to marshal and un marshal TransformOptions.
