# Custom Transforms and Renditions

Alfresco Content Services (ACS) provides a number of content transforms,
but also allows custom transforms to be added. This section describes
how to create custom transforms.

From ACS 6.2 it is possible to create custom transforms that run in
separate processes known as T-Engines (short for Transformer Engines).
The same engines may be used
in Community and Enterprise Editions. They may be directly
connected to the ACS repository as Local Transforms, but in the
Enterprise edition there is the option to include them as part of the
Transform Service which provides more balanced throughput and better
administration capabilities. A T-Engine is intended to be run as a 
Docker image, but may also be run as a standalone process.

Prior to ACS 6.0 Legacy transformers ran within the same JVM as
the ACS repository. They and their supporting code has been deprecated
and will go away at some point. ACS 6.2 still uses them if a rendition
cannot be created by the Transform Service or Local Transforms. The
process of migrating custom legacy transformers is described at the end
of this document.

One of the advantages of Local Transforms is that there is no longer any
need for custom Java code, Spring bean definitions, or alfresco
properties to be applied to the ACS repository. When adding custom
transforms, it is not uncommon to add custom renditions or additional
mimetypes, so this is also covered on this page. Generally custom
transforms and renditions can be added to Docker deployments without
having to create or apply an AMP.

## Repository Configuration

### Configure a T-Engine as a Local Transform

For the ACS repository to talk to a T-Engine, it must know the engine's 
URL. The URL can be added as an Alfresco global property,
or more simply as a Java system property. JAVA_OPTS may be used to set
this if starting the repository with Docker.
```properties
localTransform.<engineName>.url=
```

The &lt;engineName> is a unique name of the T-Engine. 
For example, `localTransform.helloworld.url`. Typically a T-Engine
contains a single transform or an associated group of transforms.
Having set the URL to a T-Engine, the ACS repository will update its
configuration by requesting the
[T-Engine's configuration](#t-engine-configuration) on a periodic basis.
It is requested more frequent on start up or if a communication or configuration
problem has occurred, and a less frequent during normal operation.

```properties
local.transform.service.cronExpression=0 0 0/1 * * ?
local.transform.service.initialAndOnError.cronExpression=0/10 * * * * ?
```

#### Transformer selection strategy
The ACS repository will use the
[T-Engine configuration](#t-engine-configuration) to
choose which T-Engine will perform a transform. A transformer definition
contains a supported list of source and target Media Types. This is used
for the most basic selection. This is further refined by checking
that the definition also supports transform options (parameters) that
have been supplied in a transform or rendition request.
See [rendition options](#configure-a-custom-rendition).
```text
Transformer 1 defines options: Op1, Op2
Transformer 2 defines options: Op1, Op2, Op3, Op4
```
```
Rendition provides values for options: Op2, Op3
```
If we assume both transformers support the required source and target
Media Types, Transformer 2 will be selected because it knows about all the supplied
options. The definition may also specify that some options are required
or grouped.

### Enabling and disabling Legacy, Local or Transform Service transforms

Legacy, Local or Transform Service transforms can be enabled or disabled
independently of each other. The ACS repository will try to transform
content using the Transform Service if possible, falling back to a Local
Transform and failing that a Legacy Transform. This makes it possible to
gradually migrate away from Legacy Transforms and to take advantage of
the Transform Service if it is available. 

```properties
transform.service.enabled=true
local.transform.service.enabled=true
legacy.transform.service.enabled=true
```

Setting the enabled state to **false** will disable all of the transforms
performed by that particular service. It is possible to disable individual
Local Transforms by setting the corresponding T-Engine URL property
[localTransform.&lt;engineName>.url](#configure-a-t-engine-as-a-local-transform)
value to an empty string.

```properties
localTransform.helloworld.url=
```

### Configure a pipeline of Local Transforms

Local transforms may be combined together in a pipeline to form a new
transformer. A pipeline definition (JSON) is used to define the sequence of
transforms and intermediate Media Types. Like any other transformer, it
specifies a list of supported source and target Media Types. The
definition may reuse the transformOptions of transformers in the
pipeline, but typically will define its own subset of these.  

The following example begins with the **helloWorldTransformer**
described in [Creating a T-Engine](#creating-a-t-Engine), which takes a
text file containing a name and produces an HTML file with a Hello
&lt;name> message in the body. This is then transformed back into a
text file.
```json
{
  "transformers": [
    {
      "transformerName": "helloWorldPipeline",
      "transformerPipeline" : [
        {"transformerName": "helloWorldTransformer", "targetMediaType": "text/html"},
        {"transformerName": "html"}
      ],
      "supportedSourceAndTargetList": [
        {"sourceMediaType": "text/plain",  "targetMediaType": "text/plain" }
      ],
      "transformOptions": [
        "exampleOptions",
        "htmlOptions"
      ]
    }
  ]
}
```

* **transformerName** - Try to create a unique name for the transformer.
* **transformerPipeline** - A list of transformers in the pipeline.
The **targetMediaType** specifies the intermediate Media Types between
transformers. There is no final targetMediaType as this comes from the
supportedSourceAndTargetList.
* **supportedSourceAndTargetList** - The supported source and target
Media Types, which refer to the Media Types this pipeline transformer
can transform from and to.
* **transformOptions** - A list of references to options required by
the pipeline transformer.

Pipeline definitions need to be placed in a directory of an ACS
repository. The default location (below) may be changed by resetting the
following Alfresco global property.
```properties
local.transform.pipeline.config.dir=shared/classes/alfresco/extension/transform/pipelines
```
On startup this location is checked every 10 seconds, but then switches
to once an hour if successfully. After a problem, it tries every 10
seconds again.
```properties
local.transform.service.cronExpression=4 30 0/1 * * ?
local.transform.service.initialAndOnError.cronExpression=0/10 * * * * ?
```

###### Docker Compose

If you are using Docker Compose in development, you will need to copy
your pipeline definition into your running AWC repository container.
One way is to use the following command and it will be picked up the
next time the location is read, which is dependent on the cron values.

```bash
docker cp custom_pipelines.json <alfresco container>:/usr/local/tomcat/shared/classes/alfresco/extension/transform/pipelines/
```

###### Kubernetes and ConfigMaps

TODO Complete this section

### Configure a custom rendition

Renditions definitions prior to ACS 6.2 were defined as Spring Beans.
A rendition as a Spring bean might look like this:
```xml
<bean id="renditionDefinitionHelloWorld" class="org.alfresco.repo.rendition2.RenditionDefinition2Impl">
    <constructor-arg name="renditionName" value="helloWorld"/>
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
The new JSON equivalent of the above **helloWorld** rendition looks like this:
```json
{
    "renditionName": "helloWorld",
    "targetMediaType": "text/html",
    "options": [
        {"name": "language", "value": "German"}
    ]
}
```
* **renditionName** - A unique rendition name.
* **targetMediaType** - The target Media Type for the rendition.
* **options** - The list of static transform options corresponding to the
transform options defined in [T-Engine configuration](#t-engine-configuration).

Location of the renditions JSON file can be specified using
a System property, the default value is:
```properties
rendition.config.dir=shared/classes/alfresco/extension/transform/renditions/
```
The location is checked on a periodic basis specified by the following
Cron expression properties:
```properties
rendition.config.cronExpression=2 30 0/1 * * ?
rendition.config.initialAndOnError.cronExpression=0/10 * * * * ?
```

#### Adding a custom rendition in Docker Compose

After starting ACS in Docker Compose:
1. Create a JSON file **custom_renditions.json** with the above rendition
definition and copy it into the Alfresco Docker container.
    ```bash
    docker cp custom_renditions.json <alfresco container>:/usr/local/tomcat/shared/classes/alfresco/extension/transform/renditions/
    ```
2. Restart the Alfresco service.
    ```bash
    docker-compose restart alfresco
    ```

#### Adding a custom rendition in Kubernetes

TODO Complete this section

### Configure a custom mimetype

Custom MIME types can be defined using the JSON format.
A custom JSON file with MIME type definitions can be added by specifying
a System property.

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

Location of the Media Type JSON file can be specified using a System
property, the default value is:
```properties
mimetype.config.dir=shared/classes/alfresco/extension/transform/mimetypes
```
The location is checked on a periodic basis specified by the following
Cron expression properties:
```properties
mimetype.config.cronExpression=0 30 0/1 * * ?
mimetype.config.initialAndOnError.cronExpression=0/10 * * * * ?
```

TODO
* Describe the mimetype definition

## Transform Service Configuration

### Configure a T-Engine in the Transform Service

TODO Raise an ATS ticket

### Configure a pipeline in the Transform Service

TODO Raise an ATS ticket, or make it part of the same ticket

## Creating a T-Engine

This section will describe how to develop, configure and run a custom
T-Engine. We will use an example Hello World T-Engine as a reference
throughout this section. The Hello World T-Engine project can be found
[here](https://github.com/Alfresco/alfresco-helloworld-transformer).

It is assumed that the reader has some familiarity with the following
technologies:
* Spring Boot
* Maven
* Docker

### Developing and Debugging T-Engines

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

#### Project setup

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

#### Custom transform API

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

#### Running and Debugging

##### Hello World T-Engine

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
    service JAVA_OPTS. See [here](#configure-a-t-engine-as-a-local-transform)
    for details. For Docker Compose, the **transform-helloworld**
    in the URL http://**transform-helloworld**:8090/ has the match
    the service name defined above.
    ```
    -DlocalTransform.helloworld.url=http://transform-helloworld:8090/
    ```

5. Create a custom **helloWorld** rendition which will use the new
Hello World T-Engine. See [here](#configure-a-custom-rendition) for details.
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

###### Test custom rendition

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

###### Logs and Debugging

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

### Migrating a Legacy Transformer into a T-Engine

TODO
* Talk about creating pipelines in json rather than Spring or via
   values in transformer.properties.

This section will describe how to migrate custom synchronous transformers
created for Alfresco Content Repository (ACS) prior to version 6.2, to new
asynchronous out of process T-Engines.
The pre 6.2 transformers will be referred to as *Legacy Transformers*.
It is assumed that the reader is familiar with creating and configuring
a new T-Engine as described in  [Creating a T-Engine](#creating-a-t-engine).

The new asynchronous approach of using T-Engines provides the means to
decouple ACS and Legacy Transformers. This allows for decoupled releases
as well as greater separation between the ACS codebase and custom
transformer code. This also means that there is no longer the need to use
AMPs in order to provide Spring context files or to override beans in ACS
to introduce new transformers, renditions or pipelines.
New transformers will be added to ACS by creating and configuring new T-Engines.

A custom Legacy Transformer would typically be packaged as an Alfresco
Module Package (AMP). The AMP would contain Java classes, Spring context
files with new transformer, rendition or pipeline Spring Beans and any
additional custom configuration required by the new transformer.
All of this functionality can now be added without the need to override
the ACS Spring Bean configuration.

The steps to create and migrate a Legacy Transformer into a custom
T-Engine are as follows:

1. Create a custom T-Engine. The [Creating a T-Engine](#creating-a-t-engine) section walks through how to
develop, configure and run a new t-Engine using a simple Hello World
example.
2. Migrate the custom Legacy Transformer Java code into the new T-Engine
as described in [Migrating custom transform code](#migrating-custom-transform-code).
3. Migrate any custom renditions defined as Spring Beans.
See how to add custom renditions in [Configure a custom rendition](#configure-a-custom-rendition)
4. Migrate any custom pipelines defined as Spring Beans.
See how to add a custom pipelines in [Configure a pipeline of Local Transforms](#configure-a-pipeline-of-local-transforms).
5. Configure ACS to use the new custom T-Engine as described in [Configure a T-Engine as a Local Transform](#configure-a-t-engine-as-a-local-transform).

#### Migrating custom transform code
Legacy Transformers are implemented by extending a now deprecated class
`org.alfresco.repo.content.transform.AbstractContentTransformer2`.
This implementation requires the Legacy Transformer to define functionality
by implementing the following abstract methods:
* **isTransformableMimetype**
* **transformInternal**

##### Migrating isTransformableMimetype
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

##### Migrating transformInternal
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
in [HelloWorldController.java](https://github.com/Alfresco/alfresco-helloworld-transformer/blob/master/src/main/java/org/alfresco/transformer/HelloWorldController.java).

The [Custom transform API](#custom-transform-api) section describes how
to add transform logic to a custom T-Engine. In short, the logic in
the `transformInternal` method in a Legacy Transformer can be copied
into a new T-Engine and modified to use the parameters provided by
the `/transform` endpoint.
An example of this can be seen in the **HelloWorldController.java**.

* Requests to a T-Engine's `/transform` endpoint contain a multipart file.
This is equivalent to the **ContentReader** parameter.
* The response from the `/transform` endpoint contains the transformed file.
This is equivalent to the **ContentWriter** parameter.
* Requests to a T-Engine's `/transform` endpoint contain a list of
transform options as defined by the [engine configuration](#t-engine-configuration).
These are equivalent to the options in the **TransformationOptions** parameter.



TODO
* Talk about not needing to create a sub class of TransformOptions, or
  the need to marshal and un marshal TransformOptions.
