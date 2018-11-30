# Transform Service

## Remote Transform Service and local transforms

The Transform Service performs transformations for Alfresco Content Service
in remote docker containers providing greater scalability. Requests to the
Transform Service are placed on a queue and processed asynchronously.
Security is also improved by better isolation.

The initial version of the Transform Service provides a limited number of
transformations so transformation still need to be performed locally
by the Alfresco Content Service as took place in previous versions.

Currently the supported transformations provided by the Transform Service are
specified in the file *transform-service-config.json*. A future version is
likely to obtain this configuration dynamically.

### Turning local and remote transformation on and off
It is possible to turn on and off local and remote transformations by
setting Alfresco global properties. By default the Transform Server is disabled
for the zip distribution but enabled for docker-compose and helm deployments.

```bash
local.transform.service.enabled=true
transform.service.enabled=false
```

### When are remote or local transforms used?

Requests made to the V1 REST API for renditions make use of the *RenditionService2*
which supports asynchronous requests. A request may be made on one Alfresco node
and the resulting transform from the Transform Service may be processed on a
different node.

The *RenditionService2* will use the remote Transform Service if it is available
and is known to support the transformation specified in the *RenditionDefinition2*.
If not, the service with fallback to the transformers performed locally by
Alfresco Content Service (the same ones supported in previous releases).

Local transforms includes those run within the JVM of the Alfresco node,
in external processes (such as LibreOffice and ImageMagick) located on the same
machine and in Docker transformers (introduced in ACS 6.0). The same Docker
transformer images are also used by the Transform Service albeit via a
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
