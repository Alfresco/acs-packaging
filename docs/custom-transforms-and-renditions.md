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
Alfresco Transform Service (ATS) which provides more balanced throughput.
A T-Engine is intended to be run as a 
Docker image, but may also be run as a standalone process.

Prior to ACS 6.0 Legacy transformers ran within the same JVM as
the ACS repository. They and their supporting classes were deprecated
and in ACS 6, but have now been removed. ACS 6.2 still uses them if a rendition
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

### Enabling and disabling Local or Transform Service transforms

Local or Transform Service transforms can be enabled or disabled
independently of each other. The ACS repository will try to transform
content using the Transform Service if possible and falling back to a Local
Transform. If you are using Share, Local Transforms are required, as they support
both synchronous and asynchronous requests. Share makes use of both, so
functionality such as preview will be unavailable if Local transforms are
disabled. The Transform service only supports asynchronous requests.

The following sections will show how to create a Local transform pipeline, a Local
transform failover or a Local transform override, but remember that they will not be
used if the Transform Service (ATS) if it is able to do the transform and is enabled.

```properties
transform.service.enabled=true
local.transform.service.enabled=true
```

Setting the enabled state to **false** will disable all of the transforms
performed by that particular service. It is possible to disable individual
Local Transforms by setting the corresponding T-Engine URL property
**localTransform.&lt;engineName>.url** value to an empty string.

```properties
localTransform.helloworld.url=
```

### T-Engine and Pipeline Config

#### Transformer selection strategy
The ACS repository and ATS router both use the
[T-Engine configuration](creating-a-t-engine.md#t-engine-configuration) in combination with their
own pipeline files to choose which T-Engine will perform a transform. A transformer
definition contains a supported list of source and target Media Types. This is used
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

The configuration may impose a source file size limit resulting in the selection of a
different transformer. Size limits are normally added to avoid the transforms consuming too many
resources. The configuration may also specify a priority which will be used in Transformer
selection if there and a number of options. The highest priority is the one with the lowest number. 

#### Configure a custom Local transform pipeline

Local Transforms may be combined together in a pipeline to form a new
transform, where the output from one becomes the input to the next and
so on. A pipeline definition (JSON) defines the sequence of
transforms and intermediate Media Types. Like any other transformer, it
specifies a list of supported source and target Media Types. If you don't supply any,
all possible combinations are assumed to be available. The
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
        {"sourceMediaType": "text/plain", "priority": 45,  "targetMediaType": "text/plain" }
      ],
      "transformOptions": [
        "helloWorldOptions"
      ]
    }
  ]
}
```

* **transformerName** - Try to create a unique name for the transform.
* **transformerPipeline** - A list of transformers in the pipeline.
The **targetMediaType** specifies the intermediate Media Types between
transformers. There is no final targetMediaType as this comes from the
supportedSourceAndTargetList.
* **supportedSourceAndTargetList** - The supported source and target
Media Types, which refer to the Media Types this pipeline transformer
can transform from and to, additionally you can set the priority and the
maxSourceSizeBytes see [Supported Source and Target List](https://github.com/Alfresco/alfresco-transform-core/blob/master/docs/engine_config.md#supported-source-and-target-list).
If blank, this indicates that all possible combinations are supported.
This is the cartesian product of all source types to the first
intermediate type and all target types from the last intermediate type.
Any combinations supported by the first transformer are excluded. They
will also have the priority from the first transform.
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
One way is to use the following command, and it will be picked up the
next time the location is read, which is dependent on the cron values.

```bash
docker cp custom_pipelines.json <alfresco container>:/usr/local/tomcat/shared/classes/alfresco/extension/transform/pipelines/
```

In a Kubernetes environment, [ConfigMaps](https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/)
can be used to add pipeline definitions. You will need to create
a ConfigMap from the JSON file and mount the ConfigMap through a volume
to the ACS repository pods.

```bash
kubectl create configmap custom-pipeline-config --from-file=name_of_a_file.json
```

The necessary volumes are already provided out of the box and the files
in ConfigMap `custom-pipeline-config` will be mounted to
`/usr/local/tomcat/shared/classes/alfresco/extension/transform/pipelines/`.
Again, the files will be picked up the next time the location is read,
or when the repository pods are restarted.

> From Kubernetes documentation: Caution: If there are some files
in the mountPath location, they will be deleted.

#### Configure custom Local failover transforms

A failover transform, simply provides a list of transforms to be
attempted one after another until one succeeds. For example, you may have a fast transform that is able to handle a
limited set of transforms and another that is slower but handles all cases.
```json
{
  "transformers": [
    {
      "transformerName": "imgExtractOrImgCreate",
      "transformerFailover" : [ "imgExtract", "imgCreate" ],
      "supportedSourceAndTargetList": [
        {"sourceMediaType": "application/vnd.oasis.opendocument.graphics", "priority": 150, "targetMediaType": "image/png" },
        ...
        {"sourceMediaType": "application/vnd.sun.xml.calc.template",       "priority": 150, "targetMediaType": "image/png" }
      ]
    }
  ]
}
```
* **transformerName** - Try to create a unique name for the transform.
* **transformerFaillover** - A list of transformers to try.
* **supportedSourceAndTargetList** - The supported source and target
Media Types, which refer to the Media Types this failover transformer
can transform from and to, additionally you can set the priority and the
maxSourceSizeBytes see [Supported Source and Target List](https://github.com/Alfresco/alfresco-transform-core/blob/master/docs/engine_config.md#supported-source-and-target-list).
Unlike pipelines, it must not be blank.
* **transformOptions** - A list of references to options required by
the pipeline transformer.

#### Overriding a Local transform
In the same way as it is possible combine Local transforms into pipelines, it is also possible to override a
previously defined transform in a file in the _local.transform.pipeline.config.dir_ directory. The last definition read
wins. The configuration from T-Engines or the Transform Service is initially read followed by files in this directory.
Files are read in alphanumeric order. So _0100-basePipelines.json_ is read before _0200-a-cutdown-libreoffice.json_. The
following example removes most of the supported source to target media types form the standard _"libreoffice"_
transform. This is not something you would normally want to do. It also changes the max size and priority of others.

```json
{
  "transformers": [
    {
      "transformerName": "libreoffice",
      "supportedSourceAndTargetList": [
        {"sourceMediaType": "text/csv", "maxSourceSizeBytes": 1000, "targetMediaType": "text/html" },
        {"sourceMediaType": "text/csv", "targetMediaType": "application/vnd.oasis.opendocument.spreadsheet" },
        {"sourceMediaType": "text/csv", "targetMediaType": "application/vnd.oasis.opendocument.spreadsheet-template" },
        {"sourceMediaType": "text/csv", "targetMediaType": "text/tab-separated-values" },
        {"sourceMediaType": "text/csv", "priority": 45, "targetMediaType": "application/vnd.ms-excel" },
        {"sourceMediaType": "text/csv", "priority": 155, "targetMediaType": "application/pdf" }
      ]
    }
  ]
}
```

#### Adding pipelines and failover transforms to a T-Engine
So far we have talked about defining pipelines and failover transforms in the ACS repository's or
ATS router's pipeline files. It is also possible to add them to the T-Engine's configuration, even
when they reference a transformer provided by another T-Engine. It is only when all the
configuration files are combined that there may be warning messages issued if these transformers
does not exist.

Generally it is better to add them to T-Engines to avoid duplication (in the repository and
router) and adds flexibility.

The following sections describe ways to modify the configuration that has already been read.
This too may be included in the T-Engine configuration.

#### Removing a transformer

Since ACS 7.2 & ATS 1.4.2

To discard a previous transformer definition include its name in the optional
`"removeTransformers"` list. You might want to do this if you have a replacement and wish keep
the overall configuration simple (so contains no alternatives), or you wish to temporarily
remove it. The following example removes two transformers.

```json
{
  "removeTransformers" : [
    "libreoffice",
    "Archive"
   ]
  ...
}
```

#### Overriding the supportedSourceAndTargetList

Since ACS 7.2 & ATS 1.4.2

Rather than totally override an existing transform definition from another T-Engine or pipeline file,
it is generally simpler and better to modify the `"supportedSourceAndTargetList"` by adding
elements to the optional `"addSupported"`, `"removeSupported"` and `"overrideSupported"` lists.
You will need to specify the `"transformerName"` but you will not need to repeat all the other
`"supportedSourceAndTargetList"` values, which means if there are changed in the original, the
same change is not needed in a second place. The following example adds one transform, removes
two others and changes the `"priority"` and `"maxSourceSizeBytes"` of another.
```json
{
  "addSupported": [
    {
      "transformerName": "Archive",
      "sourceMediaType": "application/zip",
      "targetMediaType": "text/csv",
      "priority": 60,
      "maxSourceSizeBytes": 18874368
    }
  ],
  "removeSupported": [
    {
      "transformerName": "Archive",
      "sourceMediaType": "application/zip",
      "targetMediaType": "text/xml"
    },
    {
      "transformerName": "Archive",
      "sourceMediaType": "application/zip",
      "targetMediaType": "text/plain"
    }
  ],
  "overrideSupported": [
    {
      "transformerName": "Archive",
      "sourceMediaType": "application/zip",
      "targetMediaType": "text/html",
      "priority": 60,
      "maxSourceSizeBytes": 18874368
    }
  ]
  ...
}
```

#### Default maxSourceSizeBytes and priority values

Since ACS 7.2 & ATS 1.4.2

When defining `"supportedSourceAndTargetList"` elements the `"priority"` and `"maxSourceSizeBytes"` are optional
and normally have the default values of `50` and `-1` (no limit). It is possible to
change those defaults. In precedence order from most specific to most general these are defined by combinations of
`"transformerName"` and `"sourceMediaType"`.
* **transformer and source media type default** - both specified
* **transformer default** - only the transformer name is specified
* **source media type default** - only the source media type is specified
* **system wide default** - neither are specified.

Both `"priority"` and `"maxSourceSizeBytes"` may be specified in an element, but if only one is
specified it is only that value that is being defaulted.

Being able to change the defaults is particularly useful once a T-Engine has been developed as it
allows a system administrator to handle limitations that are only found later. The
**system wide defaults** are generally not used but are included for completeness.
The following example says that the `"Office"` transformer by default should only handle zip files up to 18 Mb and by 
default the maximum size of a `.doc` file to be transformed is 4 Mb. The later example also defaults the priority,
possibly allowing another transformer that has specified a priority of say `50` to be used in
preference.
 
```json
{
  "supportedDefaults": [
    {
      "transformerName": "Office",             // default for a source type within a transformer
      "sourceMediaType": "application/zip",
      "maxSourceSizeBytes": 18874368
    },
    {
      "sourceMediaType": "application/msword", // defaults for a source type
      "maxSourceSizeBytes": 4194304,
      "priority": 45
    },
    {
      "priority": 60                           // system wide default
    },
    {
      "maxSourceSizeBytes": -1                 // system wide default
    }
  ]
  ...
}
```
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
If you specify **sourceNodeRef** without a value,
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

In a Kubernetes environment:
```bash
kubectl create configmap custom-rendition-config --from-file=name_of_a_file.json
```

The necessary volumes are already provided out of the box and the files
in ConfigMap `custom-rendition-config` will be mounted to
`/usr/local/tomcat/shared/classes/alfresco/extension/transform/renditions/`.
Again, the files will be picked up the next time the location is read,
or when the repository pods are restarted.

#### Disabling an existing rendition
Just like transforms, it is possible to override renditions. The following example effectively
disables the `doclib` rendition, used to create the thumbnail images in Share's Document Library
page and other client applications. A good name for this file might be
`0200-disableDoclib.json`.

~~~
{
  "renditions": [
    {
      "renditionName": "doclib",
      "targetMediaType": "image/png",
      "options": [
        {"name": "unsupported", "value": 123}
      ]
    }
  ]
}
~~~
Because there is not a transformer with an transform option called `unsupported`, the rendition
can never be performed. Having turned on `TransformerDebug` logging you normally you would see a
transform taking place for `-- doclib --` when you upload a file in Share. With this override the
doclib transform does not appear.

#### Overriding an existing rendition
It is possible to change a rendition by overriding it. The following `0300-biggerThumbnails.json`
file changes the size of the `doclib` image from `100x100` to be `123x123` and introduces another
rendition called `biggerThumbnail` that is `200x200`.
~~~
{
  "renditions": [
    {
      "renditionName": "doclib",
      "targetMediaType": "image/png",
      "options": [
        {"name": "resizeWidth", "value": 123},
        {"name": "resizeHeight", "value": 123},
        {"name": "allowEnlargement", "value": false},
        {"name": "maintainAspectRatio", "value": true},
        {"name": "autoOrient", "value": true},
        {"name": "thumbnail", "value": true}
      ]
    },
    {
      "renditionName": "biggerThumbnail",
      "targetMediaType": "image/png",
      "options": [
        {"name": "resizeWidth", "value": 200},
        {"name": "resizeHeight", "value": 200},
        {"name": "allowEnlargement", "value": false},
        {"name": "maintainAspectRatio", "value": true},
        {"name": "autoOrient", "value": true},
        {"name": "thumbnail", "value": true}
      ]
    }
  ]
}
~~~

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

In a Kubernetes environment:
```bash
kubectl create configmap custom-mimetype-config --from-file=name_of_a_file.json
```

The necessary volumes are already provided out of the box and the files
in ConfigMap `custom-mimetype-config` will be mounted to
`/usr/local/tomcat/shared/classes/alfresco/extension/mimetypes`.
Again, the files will be picked up the next time the location is read,
or when the repository pods are restarted.

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

## ATS Configuration
### Configure a T-Engine as a Remote Transform

Please follow the steps described in [Alfresco Transform Service Docs](https://github.com/Alfresco/alfresco-transform-service/blob/master/docs/custom-t-engine.md).

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
