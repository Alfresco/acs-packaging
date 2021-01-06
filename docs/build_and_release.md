# Build
The `acs-packaging` project uses _Travis CI_. \
The `.travis.yml` config file can be found in the root of the repository.


## Stages
Although a little unusual, builds branches other than `master` and `release/...` are preceded by
cloning and building branches with the same name in upstream projects: `alfresco-community-repo` and
`alfresco-enterprise-repo`. This is done so that development can take place in parallel between projects
without having to do development releases to link them together. In fact, you don't even have to wait for
the upstream Travis build to complete. You just need to make sure the parent poms reference
each other and the `dependency.alfresco-community-repo.version` and
`dependency.alfresco-enterprise-repo.version` in the pom.xml files use the same SNAPSHOT values.

1. **test**: Java build with unit tests, integration tests and WhiteSource scan.
2. **docker_latest**: creates `latest` tags of `alfresco/alfresco-content-repository-aws`
   and `alfresco/alfresco-content-repository`.
   Also creates `${acs.version}-${build-number}` tags (e.g. `7.0.0-9876`) for the single pipeline images.
3. **release**: On release and master branches, where the commit message includes `[release]`, this stage
   creates `${project.version}` tags (e.g. `7.0.0`, `7.0.0-A20`) of `alfresco/alfresco-content-repository-aws`
   and `alfresco/alfresco-content-repository`. These are in addition to the tags created in the *docker-latest* stage.
3. **publish**: Artifact deployment to AWS Release bucket for access by customers.
