### About
We are using  Open LDAP image created on this [osixia/docker-openldap](https://github.com/osixia/docker-openldap) github repository.

They have a comprehensive [README.md](https://github.com/osixia/docker-openldap/blob/stable/README.md) file with steps on how to use this image, how to configure it, extent it, etc.

Each version of `osixia/openldap` has an appropriate version of Open LDAP.

Currently (5 April 2019) we are using the following matrix:

| osixia/openldap (tag)  | OpenLDAP (version) |
| ------------- | ------------- |
| 1.1.5  | 2.4.40  |


### How to upgrade in the future to a new version of Open LDAP 

* checkout the releases done on [osixia/docker-openldap](https://github.com/osixia/docker-openldap) github repository

* at this point (5 April 2019) the last version of `osixia/openldap:1.2.4` is using Open LDAP v2.4.47

|osixia/openldap (tag)  | OpenLDAP (version) |
| ------------- | ------------- |
| 1.2.4  | 2.4.47  |

* to use this newest version(1.2.4), just update the FROM clause on our [Dockerfile](./Dockerfile#L1) to new version `FROM osixia/openldap:1.2.4`

* update the [.env](../.env) on `AUTHENTICATION_TAG=2.4.47` because we will build a new `quay.io/alfresco/openldap` with version `2.4.47`
* build the stack
* run the stack

### Testing
LDAP Login Details

|username  | password |
| ------------- | ------------- |
| user1  | user1  |
| gica  | gica  |

>the `user1` was added in the [importData.ldif](./importData.ldif#L100)
> the `user1` password has been MD5 generated as `{MD5}JMnhXlKvxHwiW3V+e+4fnQ==` and saved in the same file