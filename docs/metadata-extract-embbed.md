
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

This page describes how metadata extraction and embedding works, so that it is possible to add a
custom T-Engine to do other types. This applies to both Community and Enterprise editions.

It also lists the various extractors that have been moved to T-Engines.

A framework for embedding metadata into a file was provided as part of the content repository prior
to ACS 7. This too still exists, but has been deprecated. Even though the content repository did not
provide any out of the box implementations, the embedding framework of metadata via T-Engines exists.

In the case of an extract, the T-Engine returns a JSON file that contains name value pairs. The names
are fully qualified QNames of properties on the source node. The values are the metadata values extracted
from the content. The transform defines the mapping of metadata values to properties. Once returned to
the content repository, the properties are automatically set.

In the case of an embed, the T-Engine takes name value pairs from the transform options, maps them to
metadata values which are then updated in the supplied content. The content is then returned to the 
content repository and the node is updated. 

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

It is typical for the `extractMetadata` method to call another `extractMetadata` method on a sub class of
`AbstractMetadataExtractor` as this class provide the bulk of the functionallity needed to configure metadata extraction
or embedding.
~~~
    public void extractMetadata(String transformName, String sourceMimetype, String targetMimetype,
                                Map<String, String> transformOptions,
                                File sourceFile, File targetFile) throws Exception
    {
        AbstractMetadataExtractor extractor = ...
        extractor.extractMetadata(sourceMimetype, transformOptions, sourceFile, targetFile);
    }

    // Similar code for embedMetadata
~~~

### AbstractMetadataExtractor
The `AbstractMetadataExtractor` may be extended to perform metadata extract and embed tasks, by overriding two methods
in the sub classes:
~~~
    public abstract Map<String, Serializable> extractMetadata(String sourceMimetype, Map<String, String> transformOptions,
                                                                  File sourceFile) throws Exception;

    public void embedMetadata(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
                              File sourceFile, File targetFile) throws Exception
    {
        // Default nothing, as embedding is not supported in most cases
    }
~~~
Method parameters:

* **sourceMimetype** mimetype of the source
* **transformOptions** transform options from the client
* **sourceFile** the source as a file

* The `extractMetadata` should extract and return ALL available metadata from the sourceFile.
* These values are then mapped into content repository property names and values, depending on what is defined in a
  `<classname>_metadata_extract.properties`"}` file. Value may be discarded or a single value may even be used for
  multiple properties.
* The selected values are set back to the content repository as JSON as a mapping of fully qualified content repository
  property names to values, where the values are applied to the source node.

### &lt;classname>_metadata_extract.properties

The `AbstractMetadataExtractor` class reads the `<classname>_metadata_extract.properties` file, so that it knows how to
map metadata returned from the sub class `extractMetadata` method onto crontent repository properties. The following is
an example for an email (file extension eml):

~~~
#
# RFC822MetadataExtractor - default mapping
#

# Namespaces
namespace.prefix.imap=http://www.alfresco.org/model/imap/1.0
namespace.prefix.cm=http://www.alfresco.org/model/content/1.0

# Mappings
messageFrom=imap:messageFrom, cm:originator
messageTo=imap:messageTo, cm:addressee
messageCc=imap:messageCc, cm:addressees
messageSubject=imap:messageSubject, cm:title, cm:description, cm:subjectline
messageSent=imap:dateSent, cm:sentdate
messageReceived=imap:dateReceived
Thread-Index=imap:threadIndex
Message-ID=imap:messageId
~~~
As can be seen, the email's metadata for `messageFrom` (if available) will be used to set two properties in the content
repository (if they exist): `imap:messageFrom`, `cm:originator`. The property names use namespace prefixes specified above.


### overwritePolicy
It is possible to specify if properties in the content repository will be set depending on the values extracted
or the properties already set on the node. By default, `PRAGMATIC` is used. Generally you will not need to change this.
Other values (`CAUTIOUS`, `EAGER`, `PRUDENT`) are described in [OverwritePolicy](https://github.com/Alfresco/alfresco-community-repo/blob/master/repository/src/main/java/org/alfresco/repo/content/metadata/MetadataExtracter.java#L70-L318).
To use a different policy add `"sys:overwritePolicy"` with the required property to the Map returned from
`extractMetadata`.

### enableStringTagging

TODO

### carryAspectProperties

TODO

### stringTaggingSeparators

TODO

### Extract Request


The request from the content repository to extract metadata goes through `RenditionService2`, so will use the asynchronous Alfresco
Transform Service if available and synchronous Local transform if not.

Normally the only transform options are `timeout` and `sourceEncoding`, so the extractor code only has the source mimetype
and content itself to work on. Customisation of mapping should really be done in the T-Engine as described above.
However, it is currently possible for code running in the content repository to override the default mapping of metadata
to content properties, with an `extractMapping` transform option. This approach is deprecated and may be removed in
a future minor ACS 7.x release. An AMP should supply a class that implements the `MetadataExtractorPropertyMappingOverride` interface and add it to the
`metadataExtractorPropertyMappingOverrides` property of the `extractor.Asynchronous` spring bean.
~~~
/**
 * Overrides the default metadata mappings for PDF documents:
 *
 * <pre>
 * author=cm:author
 * title=cm:title
 * subject=cm:description
 * created=cm:created
 * </pre>
 * with:
 * <pre>
 * author=cm:author
 * title=cm:title,cm:description
 * </pre>
 */
public class PdfMetadataExtractorOverride implements MetadataExtractorPropertyMappingOverride {
    @Override
    public boolean match(String sourceMimetype) {
        return MIMETYPE_PDF.equals(sourceMimetype);
    }

    @Override
    public Map<String, Set<String>> getExtractMapping(NodeRef nodeRef) {
        Map<String, Set<String>> mapping = new HashMap<>();
        mapping.put("author", Collections.singleton("{http://www.alfresco.org/model/content/1.0}author"));
        mapping.put("title",  Set.of("{http://www.alfresco.org/model/content/1.0}title",
                                     "{http://www.alfresco.org/model/content/1.0}description"));
        return mapping;
    }
}
~~~
Resulting in a request that contains the following transform options:
~~~
{"extractMapping":{
   "author":["{http://www.alfresco.org/model/content/1.0}author"],
   "title":["{http://www.alfresco.org/model/content/1.0}title",
            "{http://www.alfresco.org/model/content/1.0}description"]},
 "timeout":20000,
 "sourceEncoding":"UTF-8"}
~~~


### Extract Response
The transformed content that is returned to the content repository is json that specifies which properties
should be updated on the source node. For example:

~~~
{"{http://www.alfresco.org/model/content/1.0}description":"Making Bread",
 "{http://www.alfresco.org/model/content/1.0}title":"Making Bread",
 "{http://www.alfresco.org/model/content/1.0}author":"Fred"}
~~~

### Embed Request
An embed request simply contains a transform option called `metadata` that contains a map of metadata names to
values, resulting in transform options like the following:
~~~
{"timeout":20000, "sourceEncoding":"UTF-8", "metadata":{"author":"Fred", "title":"Making Bread"}}
~~~

### Embed Response
This is simply the source content with the metadata embedded. The content repository updates
the content of the node with what is returned.

## Content repository

### Framework
The ACS 6 framework for running metadata extractors and embedders still exists. An additional `AsynchronousExtractor`
has been added to communicate with the `RenditionService2` from ACS 7. The AsynchronousExtractor handles the request and response
in a generic way allowing all the content type specific code to be moved to a T-Engine.

### XML framework
The following XML based extractors have NOT been removed from the content repository as custom extensions may be
using them. There are no out-of-the-box extractors that use them as part of the content repository. Ideally any
custom extensions should be moved to a custom T-Engine using code based on these classes.
* XmlMetadataExtracter
* XPathMetadataExtracter


## Extractor that have be moved to T-Engines

The following extractors exist now in T-Engines rather than the content repository:

* OfficeMetadataExtractor
* TikaAutoMetadataExtractor
* DWGMetadataExtractor
* OpenDocumentMetadataExtractor
* PdfBoxMetadataExtractor
* MailMetadataExtractor
* PoiMetadataExtractor
* TikaAudioMetadataExtractor
* MP3MetadataExtractor
* HtmlMetadataExtractor
* RFC822MetadataExtractor

The `LibreOffice` extractor has also been moved to a T-Engine, even though Tika based extractors are  now used for all
types it supported. This has been the case since ACS 6.0.1. It was moved into a T-Engine to simplify 
moving any any custom code that may have extended it.

The `Tika` based classes for extractors using configuration files or spring context files have been removed from the
content repository as the preferred way to create extractors is via a T-Engine and these approaches require in process
extensions.
