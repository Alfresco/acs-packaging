# Transform Service

## Transform Service and Local Transforms

The Transform Service performs transformations for Alfresco Content Service
in docker containers providing greater scalability. Requests to the
Transform Service are placed on a queue and processed asynchronously.
Security is also improved by better isolation.

Local transforms use the same docker containers but provide synchronous transforms
to the Alfresco Content Service. They may be used as an alternative to the Transform
Service using a simpler architecture that does not include a queue or transform router.

It is possible to [turn on and off](custom-transforms-and-renditions.md#enabling-and-disabling-legacy-local-or-transform-service-transforms) Local, Legacy and Transform Service
transforms by setting Alfresco global properties.
By default the Transform Service is disabled for the zip distribution
but enabled for docker-compose and helm deployments.

### When is the Transform Service or a Local Transform used?

Requests made to the V1 REST API for renditions make use of the *RenditionService2*
which supports asynchronous requests. A request may be made on one Alfresco node
and the resulting transform from the Transform Service may be processed on a
different node.

The *RenditionService2* will use the Transform Service if it is available
and is known to support the transformation specified in the *RenditionDefinition2*.
If not, the service with fallback to the Local transforms if they support the transform.

For more information see [Custom Transforms and Renditions](custom-transforms-and-renditions.md).

The original *RenditionService* and *ThumbnailService* are now deprecated as they support synchronous
requests or have callbacks that must be processed on the same node. Local transforms support these older interfaces.
Where the call is asynchronous (without a callback) the code has been modified to use the *RenditionService2* if there
is an equivalent *RenditionDefinition2*.
