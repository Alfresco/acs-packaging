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

One of the advantages of Custom Transforms and Renditions in ACS 6.2 is
that there is no longer any need for custom Java code, Spring bean
definitions, or alfresco properties to be applied to the ACS repository.
Generally custom transforms and renditions can now be added to Docker
deployments without having to create or apply an AMP/JAR, or even restarting
the repository.

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
[T-Engine configuration](creating-a-t-engine.md#t-engine-configuration)
on a periodic basis. It is requested more frequent on start up or if a
communication or configuration problem has occurred, and a less
frequently otherwise.

```properties
local.transform.service.cronExpression=4 30 0/1 * * ?
local.transform.service.initialAndOnError.cronExpression=0/10 * * * * ?
```

#### Transformer selection strategy
The ACS repository will use the
[T-Engine configuration](creating-a-t-engine.md#t-engine-configuration) to
choose which T-Engine will perform a transform. A transformer definition
contains a supported list of source and target Media Types. This is used
for the most basic selection. This is further refined by checking
that the definition also supports transform options (parameters) that
have been supplied in a transform request or a Rendition Definition used
in a rendition request. See [Configure a Custom Rendition](#configure-a-custom-rendition).
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
**localTransform.&lt;engineName>.url** value to an empty string.

```properties
localTransform.helloworld.url=
```

### Configure a custom transform pipeline

Local Transforms may be combined together in a pipeline to form a new
transformer. A pipeline definition (JSON) defines the sequence of
transforms and intermediate Media Types. Like any other transformer, it
specifies a list of supported source and target Media Types. The
definition may reuse the transformOptions of transformers in the
pipeline, but typically will define its own subset of these.  

The following example begins with the **helloWorld** Transformer
described in [Creating a T-Engine](#creating-a-t-engine.md), which takes a
text file containing a name and produces an HTML file with a Hello
&lt;name> message in the body. This is then transformed back into a
text file. This example contains just one pipeline transformer, but
many may be defined in the same file.
```json
{
  "transformers": [
    {
      "transformerName": "helloWorldText",
      "transformerPipeline" : [
        {"transformerName": "helloWorld", "targetMediaType": "text/html"},
        {"transformerName": "html"}
      ],
      "supportedSourceAndTargetList": [
        {"sourceMediaType": "text/plain",  "targetMediaType": "text/plain" }
      ],
      "transformOptions": [
        "helloWorldOptions",
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

Custom Pipeline definitions need to be placed in a directory of the ACS
repository. The default location (below) may be changed by resetting the
following Alfresco global property.
```properties
local.transform.pipeline.config.dir=shared/classes/alfresco/extension/transform/pipelines
```
On startup this location is checked every 10 seconds, but then switches
to once an hour if successfully. After a problem, it tries every 10
seconds again. These are the same properties use to decide when to read
T-Engine configurations, because pipelines combine transformers in the
T-Engines.

```properties
local.transform.service.cronExpression=4 30 0/1 * * ?
local.transform.service.initialAndOnError.cronExpression=0/10 * * * * ?
```

If you are using Docker Compose in development, you will need to copy
your pipeline definition into your running ACS repository container.
One way is to use the following command and it will be picked up the
next time the location is read, which is dependent on the cron values.

```bash
docker cp custom_pipelines.json <alfresco container>:/usr/local/tomcat/shared/classes/alfresco/extension/transform/pipelines/
```

In a Kubernetes environment, [ConfigMaps](https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/)
can be used to add pipeline definitions. You will need to create
a ConfigMap from the JSON file and mount the ConfigMap through a volume
to the ACS repository pods.

```bash
kubectl create configmap custom-pipeline-config --from-file=custom_pipelines.json
```

In the ACS repository pod configuration:
```yaml
volumes:
    - name: custom-pipeline-config-volume
      configMap:
          name: custom-pipeline-config
```

```yaml
volumeMounts:
    - name: custom-pipeline-config-volume
      mountPath: /usr/local/tomcat/shared/classes/alfresco/extension/transform/pipelines/
```
> From Kubernetes documentation: Caution: If there are some files
in the mountPath location, they will be deleted.

### Configure a custom rendition

Renditions are a representation of source content in another form. A
Rendition Definition (JSON) defines the transform option (parameter)
values that will be passed to a transformer and the target Media Type.

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
* **renditionName** - A unique rendition name.
* **targetMediaType** - The target Media Type for the rendition.
* **options** - The list of transform option names and values
corresponding to the transform options defined in
[T-Engine configuration](creating-a-t-engine.md#t-engine-configuration).
If you specify **sourceEncoding** or **sourceNodeRef** without a value,
the system will automatically add the values at run time. 

Just like Pipeline Definitions, custom Rendition Definitions need to be placed
in a directory of the ACS repository. There are similar properties that
control where and when these definitions are read and the same approach
may be taken to get them into Docker Compose and Kubernetes environments.

```properties
rendition.config.dir=shared/classes/alfresco/extension/transform/renditions/
```
```properties
rendition.config.cronExpression=2 30 0/1 * * ?
rendition.config.initialAndOnError.cronExpression=0/10 * * * * ?
```

### Configure a custom MIME type

Quite often the reason a custom transform is created is to convert to or
from a MIME type (or Media type) that is not known to ACS by default.
Another reason is to introduce an application specific MIME type that
indicates a specific use of a more general format such as XML or JSON. 
From ACS 6.2, it is possible add custom MIME types in a similar way to
custom Pipelines and Renditions. The JSON format and properties are as
follows:
```json
{
  "mediaTypes": [
    {
      "name": "MPEG4 Audio",
      "mediaType": "audio/mp4",
      "extensions": [
        {"extension": "m4a"}
      ]
    },
    {
      "name": "Plain Text",
      "mediaType": "text/plain",
      "text": true,
      "extensions": [
        {"extension": "txt", "default": true},
        {"extension": "sql", "name": "SQL"},
        {"extension": "properties", "name": "Java Properties"},
        {"extension": "log", "name": "Log File"}
      ]
    }
  ]
}
```
* **name** Display name of the mimetype or file extension. Optional for extensions.
* **mediaType** used to identify the content.
* **text** optional value indicating if the mimetype is text based.
* **extensions** a list of possible extensions.
* **extension** the file extension.
* **default** indicates the extension is the default one if there is more than one.

```properties
mimetype.config.dir=shared/classes/alfresco/extension/mimetypes
```
```properties
mimetype.config.cronExpression=0 30 0/1 * * ?
mimetype.config.initialAndOnError.cronExpression=0/10 * * * * ?
```

### Configure the repository to use the Transform Service

By default the Transform service is disabled by default, but Docker
Compose and Kubernetes Helm Charts may enable it again by setting
**transform.service.enabled**. The Transform Service handles
communication with all its own T-Engines and builds up its own combined
configuration JSON which is requested by the ACS repository
periodically.

```properties
transform.service.cronExpression=4 30 0/1 * * ?
transform.service.initialAndOnError.cronExpression=0/10 * * * * ?
```

## Creating a T-Engine

The deployment and development of a T-Engine transformer is simpler
than before.
* Transformers no longer needs to be applied as AMPs/JARs on top of an ACS repository.
* New versions may be deployed separately without restarting the repository.
* As a standalone Spring Boot application develop and test
  cycles are reduced.
* A base Spring Boot application is provided with hook points to extend
  with custom transform code.
* The base also includes the creation of a Docker image for your
  Spring Boot application. Even if you don't intend to deploy with Docker,
  this may still be of interest, as the configuration of any tools or
  libraries used in the transform need only be done once rather than for
  every development or ad-hoc test environment.

### Developing a new T-Engine

The process of developing a new T-Engine is described on the
[Creating a T-Engine](creating-a-t-engine.md) page. It walks
through the steps involved in creating a simple Hello World transformer
and includes commands to help test.

When developing new Local Transformers it is generally a good idea to
increase the frequency of the polling of the various locations that
contain custom Pipeline, Rendition, Mimetype Definitions and also of
the Transform Service.
```properties
mimetype.config.cronExpression=0 0/1 * * * ?
rendition.config.cronExpression=2 0/1 * * * ?
local.transform.service.cronExpression=4 0/1 * * * ?
transform.service.cronExpression=6 0/1 * * * ?
```

### Migrating a Legacy Transformer

ACS 6.2 is useful if you already have custom transformers based on the
legacy approach, because it will allow you to gradually migrate them.
Initially you will probably want to create new T-Engines but run them as
Local Transformers attached directly to an ACS repository. All of the
transforms provided with the base ACS are available as Local
Transformers, so may be combined with your own Local
Transformers in Local Pipelines. Once happy you have every thing working
it then probably makes sense to add your new T-Engines to the Transform
Service. The
[Migrating a Legacy Transformer](migrating-a-legacy-transformer.md) page
helps by showing which areas of legacy code are no longer needed and
which sections can be simply copied and pasted into the new code. Some of
the concepts have changed sightly to simplify what the custom transform
developer needs to do and understand.

## Transform Service Configuration

### Configure a T-Engine in the Transform Service

TODO

### Configure a pipeline in the Transform Service

TODO
