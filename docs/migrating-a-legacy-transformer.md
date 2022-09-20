For context see [Custom Transforms and Renditions](custom-transforms-and-renditions.md) and [Creating A T-Engine](creating-a-t-engine.md).

# Migrating a Legacy Transformer

This section will describe how to migrate custom synchronous transformers
created for Alfresco Content Repository (ACS) prior to version 6.2, to new
asynchronous out of process T-Engines.
The pre 6.2 transformers will be referred to as *Legacy Transformers*.
These have now been removed.
It is assumed that the reader is familiar with creating and configuring
a new T-Engine as described in [Creating a T-Engine](creating-a-t-engine.md).

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
additional custom configuration required by the new transformer, see [example](https://github.com/Alfresco/alfresco-helloworld-transformer/tree/master/alfresco-helloworld-transformer-amp).
All of this functionality can now be added without the need to override
the ACS Spring Bean configuration.

The steps to create and migrate a Legacy Transformer into a custom
T-Engine are as follows:

1. [Creating a T-Engine](creating-a-t-engine.md) walks through how to
develop, configure and run a new t-Engine using a simple Hello World
example.
2. Migrate the custom Legacy Transformer Java code into the new T-Engine
as described in [Migrating custom transform code](#migrating-custom-transform-code).
3. Migrate any custom renditions defined as Spring Beans.
See how to add custom renditions in [Configure a custom rendition](custom-transforms-and-renditions.md#configure-a-custom-rendition)
4. Migrate any custom pipelines defined as Spring Beans.
See how to add a custom pipelines in [Configure a pipeline of Local Transforms](custom-transforms-and-renditions.md#transform-pipelines).
5. Configure ACS to use the new custom T-Engine as described in [Configure a T-Engine as a Local Transform](custom-transforms-and-renditions.md#configure-a-t-engine-as-a-local-transform).

## Migrating custom transform code
Legacy Transformers were implemented by extending a class
`org.alfresco.repo.content.transform.AbstractContentTransformer2`
which was removed in ACS 7.
This implementation requires the Legacy Transformer to define functionality
by implementing the following abstract methods:
* **isTransformableMimetype**
* **transformInternal**

### Migrating isTransformableMimetype
Example of a [legacy Transformer](https://github.com/Alfresco/alfresco-helloworld-transformer/blob/master/alfresco-helloworld-transformer-amp/helloworld-amp/src/main/java/org/alfresco/content/transform/HelloWorldTransformer.java#L56).
```java
public boolean isTransformableMimetype(String sourceMimetype, String targetMimetype, TransformationOptions options)
```
The `isTransformableMimetype` method allowed ACS to determine whether
this transformer is applicable for a given transform request.
When migrating a Legacy Transformer to a T-Engine, this method is no longer
needed.

**How to migrate:**

This functionality is now handled by the t-engine base, but is controlled by configuration returned
from the [getTransformConfig](https://github.com/Alfresco/alfresco-transform-core/blob/master/engines/base/src/main/java/org/alfresco/transform/base/TransformEngine.java#L57) method of the `TransformEngine` interface. See how to define the
configuration in [Transformer Config](custom-transforms-and-renditions.md#transformer-config).

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

Notice how the signature of the [Legacy Transformer's](https://github.com/Alfresco/alfresco-helloworld-transformer/blob/master/alfresco-helloworld-transformer-amp/helloworld-amp/src/main/java/org/alfresco/content/transform/HelloWorldTransformer.java#L35) `transformInternal`
method is similar to the [transform](https://github.com/Alfresco/alfresco-transform-core/blob/master/engines/base/src/main/java/org/alfresco/transform/base/CustomTransformer.java#L46)
method of the `CustomTransformer` interface, implemented by  [HelloTransformer.java](https://github.com/Alfresco/alfresco-helloworld-transformer/blob/master/helloworld-t-engine/src/main/java/org/alfresco/transform/HelloTransformer.java#L47).


## Migrating a Pipeline Transformer

**Legacy Transformers Pipelines**

Pipeline Transformers for the Legacy Transformers were defined using properties in
alfresco-global.properties. The pipline definition syntax via properties is  `Transformer1 | Extension | Transformer2`.
The resulting pipeline transformer will have the same supportedExtension as Transformer1, but the resulting
targetExtension will be the sum of targetExtension(Transformer1) + targetExtension(Transformer2(Extension)).
Additional properties are available:
* `.extension.Ext1.Ext2.supported=false` restricts the transformation from Ext1 to Ext2 for a specific Transformer.
* `.priority=200` sets priority value of the transformer, the values are like the order in a queue,
the lower the number the higher the priority is.
* `.available=false` disables a transformer.

Sample configuration of Legacy Transformer Pipeline
```
# alfresco-pdf-renderer.ImageMagick
# ---------------------------------
# content.transformer.alfresco-pdf-renderer.ImageMagick.pipeline=alfresco-pdf-renderer|png|ImageMagick
# content.transformer.alfresco-pdf-renderer.ImageMagick.priority=200
# content.transformer.alfresco-pdf-renderer.ImageMagick.available=false
# content.transformer.alfresco-pdf-renderer.ImageMagick.extension.ai.jpg.supported=false

```

**How to migrate:**

>See [Transform pipelines](custom-transforms-and-renditions.md#transform-pipelines).

Pipeline definitions are done via JSON rather than alfresco-global.properties. The JSON is able to
perform all the operations that were previously available with alfresco-global.properties with the
following exceptions:

* Timeouts have not been implemented;
* The concept of reading a specified number of bytes and then stopping has not been implemented;
* Transformers cannot be disabled with a `.available=false` property. They can however be removed
  by later transform config.
