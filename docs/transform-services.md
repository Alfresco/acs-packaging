# Transform Service

## Transform Service and Local Transforms

The Transform Service performs transformations for Alfresco Content Service
in docker containers providing greater scalability. Requests to the
Transform Service are placed on a queue and processed asynchronously.
Security is also improved by better isolation.

It is possible to [turn on and off](https://github.com/Alfresco/acs-packaging/blob/feature/REPO-4338_helloworld_transformer/docs/custom-transforms-and-renditions.md#enabling-and-disabling-legacy-local-or-transform-service-transforms) Local, Legacy and Transform Service
transforms by setting Alfresco global properties.
By default the Transform Server is disabled for the zip distribution
but enabled for docker-compose and helm deployments.

### When are Transform Service or Local or Legacy Transforms used?

Requests made to the V1 REST API for renditions make use of the *RenditionService2*
which supports asynchronous requests. A request may be made on one Alfresco node
and the resulting transform from the Transform Service may be processed on a
different node.

The *RenditionService2* will use the Transform Service if it is available
and is known to support the transformation specified in the *RenditionDefinition2*.
If not, the service with fallback to the transformers performed locally by
Alfresco Content Service.

For more information see [Custom Transforms and Renditions](custom-transforms-and-renditions.md)).

The original *RenditionService* is now deprecated as it supports synchronous
requests or requests that have callbacks that must be processed on the same
node. It still exists and a number of existing calls that can be asynchronous
without a callback been modified to use the *RenditionService2* if there is an
equivalent *RenditionDefinition2*.
