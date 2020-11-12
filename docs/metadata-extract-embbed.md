
# Metadata Extractors

## Introduction

The metadata extractors provided as part of the content repository in ACS 6 and before
have been moved into the T-Engines (transform engines) in ACS 7 to improve scalability,
stability, security and flexibility. New extractors may be added without the need for
a new content repository release or applying an AMP on top of the content repository.

The framework for creating metadata extractors that run as part of content repository
still exists, so existing AMPs that add extractors will still work as long as there is
not an extractor in a T-Engine that claims to do the same task.

This page describes how to add a custom metadata extractor in a T-Engine and applies to both
Community and Enterprise editions.

It also decsribes the the various Tika based extractors that have been moved to the tika
and all-in-one T-Engines.
The Tika rather that the LibreOffice extractor has been used since ACS 6.0.1. However the code
for LibreOffice has also been moved into a T-Engine in case there is any custom code was
making use of it.

A framework for embedding metadata back into a file was provided as part of the content
repository in ACS 6 and before. Even though the content repository did not provide any
out of the box implemtations, this too is supported via a T-Engine from ACS 7.

## Just another transform
Metadata extractors and embedders are just a specialist form of transform. The ~targetMediaType~
in the T-Engine ~engine-config.json~ is set to ~alfresco-metadata-extract~ or ~alfresco-metadata-embed~
the following is a snipit from the tika_engine_config.json)[]
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
      ]
    },
~~~

If a T-Engine definition says it supports a metadata extract or embed, it will be used in preference
to any extractor or embedder in the content repository.

### Transform interface
The code that actually transforms a specific document type in a T-Engine generally implements the [Transform](https://github.com/Alfresco/alfresco-transform-core/blob/master/alfresco-transformer-base/src/main/java/org/alfresco/transformer/executors/Transformer.java)
interface. In addition the ~transform~ method there are also ~extractMetadata~ and ~embedMetadata~ methods
which will be called depending on the target media type.

### Extract Request
The request from the content repository goes through the ~RenditionService2~ as you would expect. Normally
the only transform option is a timeout, so the extractor code only has the source mimetype and the content itself
to work on.

TODO example

Some requests optionally also contains a ~~~mapping~~ option. This however is deprecated so should not be used
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
The ACS 6 framework for running metadata extractors and embedders still exists. An additional ~AsynchronousExtractor~
has been added to communicate with T-Engines from ACS 7. The ~AsynchronousExtractor~ handels the request and responce
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