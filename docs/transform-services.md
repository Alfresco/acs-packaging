# Transform Service

## Transform Service and Local Transforms

The Transform Service performs transformations for Alfresco Content Service
in docker containers providing greater scalability. Requests to the
Transform Service are placed on a queue and processed asynchronously.
Security is also improved by better isolation.

The initial version of the Transform Service provides a limited number of
transformations so transformation still need to be performed locally
by the Alfresco Content Service as took place in previous versions.

Currently the supported transformations provided by the Transform Service are
specified in the file *transform-service-config.json*. A future version is
likely to obtain this configuration dynamically.

### Enabling and disabling Legacy, Local or Transform Service transforms
It is possible to turn on and off Local, Legacy and Transform Service
transforms by setting Alfresco global properties.
By default the Transform Server is disabled for the zip distribution
but enabled for docker-compose and helm deployments.
For more information see [here](custom-transforms-and-renditions.md#enabling-and-disabling-legacy,-local-or-transform-service-transforms)

```bash
transform.service.enabled=false
local.transform.service.enabled=true
legacy.transform.service.enabled=true
```

### When are Transform Service or Local or Legacy Transforms used?

Requests made to the V1 REST API for renditions make use of the *RenditionService2*
which supports asynchronous requests. A request may be made on one Alfresco node
and the resulting transform from the Transform Service may be processed on a
different node.

The *RenditionService2* will use the Transform Service if it is available
and is known to support the transformation specified in the *RenditionDefinition2*.
If not, the service with fallback to the transformers performed locally by
Alfresco Content Service (the same ones supported in previous releases).

Legacy Transforms includes those run within the JVM of the Alfresco node,
in external processes (such as LibreOffice and ImageMagick) located on the same
machine. Local Transforms run within Docker transformers (introduced in ACS 6.0)
called T-Engines, for more information see [here](custom-transforms-and-renditions.md)).
The same Docker transformer images are also used by the Transform Service albeit via a
different API.

The original *RenditionService* is now deprecated as it supports synchronous
requests or requests that have callbacks that must be processed on the same
node. It still exists and a number of existing calls that can be asynchronous
without a callback been modified to use the *RenditionService2* if there is an
equivalent *RenditionDefinition2*.


### API between the Transform Service and clients

TBA

### Rendition Service 2 Definitions

TBA

### Migrating custom transformers

TBA
