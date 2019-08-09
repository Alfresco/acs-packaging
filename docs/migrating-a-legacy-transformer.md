# Migrating a Legacy Transformer

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

## Migrating custom transform code
Legacy Transformers are implemented by extending a now deprecated class
`org.alfresco.repo.content.transform.AbstractContentTransformer2`.
This implementation requires the Legacy Transformer to define functionality
by implementing the following abstract methods:
* **isTransformableMimetype**
* **transformInternal**

### Migrating isTransformableMimetype
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

### Migrating transformInternal
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
