Direct Access URLs
==================

## Purpose
The main purpose of the _Direct Access URLs_ is to accelerate the local download of content.

___


## Overview
Most Cloud Storage Providers allow generating publicly accessible URLs for sharing access to objects although the way it is provided and named differs throughout platforms.

AWS S3 provides a way of generating
[pre-signed URLs](https://docs.aws.amazon.com/AmazonS3/latest/dev/ShareObjectPreSignedURL.html)
for sharing objects.
Azure Blob Storage provide [Shared Access Signature (SAS) tokens](https://docs.microsoft.com/en-us/rest/api/storageservices/delegate-access-with-shared-access-signature) which as a part of object's URL can serve very similar role.

Above described features enable the implementation of direct access to our content (e.g. shortcutting the Shared File Store for transformations or
faster/direct access in the context of ADW to documents).

>:warning: **Note:** The AWS S3 pre-signed URLs are temporary links with an expiration time. Same applies to Azure SAS tokens which have expiration time.

The repository infrastructure supports direct access urls since ACS 7.1.0. This includes the ContentService
and the ContentStore interface for which default methods have been provided so that ContentStore implementations that
implement the old version of this interface will throw a Not Supported exception.
The new methods are auditable using the node reference and URL expiry time (in seconds) as the auditing parameters.

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


#### Storage Connector Content Store (e.g. S3 or Azure Connector)
Each content store (i.e. "_final_" content store, one that provides actual storage, as opposed to a
_caching content store_), should have dedicated configuration options:
* **`connector.{$connectorName}.directAccessUrl.enabled=false`**
    - Controls whether DAUs are enabled on this specific content store.
* **`connector.{$connectorName}.directAccessUrl.defaultExpiryTimeInSec=30`**
    - Sets the expiry time for the DAU in this store, by overriding the global config. If this
      value exceeds the content store limit (described below) or the global limit it should
      fallback to the global configuration.
    - Its value cannot exceed the system-wide max expiry time configuration
      (`system.directAccessUrl.maxExpiryTimeInSec`), it can only be equal or lower (_DAUs for the specific content store disabled
      otherwise_).
    - If not set, the default system-wide setting will be used
      (`system.directAccessUrl.defaultExpiryTimeInSec`).
* **`connector.{$connectorName}.directAccessUrl.maxExpiryTimeInSec=300`**
    - The maximum expiry time interval that can be requested by clients - content-store specific
      setting.
    - Its value cannot exceed the system-wide configuration
      (`system.directAccessUrl.maxExpiryTimeInSec`), it can only be equal or lower (_DAUs for the specific content store disabled
      otherwise_).
    - If not set, the default system-wide setting will be used
      (`system.directAccessUrl.maxExpiryTimeInSec`).

>**Note:** `{$connectorName}` depends on Alfresco Cloud Connector. For S3 Connector it is 's3', for Azure Connector it is 'az'.

>**Note:** Callers within the platform (i.e. Java interfaces) can either request a specific
> expiry time or rely on the default (see above described properties for proposed default values).

>**Note:** When multiple S3 buckets are used for storage in Alfresco, each S3 Content Store can
> be configured with either the default (common) S3 Connector-specific properties (i.e.
> `connector.s3.directAccessUrl.enabled` & common/default settings) OR new separate properties could be defined for
> each and every store (e.g. `connector.s3.store1.directAccessUrl.enabled`,
> `connector.s3.store2.directAccessUrl.enabled`, etc.).
>
> For multiple Azure blob containers, each of them can also be configured similarly to S3 buckets (i.e.
> `connector.az.directAccessUrl.enabled` & common/default settings) OR new separate properties could be defined for
> each and every store/container (e.g. `connector.az.store1.directAccessUrl.enabled`,
> `connector.az.store2.directAccessUrl.enabled`, etc.).


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
* `/nodes/{nodeId}/versions/{versionId}/renditions/{renditionId}/request-direct-access-url`
* `/deleted-nodes/{nodeId}/request-direct-access-url`
* `/deleted-nodes/{nodeId}/renditions/{renditionId}/request-direct-access-url`

**Method:** **`POST`**

**Response:** Link to the resource wrapped in a JSON Object which also contains an attachment flag and the DAU expiration date.
Sample response for S3 Connector:
```json
{
  "entry": {
    "contentUrl": "https://<bucket_name>.s3.<region_name>.amazonaws.com/<binary_name>.bin?response-content-disposition=attachment%3B%20filename%20%3D%22graph.JPG%22&response-content-type=image%2Fjpeg&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEMv%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMiJGMEQCIDmc%2Fb1e55l4sQjKGG3%2Fr1CU0gtzOOqFnr0Q%2BuXoNa%2BXAiB5oSPGJI1%2FZORobOtV%2BUmiim6GMQJxoKT9I%2Fn6t9ANvir6AwgUEAMaDDE3NTEyNTQyOTQ0MiIMA1qC5mzeuQyHnfd%2BKtcDgAHmPq1MEq5lrb2ggn7Ev%2FSJ%2FQgMVB33Y7NyfsD4BTB3Cn7e1uH17uIH8SkHX6tA9cjBOKx6Sym3gzzP2kTdKSPimQ1UOXMw4uhtaI0f%2FkqnI%2BhMh6GZXT6lOfqDE%2Fkz9nM3QuBxaNI2b8Nb71lP0KPmq7bzBagJOIccf2%2BK3VW3en5gS%2FVAoU2Wx8j1HEQJuk%2FS1whspl970hPFXKIFGIbedO5H8P66wOYdb9LKiHVxvNK7cAJfrVT6jnmqf1L6GyRJa01xgOqgUw1LvsqGsf8kkw%2FkWwJz25StcmJLtpLcWsmZ0x8aHmDNi8SHixteB5XXKJ9Bv8Ex0iIMH3%2Bs8uWmBFssu9il6u8GyV%2FlaIhKYcZLLpIFSTtVudWe60UpQhFPqyHZ6gqqi4e%2BZZfGqqhUNbZucqMvc31V76NbvwdHxI%2F0H0I8fVqCtIatO655qtq6sy%2B29qYymE7RLI9Vnrotkz%2FJafHt4LDIOjX3aDcHS0%2FTxr4QmyJbh%2B%2F0JKsSlqyoosUgzi0mqzw0B8zsTlrkfR9dPkQTNntxZoARaddEIA4Q8QRryQLFe8FITeHSFhUpdPXei3ZEmguSUpkqUQroUdQm8W3C2aoV%2F0A%2BS80IaffqNUY6MPawjpAGOqYBSMI0t5Xt7oW8QqGQrDSMllhX18T0UoxNEvYBii6vFzjuKKasQV5WaGtOMhcg8B5Ee7AxXTCl06FSPhmrQ3f%2FtFTqYtbd8FR8QTK0ZJekBMoM5thzFJ4EztnCYrkAnDo1oDUDOuBQxVho8w5llTEaKLo1SgomysnvpRFshJdBl%2BKXuFVM6Q2tmqSCY%2Bmm%2BVVte%2Bt8Yc4Ulg5eZpkkt3g2HOBaI0cnOw%3D%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20220209T115428Z&X-Amz-SignedHeaders=host&X-Amz-Expires=30&X-Amz-Credential=ASIASRRSJ7TBNPZVGWOY%2F20220209%2Fus-east-2%2Fs3%2Faws4_request&X-Amz-Signature=6b240b52024eca8a07e47dfad6970f84a75de049a1ae7af5855ed8c655f76cda",
    "attachment": true,
    "expiryTime": "2022-02-09T11:54:58.700+0000"
  }
}
```
Sample response for Azure Connector:
```json
{
    "entry": {
        "contentUrl": "https://<storage_account_name>.blob.core.windows.net/<container_name>/<blob_name>?sv=2020-10-02&spr=https&se=2022-02-09T04%3A09%3A40Z&sr=b&sp=r&sig=LkznZiG6u2BUDprdKyk0Hm9XkURG%2BZZp0qy0Ls3kgVY%3D&rscd=attachment%3B%20filename%20%3D%22graph.JPG%22&rsct=image%2Fjpeg",
        "attachment": true,
        "expiryTime": "2022-02-09T04:09:40.638+0000"
    }
}
```

**Error Codes:**
* If there’s no Direct Access URL provider (e.g. Alfresco S3 Connector or Azure Connector module) installed in
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


##### Azure Connector

Several Azure Java SDK objects (see [BlobSasPermission](https://docs.microsoft.com/en-us/java/api/com.azure.storage.blob.sas.blobsaspermission?view=azure-java-stable) [BlobServiceSasSignatureValues](https://docs.microsoft.com/en-us/java/api/com.azure.storage.blob.sas.blobservicesassignaturevalues?view=azure-java-stable)) ,  are used to generate SAS (Shared Access Signature) which is the used to generate direct access URLs with the configured duration (see
Repository and Content Store expiry times configurations).

The pre-signed request generates a download for the remote content.

>**Known Limitations:** SAS generation on Azure Blob depends on the authorization type used (only valid for Azure AD or shared key authorization), see:
> https://docs.microsoft.com/en-us/rest/api/storageservices/create-user-delegation-sas

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
creation of a new (and separate) pre-signed URL for an AWS S3 / Azure Blob object.

The generation of a pre-signed URL is a purely AWS/Azure SDK **client-side operation** - meaning the
URL is generated (&signed) locally in the Alfresco JVM, without any communication with AWS/Azure
(no I/O).

The Direct Access URL generation is a fairly simple and quick operation, not particularly
computation-intensive (though it does involve a small cryptographic operation - the generated
URLs are signed with the AWS/Azure credentials).

It’s best if client applications request a DAU right before the actual download operation, or
only after the intention of using the DAU is certain.
Client applications should avoid generating DAUs for Alfresco content proactively, when the
actual file download is unlikely to happen - especially if a large set of files is involved.

___
