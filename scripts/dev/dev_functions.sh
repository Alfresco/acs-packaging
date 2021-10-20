#!/usr/bin/env bash

function updatePomParent() {
    local PROJECT="$1"
    local PARENT_POM_VERSION="$2"

    # Cannot use "mvn versions:update-parent" as the version must exist before it can be set. The ed command is also faster.

    pushd "${ROOT_DIR}/${PROJECT}" &>${LOGGING_OUT}
    >&2 echo "${PROJECT} set parent=${PARENT_POM_VERSION}"
    ed -s pom.xml &>${LOGGING_OUT} << EOF
/<parent>
/<version>.*<\/version>/s//<version>${PARENT_POM_VERSION}<\/version>/
wq
EOF
    git status &>${LOGGING_OUT}
    git --no-pager diff pom.xml &>${LOGGING_OUT}
    popd &>${LOGGING_OUT}
}

function updatePomProperty() {
    local PROJECT="$1"
    local PROPERTY_VALUE="$2"
    local PROPERTY_NAME="$3"

    # Can use "mvn versions:set-property", but ed is so much faster.
    # mvn -B versions:set-property  versions:commit  -Dproperty="${PROPERTY_NAME}" "-DnewVersion=${PROPERTY_VALUE}" &>${LOGGING_OUT}

    pushd "${ROOT_DIR}/${PROJECT}" &>${LOGGING_OUT}
    >&2 echo "${PROJECT} set ${PROPERTY_NAME}=${PROPERTY_VALUE}"
    ed -s pom.xml &>${LOGGING_OUT} << EOF
/\(<${PROPERTY_NAME}>\).*\(<\/${PROPERTY_NAME}>\)/s//\1${PROPERTY_VALUE}\2/
wq
EOF
    git status &>${LOGGING_OUT}
    git --no-pager diff pom.xml &>${LOGGING_OUT}
    popd &>${LOGGING_OUT}
}
