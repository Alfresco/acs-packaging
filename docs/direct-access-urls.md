Direct Access URLs
==================

## Purpose
The main purpose of the _Direct Access URLs_ is to accelerate the local download of content.

___


## Overview
AWS S3 provides a way of generating
[pre-signed URLs](https://docs.aws.amazon.com/AmazonS3/latest/dev/ShareObjectPreSignedURL.html)
for sharing objects. This feature is a perfect candidate for the implementation of direct
access to our content (e.g. shortcutting the Shared File Store for transformations or
faster/direct access in the context of ADW to documents).

>:warning: **Note:** The AWS S3 pre-signed URLs are temporary links with an expiration time.

The repository infrastructure now supports direct access urls. This includes the ContentService
and the ContentStore interface for which default methods have been provided so that ContentStore implementations that 
implement the old version of this interface will throw a Not Supported exception.
The new methods are auditable using the node reference and time in seconds for which the direct access URL is
valid for as the  parameters.

**Rest API endpoints** can be used for requesting a new _Direct Access URL_ (a
direct download link) for a specific file in the Content Repository.

The access to _Direct URLs_ is strictly controlled. Their expiration date is
set/restricted by configurations in Alfresco Repository using global & content-store specific
properties. Values in the content store properties **default expiry time** and **maximum expiry time**, if valid, will
be used in preference to the system-wide properties. If invalid, an attempt will be made to default to the system-wide
properties, however, if that still does not result in a valid configuration, the DAUs for that specific content store will
be disabled.


### Configurations

#### System wide configuration (ACS)
ACS system-wide configuration settings:
* **`system.directAccessUrl.enabled=false`**
    - Controls whether this feature is available, system wide.
    - For DAUs to work, the feature needs to be enabled both system-wide and on the individual
      content-store.
* **`system.directAccessUrl.defaultExpiryTimeInSec=30`**
    - Sets the default expiry time for the DAU across all Content Stores.
    - It’s value cannot exceed the system-wide max expiry time
      (`system.directAccessUrl.maxExpiryTimeInSec`), it can only be equal or lower (_all DAUs disabled
      otherwise_).
    - This property is **mandatory** if DAUs are enabled (system-wide) - (_all DAUs disabled
      otherwise_).
* **`system.directAccessUrl.maxExpiryTimeInSec=300`**
    - Sets the upper limit for the DAUs expiry time, meaning a Content Store will be able to
      override this value but not exceed it, and the same goes for the clients. A service (Java
      Interface) client will be able to request a DAU for a custom expiry time but that time can’t
      exceed this value.
    - If the requested time exceeds the max value, the expiry time reverts to the default
      configured one.
    - This property is **mandatory** if DAUs are enabled (system-wide) - (_all DAUs disabled
      otherwise_).


#### REST API configuration (ACS)
The REST API configuration only affects the REST layer in ACS:
* **`restApi.directAccessUrl.enabled=false`**
    - Enables/disables DAU requests via the REST API.
* **`restApi.directAccessUrl.defaultExpiryTimeInSec=30`**
    - Sets the expiry time for all the DAU requested via a REST call (DAU REST API calls **cannot
      request** an explicit expiry time - unlike the service layer calls).
    - Its value cannot exceed the system-wide max expiry time configuration
      (`system.directAccessUrl.maxExpiryTimeInSec`), it can only be equal or lower (_REST API DAUs disabled
      otherwise_).
    - If not set, the default system-wide setting will be used
      (`system.directAccessUrl.defaultExpiryTimeInSec`).


#### Storage Connector Content Store (e.g. S3)
Each content store (i.e. "_final_" content store, one that provides actual storage, as opposed to a
_caching content store_), should have dedicated configuration options:
* **`connector.s3.directAccessUrl.enabled=false`**
    - Controls whether DAUs are enabled on this specific content store.
* **`connector.s3.directAccessUrl.defaultExpiryTimeInSec=30`**
    - Sets the expiry time for the DAU in this store, by overriding the global config. If this
      value exceeds the content store limit (described below) or the global limit it should
      fallback to the global configuration.
    - Its value cannot exceed the system-wide max expiry time configuration
      (`system.directAccessUrl.maxExpiryTimeInSec`), it can only be equal or lower (_DAUs for the specific content store disabled
      otherwise_).
    - If not set, the default system-wide setting will be used
      (`system.directAccessUrl.defaultExpiryTimeInSec`).
* **`connector.s3.directAccessUrl.maxExpiryTimeInSec=300`**
    - The maximum expiry time interval that can be requested by clients - content-store specific
      setting.
    - Its value cannot exceed the system-wide configuration
      (`system.directAccessUrl.maxExpiryTimeInSec`), it can only be equal or lower (_DAUs for the specific content store disabled
      otherwise_).
    - If not set, the default system-wide setting will be used
      (`system.directAccessUrl.maxExpiryTimeInSec`).

>**Note:** Callers within the platform (i.e. Java interfaces) can either request a specific
> expiry time or rely on the default.

>**Note:** When multiple S3 buckets are used for storage in Alfresco, each S3 Content Store can
> be configured with either the default (common) S3 Connector-specific properties (i.e.
> `connector.s3.directAccessUrl.enabled` & Co) OR new separate properties could be defined for
> each and every store (e.g. `connector.s3store1.directAccessUrl.enabled`,
> `connector.s3store2.directAccessUrl.enabled`, etc.).


#### Default Configuration
By default, Direct Access URLs are disabled. Meaning the following configuration properties
are **`false`**:
1. `system.directAccessUrl.enabled`
2. `restApi.directAccessUrl.enabled`
3. `connector.s3.directAccessUrl.enabled`


#### Configuration priorities
For Direct Access URLs to be usable on the service-layer, the feature must be enabled both
_system-wide_ and on the content-store(s). For the feature to be usable through REST
(outside the JVM) the _rest-api configuration_ must also be enabled.

The `system.directAccessUrl.enabled` property is the main switch of the feature. If this is
set to false *ALL* Direct Access URLs are disabled.

The next configuration that controls specific Direct Access URLs is the content store one. The
`connector.s3.directAccessUrl.enabled` property controls whether Direct Access URLs are
enabled for that specific store.

Whether or not a client can request a Direct Access URL by using a REST endpoint is controlled by
the `restApi.directAccessUrl.enabled` property. If the REST endpoint is disabled, but the
feature is enabled system-wide and on the content-store, then the direct access URLs will only
be usable by Java clients (only service-level requests will be possible).


### API
#### REST Endpoints (ACS)
The following endpoints can be used to obtain direct access URLs:

**Paths:**
* `/nodes/{nodeId}/request-direct-access-url`
* `/nodes/{nodeId}/renditions/{renditionId}/request-direct-access-url`
* `/nodes/{nodeId}/versions/{versionId}/request-direct-access-url`
* `/deleted-nodes/{nodeId}/request-direct-access-url`
* `/deleted-nodes/{nodeId}/renditions/{renditionId}/request-direct-access-url`
  
**Method:** **`POST`**
  
**Response:** Link to the resource wrapped in a JSON Object which also contains an attachment flag and the DAU expiration date.
  
**Error Codes:**
* If there’s no Direct Access URL provider (e.g. Alfresco S3 Connector extension) installed in
  Alfresco, or DAUs are not enabled, returns **501** HTTP Status Code.

  
**Parameters:**
* **`attachment`** - an optional flag which controls the download method (attachment URL vs
  embedded URL). Defaults to `true` when not specified, meaning the value of the
  [Content Disposition](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition)
  response header will be `attachment`.
* The `filename` part of the
  [Content Disposition](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition)
  header will be set in the service layer logic and can't be controlled by the DAU client.

#### Discovery API

The Discovery API provides status information about the Direct Access URLs feature
(enabled/disabled) via a new field:

`RepositoryInfo > StatusInfo > isDirectAccessUrlEnabled`.

The new field is **`true`** only when DAUs are enabled *system-wide* and DAUs are enabled on
the *REST API* and when if there is at least one ContentStore which supports and has DAUs enabled.

##### S3 Connector

The [AWS Java S3 SDK](https://docs.aws.amazon.com/AmazonS3/latest/dev/ShareObjectPreSignedURLJavaSDK.html)
is used to generate the pre-signed direct access URLs with the configured duration (see
Repository and Content Store expiry times configurations).

The pre-signed request generates a download for the remote content.

>**Known Limitations:** DAU generation on S3 depends on the security credentials used:
> https://docs.aws.amazon.com/AmazonS3/latest/dev/ShareObjectPreSignedURL.html

___


## Main flows

### Enabling Direct Access URLs
In order to be able to request a Direct Access URLs 3 configurations must be set to **`true`**:
1. `system.directAccessUrl.enabled` (system wide)
2. `restApi.directAccessUrl.enabled` (for enabling REST calls)
3. `connector.s3.directAccessUrl.enabled` (specific to a store)

See [direct-access-urls](https://github.com/Alfresco/alfresco-enterprise-repo/blob/master/docs/direct-access-urls.md) flows page for further details. 


#### Multiple Content Stores
When multiple content stores are configured in ACS, the DAU feature can be enabled/disabled
individually on each store (provided it supports DAUs).

#### Encrypted stores
Not supported.

### REST API flow
The REST API endpoint methods will always call the service layer with the default REST expiry
time (`restApi.directAccessUrl.defaultExpiryTimeInSec`). The service layer will decide
whether the expiry time must be reduced (for instance when the content store max expiry time is
lower).

___

## Security threats and controls
Once generated, the pre-signed URLs can be used by anyone that obtains them (either within or
outside Alfresco).

The Direct Access URLs have an expiry time, but they can’t be invalidated prior to that expiry
time.

The expiration date is restricted through Alfresco configurations (global & content-store
specific properties).

Once started, a long-running download will continue even though the Direct URL might expire
before the download finishes.

If Alfresco Repository uses (is configured with) multiple content stores, then the DAU feature
can be enabled on only one (or a subset) of those content stores.
___


## Performance and scalability
Each call to one of the new `.../request-direct-access-url` Alfresco REST endpoints results in the
creation of a new (and separate) pre-signed URL for an AWS S3 object.

The generation of a pre-signed URL is a purely AWS SDK **client-side operation** - meaning the
URL is generated (&signed) locally in the Alfresco JVM, without any communication with AWS
(no I/O).

The Direct Access URL generation is a fairly simple and quick operation, not particularly
computation-intensive (though it does involve a small cryptographic operation - the generated
URLs are signed with the AWS credentials).

It’s best if client applications request a DAU right before the actual download operation, or
only after the intention of using the DAU is certain.
Client applications should avoid generating DAUs for Alfresco content proactively, when the
actual file download is unlikely to happen - especially if a large set of files is involved.

___