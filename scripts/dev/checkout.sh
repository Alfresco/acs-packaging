#!/usr/bin/env bash
set -o errexit

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

ROOT_DIR="${SCRIPT_DIR}/../../.."

usage() {
    echo "Checkout the code for a particular version of acs-packaging." 1>&2;
    echo "Usage: $0 [-v <version>] [-l] [-h]" 1>&2;
    echo "  -v <version>: The version of acs-packaging (defaults to the current checked out version)" 1>&2;
    echo "  -l: Output extra logging" 1>&2;
    echo "  -h: Display this help" 1>&2;
    exit 1;
}

ACS_VERSION=""
LOGGING_OUT="/dev/null"
while getopts ":hv:l" arg; do
    case $arg in
        v)
            ACS_VERSION=${OPTARG}
            ;;
        l)
            LOGGING_OUT=`tty`
            ;;
        h | *)
            usage
            exit 0
            ;;
    esac
done

function checkout() {
    target=$1
    version=$2

    cd "${ROOT_DIR}/${target}/"
    git fetch &>${LOGGING_OUT}
    git checkout "${version}" &>${LOGGING_OUT}
    echo "${target} is now at ${version}"
}

function checkout_from_project() {
    target=$1
    source=$2
    property=$3

    # Get the target version from the property in the specified pom file.
    version=`grep ${property} ${ROOT_DIR}/${source}/pom.xml | sed "s|^.*<[^>]*>\([^<]*\)</[^>]*>.*$|\1|g"`
    if [[ "${version}" != "" ]]
    then
        checkout "${target}" "${version}"
    else
        echo "WARNING: Could not find version for ${target}."
    fi
}


if [[ "#${ACS_VERSION}" != "#" ]]
then
    checkout "acs-packaging" "${ACS_VERSION}"
fi

checkout_from_project "alfresco-enterprise-repo" "acs-packaging" "<dependency.alfresco-enterprise-repo.version>"
checkout_from_project "alfresco-community-repo" "alfresco-enterprise-repo" "<dependency.alfresco-community-repo.version>"
checkout_from_project "alfresco-enterprise-share" "acs-packaging" "<dependency.alfresco-enterprise-share.version>"
