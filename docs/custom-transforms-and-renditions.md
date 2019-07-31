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
of this section.

One of the advantages of Local Transforms is that there is no longer any
need for custom Java code, Spring bean definitions, or alfresco
properties to be applied to the ACS repository. When adding custom
transforms, it is not uncommon to add custom renditions or additional
mimetypes. This section also covers adding these with json rather than
Java or Spring beans.

## Repository Configuration

### Configure a T-Engine as a Local Transform

TODO talk about the localTransform.*.url System property and how to do
that when starting the repo Docker images (JAVA_OPTS).

Talk about the retry / failure cron task that looks for T-Engine
configuration.

Talk about the options role in transformer selection.

### Enabling and disabling Legacy, Local or Transform Service transforms.

TODO
* How do you disable all legacy, all local or all transform service transforms?
* How do you disable individual Local transforms (unset the localTransform.xxx.url).
* Talk about restarting pods or bouncing the repo

### Configure a pipeline of local transforms

TODO - Talk about the json file

### Configure a custom rendition

TODO - links to the work on creating renditions

### Configure a custom mimetype

TODO - links to the work on creating renditions

## Transform Service Configuration

### Configure a T-Engine in the Transform Service

TODO Raise an ATS ticket

### Configure a pipeline in the Transform Service

TODO Raise an ATS ticket, or make it part of the same ticket

## Creating a T-Engine

This chapter will describe how to develop, configure and run a custom T-Engine.
We will use an example Hello World T-Engine as a reference throughout this chapter.
The Hello World T-Engine project can be found [here].

It is assumed that the reader has some familiarity with the following technologies:
* Spring Boot
* Maven
* Docker

### Developing and Debugging T-Engines

T-Engines are Dockerized Spring Boot applications.
New T-Engines are set up as Maven projects built on top of [Alfresco Transform Core].
The Alfresco Transform Core project brings in Spring Boot capabilities
as well as base classes which allowing us to easily develop new T-Engines.
Using the provided example T-Engine, we are going to take a look at:
 * How to [set up](#project-setup) a T-Engine as a Dockerized Spring Application.
 * How to specify [T-Engine configuration](#t-engine-configuration).
 * How to implement a [T-Engine controller](#custom-transform-api).

#### Project setup

In order to configure the custom T-Engine to be built as a Spring Boot application
in a Docker image, we need to add some configuration.
The quickest way to get started is to clone the example T-Engine from [here],
this example is fully configured and ready to be built and run.
Alternatively, we can create a blank Maven project with the same folder
structure as in the example project.

The following configuration files are required:

[pom.xml]

The POM file defines Alfresco Transform Core as the parent and adds
required dependencies. It also configures plugins for building
the Spring Boot application and generating the Docker image.

[Dockerfile]

The Dockerfile is needed by the [docker-maven-plugin] configured in the pom.xml
to generate a docker image.
It defines a simple Docker image with our Spring Boot application fat jar copied in,
specifies default user information and exposes port 8090.

[Application.java]

The Application class defines an entry point for the Spring Boot application.

#### T-Engine configuration

See ACS side of the configuration [here](#configure-a-t-engine-as-a-local-transform).

Each T-Engine has to provide engine configuration which is read by
T-Engine's clients to determine supported transformations.
The configuration is specified in terms of source and target Media Types
and transformOptions.

Keywords:
* **sourceMediaType** - Media Type of the file sent to the T-Engine in
a transform request.
* **targetMediaType** - Media Type of the file returned by the T-Engine in
a transform response.
* **transformOptions** - Custom list of (key, value) pairs supplied to the T-Engine in
a transform request.

The following engine configuration is from the example T-Engine.
The T-Engine provides a simple content transform which takes
a source text file and transforms it into a target HTML file.
The target HTML file will say "Hello World!, Hello {name from the source file}!"
in the language requested via transformOptions.

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
      "transformerName": "exampleTransformer",
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
such as *exampleOptions* and a list of option names, in this case just *language*.
* **transformers** - A list of transformer definitions.
Each transformer definition has a unique **transformerName**,
list of supported **sourceMediaTypes** and **targetMediaTypes**.

The **engine_config.json** file has to be provided at the top level
of the resources folder (same as in the example) with the same name (engine_config.json).
No additional wiring is required for this file to be served by the T-Engine.

> ACS uses transformOptions in its transformer selection strategy.
For this reason it is recommended to prefix option names with a namespace to prevent clashes.

#### Custom transform API

T-Engines define their endpoints through an annotated Spring controller
provided by Alfresco Transform Core.
The [ExampleController.java] in the example T-Engine illustrates how to
implement such controller.


```java
@PostMapping(value = "/transform", consumes = MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<Resource> transform(HttpServletRequest request,
                                              @RequestParam("file") MultipartFile sourceMultipartFile,
                                              @RequestParam(value = "targetExtension") String targetExtension,
                                              @RequestParam(value = "language") String language)
```

The handler method for the `/transform` endpoint, it serves HTTP requests for transforms.
ACS will make requests to this endpoint when configured to use local transforms.

Method parameters:

* **sourceMultipartFile** - The file to be transformed from the transform request.
* **targetExtension** - The target extension of the transformed file to be returned in the response.
This is always provided by ACS requests.
* **language** - This is the custom transform option defined for the example T-Engine.

The transform method's signature will vary depending on the [engine configuration](#t-engine-configuration)
defined in this T-Engine. The example T-Engine is configured to take
a single `language` option, but number of the method's parameters will
grow if more transformOptions are added.

```java
public void processTransform(File sourceFile, File targetFile, Map<String, String> transformOptions, Long timeout)
```

This method is called by requests which come in through a message queue used by the Transform Service.
It performs the same transform as the `/transform` endpoint.

```java
public ProbeTestTransform getProbeTestTransform()
```

This method provides a way to define a test transform for [T-Engine Probes](https://github.com/Alfresco/alfresco-transform-core/blob/master/docs/Probes.md).
For example a test transform on a file included in the same Docker image.

#### Running and Debugging

##### Hello World T-Engine

This chapter will describe how to run and debug the example [Hello World T-Engine].
The steps for building and running the T-Engine are described in the project's
[README.md].

The Hello World T-Engine transforms a text file into a html file
and takes a language option. See [engine configuration].
To test the transform:

1. Build the T-Engine
```
mvn clean install -Plocal
```
2. Start the T-Engine
```
docker run -d -p 8099:8090 --name alfresco-helloworld-transformer alfresco/alfresco-helloworld-transformer:latest
```
3. Create a **sourceFile.txt** file with content:
```text
T-Engines
```

4. Send a HTTP POST request to the /transform. The Hello World T-Engine provides
a convenience [HTML form] to do this.
Once the T-Engine is running the form can be accessed at http://localhost:8099/

5. In the HTML Form, choose the **sourceFile.txt** file.
    Specify a language, supported languages are: English, Spanish, German.
6. Click transform

##### Hello World T-Engine with ACS

* Refer to other chapters for the setup:
localTransform.xxx.url, add a custom rendition.
* Upload file via Share or REST API, get the node id
* REST API request for available renditions, note the custom rendition
* REST API request to create the custom rendition
* REST API request to retrieve the rendition





TODO
* Talk about using the test /transform and /log endpoints
* Identify the repo log4j settings to set.
* Talk about how to access log files in the T-Engine
* Talk about the bits of the Support Tools section of the Alfresco
  Admin Tool that have not been deprecated, and how to use it to
  work out if your transforms have been created. 


### Migrating a Legacy Transformer into a T-Engine

TODO
* Identify the bits that should be copied to the T-Engine: Java code
  (normally from the transformInternal method) that does the actual
   transform or 3rd party libraries or calls to 3rd party services.
   Talk about not needing to copy other code such as the
    isTransformableMimetype method as this is defined in a json file in
     the T-Engine. 
* Talk about not needing to create a sub class of TransformOptions, or
  the need to marshal and un marshal TransformOptions.
* Talk about there being no need for an AMP and that this decouples
  releases.
* Talk about there being no need for Spring beans.
* Talk about being able to run legacy transformers, local transforms and
  and the Transform service at the same time.
* Talk about creating pipelines in json rather than Spring or via
   values in transformer.properties.