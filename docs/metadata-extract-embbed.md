
# Metadata Extractors

## Introduction

The extraction of metadata in the content repository is performed in T-Engines (transform engines).
Prior to ACS 7, it was performed inside the content repository. T-Engines provide improve scalability,
stability, security and flexibility. New extractors may be added without the need for
a new content repository release or applying an AMP on top of the content repository.

The ACS 6 framework for creating metadata extractors that run as part of content repository
still exists, so existing AMPs that add extractors will still work as long as there is
not an extractor in a T-Engine that claims to do the same task. The framework is deprecated and could
well be removed in a future release.

This page describes how to add a custom metadata extractor in a T-Engine and applies to both
Community and Enterprise editions.

It also decsribes the the various Tika based extractors that have been moved to the tika
and all-in-one T-Engines. The Tika rather than the LibreOffice extractor has been used since
ACS 6.0.1. However, the code for LibreOffice has also been moved into a T-Engine in case any
custom code was making use of it.

A framework for embedding metadata into a file was provided as part of the content repository prior
to ACS 7. This too still exists, but has been deprecated. Even though the content repository did not
provide any out of the box implementations, the embedding framework of metadata via T-Engines exists.

## Just another transform
Metadata extractors and embedders are just a specialist form of transform. The `targetMediaType`
in the T-Engine `engine-config.json` is set to `"alfresco-metadata-extract"` or `"alfresco-metadata-embed"`
the following is a snippet from the tika_engine_config.json)[https://github.com/Alfresco/alfresco-transform-core/blob/master/alfresco-transform-tika/alfresco-transform-tika/src/main/resources/tika_engine_config.json]
~~~
    {
      "transformerName": "TikaAudioMetadataExtractor",
      "supportedSourceAndTargetList": [
        {"sourceMediaType": "video/x-m4v",     "targetMediaType": "alfresco-metadata-extract"},
        {"sourceMediaType": "audio/x-oggflac", "targetMediaType": "alfresco-metadata-extract"},
        {"sourceMediaType": "application/mp4", "targetMediaType": "alfresco-metadata-extract"},
        {"sourceMediaType": "audio/vorbis",    "targetMediaType": "alfresco-metadata-extract"},
        {"sourceMediaType": "video/3gpp",      "targetMediaType": "alfresco-metadata-extract"},
        {"sourceMediaType": "audio/x-flac",    "targetMediaType": "alfresco-metadata-extract"},
        {"sourceMediaType": "video/3gpp2",     "targetMediaType": "alfresco-metadata-extract"},
        {"sourceMediaType": "video/quicktime", "targetMediaType": "alfresco-metadata-extract"},
        {"sourceMediaType": "audio/mp4",       "targetMediaType": "alfresco-metadata-extract"},
        {"sourceMediaType": "video/mp4",       "targetMediaType": "alfresco-metadata-extract"}
      ],
      "transformOptions": [
        "metadataOptions"
      ]
    },
~~~

If a T-Engine definition says it supports a metadata extract or embed, it will be used in preference
to any extractor or embedder using the deprecated frameworks in the content repository.

### Transform interface
Code that transforms a specific document type in a T-Engine generally implements the [Transform](https://github.com/Alfresco/alfresco-transform-core/blob/master/alfresco-transformer-base/src/main/java/org/alfresco/transformer/executors/Transformer.java)
interface. In addition to the `transform` method, `extractMetadata` and `embedMetadata` methods
will be called depending on the target media type. The implementing class is called from the
[transformImpl](creating-a-t-engine.md#transformImpl) method of the controller class.

~~~
    default void transform(String transformName, String sourceMimetype, String targetMimetype,
                           Map<String, String> transformOptions,
                           File sourceFile, File targetFile) throws Exception {
    }

    default void extractMetadata(String transformName, String sourceMimetype, String targetMimetype,
                                 Map<String, String> transformOptions,
                                 File sourceFile, File targetFile) throws Exception {
    }

    default void embedMetadata(String transformName, String sourceMimetype, String targetMimetype,
                               Map<String, String> transformOptions,
                               File sourceFile, File targetFile) throws Exception {
    }
~~~

### AbstractMetadataExtractor
The `AbstractMetadataExtractor` may be extended to perform metadata extract and embed tasks. It has two abstract
methods which need to be supplied:
~~~
    public Map<String, Serializable> extractMetadata(String sourceMimetype, Map<String, String> transformOptions,
                                                     File sourceFile) throws Exception;
    public abstract Map<String, Serializable> extractMetadata(String sourceMimetype, Map<String, String> transformOptions,
                                                              File sourceFile) throws Exception;
~~~

If a transform specifies that it can convert from a source mimetype {@code "<MIMETYPE>"} to {@code "alfresco-metadata-extract"}
 * (specified in the {@code engine_config.json}), it is indicating that it can extract metadata from {@code <MIMETYPE>}.

The T-Engine's Controller class will call a method in a class that extends {@link AbstractMetadataExtractor}
 *   based on the source and target mediatypes in the normal way.</li>
 *   <li>The method extracts ALL available metadata is extracted from the document and then calls
 *   {@link #mapMetadataAndWrite(File, Map, Map)}.</li>
 *   <li>Selected values from the available metadata are mapped into content repository property names and values,
 *   depending on what is defined in a {@code "<classname>_metadata_extract.properties"} file.</li>
 *   <li>The selected values are set back to the content repository as a JSON representation of a Map, where the values
 *   are applied to the source node.</li>
 * </ul>
 * To support the same functionality as metadata extractors configured inside the content repository,
 * extra key value pairs may be returned from {@link #extractMetadata}. These are:
 * <ul>
 *     <li>{@code "sys:overwritePolicy"} which can specify the
 *     {@code org.alfresco.repo.content.metadata.MetadataExtracter.OverwritePolicy} name. Defaults to "PRAGMATIC".</li>
 *     <li>{@code "sys:enableStringTagging"} if {@code "true"} finds or creates tags for each string mapped to
 *     {@code cm:taggable}. Defaults to {@code "false"} to ignore mapping strings to tags.</li>
 *     <li>{@code "sys:carryAspectProperties"} </li>
 *     <li>{@code "sys:stringTaggingSeparators"} </li>
 * </ul>
 *
 * If a transform specifies that it can convert from {@code "<MIMETYPE>"} to {@code "alfresco-metadata-embed"}, it is
 * indicating that it can embed metadata in {@code <MIMETYPE>}.
 *
 * The transform results in a new version of supplied source file that contains the metadata supplied in the transform
 * options.
 
 ~~~
    public abstract Map<String, Serializable> extractMetadata(String sourceMimetype, Map<String, String> transformOptions,
                                                              File sourceFile) throws Exception;

    public abstract Map<String, Serializable> extractMetadata(String sourceMimetype, Map<String, String> transformOptions,
                                                              File sourceFile) throws Exception;
~~~

### Extract Request


The request from the content repository goes through `RenditionService2`, so will use the asynchronous Alfresco
Transform Service if available and a synchronous Local transform if not.

Normally the only transform option is a `timeout`, so the extractor code only has the source mimetype
and content itself to work on. Although strongly discouraged it is possible for code running in the content repository
to specify its own mapping of content metadata properties to repository properties.


TODO example

Some requests optionally also contains a `extractMapping` option. This however is deprecated so should not be used
as it will go away in a future release. Possibly even in the lifetime of ACS 7. It is there to support
code that currently exists as an AMP which expects to be able to modify the mappings of metadata in the
content to repository properties, but from the repository side. This mapping is normally done in the T-Engine.

TODO example of the mapping option (needed by RM)

### Extract Response
The transformed content that is returned to the content repository is a json file that specifies which properties
should be updated on the source node.

TODO Example followed by explination

### Embed Request

### Embed Response
This is simply the source content with updated metadata.

## Content repository

### Framework
The ACS 6 framework for running metadata extractors and embedders still exists. An additional `AsynchronousExtractor`
has been added to communicate with T-Engines from ACS 7. The `AsynchronousExtractor` handles the request and responce
in a generic way allowing all the content type specific code to be moved to a T-Engine.

### XML framework
The following XML based extractors have NOT been removed from the content repository as custom extensions may be
using them. There are no out-of-the-box extractors that use them as part of the content repository. Ideally any
custom extensions should be moved to a custom T-Engine using code based on these classes.
* XmlMetadataExtracter
* XPathMetadataExtracter


## Tika extractor
 - list
 - config files (locations/overriding).
 - no spring config

## Example extractor

TODO dummy code that just returns hard coded values.

## Overriding mappings on the repository side (deprecated)

As described above it is possible to override the mapping of metadata to properties from the content repository.
This is already deprecated and likely to be removed even in the ACS 7 lifetime, so should not be used.

TODO explain how RM can do this. Possible we remove this section and the one above into the code used by RM.