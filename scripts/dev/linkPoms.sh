#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="${SCRIPT_DIR}/../../.."

source "$(dirname "${BASH_SOURCE[0]}")/dev_functions.sh"

usage() {
    echo "Updates the downstream projects with the versions of the upstream projects. Reversed by unlinkPoms.sh" 1>&2;
    echo 1>&2;
    echo "Usage: $0 [-b <branch>] [-mpxuh]" 1>&2;
    echo "  -m: Checkout master of each project" 1>&2;
    echo "  -b: Checkout the <branch> of each project or master if <branch> is blank" 1>&2;
    echo "  -p: Pull the latest version of each project" 1>&2;
    echo "  -x: Skip the extract of values from each project" 1>&2;
    echo "  -u: Skip the update of values in each project" 1>&2;
    echo "  -h: Display this help" 1>&2;
    exit 1;
}

function checkout() {
    local PROJECT="${1}"
    local BRANCH="${2}"

    if [ -d "${ROOT_DIR}/${PROJECT}" ]
    then
      pushd "${ROOT_DIR}/${PROJECT}" &>/dev/null
      git checkout "${BRANCH}" &>/tmp/$$.log
      if [ $? -ne 0 ]
      then
        echo
        echo "\"git checkout ${BRANCH}\" failed on ${PROJECT}"
        cat "/tmp/$$.log"
        exit 1
      fi
      echo "${PROJECT} is now on ${BRANCH}"
      popd &>/dev/null
    fi
}

function pull_latest() {
    local PROJECT="${1}"

    if [ -d "${ROOT_DIR}/${PROJECT}" ]
    then
      pushd "${ROOT_DIR}/${PROJECT}" &>/dev/null
      git pull &>/tmp/$$.log
      if [ $? -ne 0 ]
      then
        echo
        echo "\"git pull\" failed on ${PROJECT}"
        cat "/tmp/$$.log"
        exit 1
      fi
      echo "${PROJECT} is now using latest"
      popd &>/dev/null
    fi
}

function exportPomProperty() {
    local PROJECT="$1"
    local ENV_NAME="$2"
    local PROPERTY_NAME="$3"

    if [ -d "${ROOT_DIR}/${PROJECT}" ]
      then
      pushd "${ROOT_DIR}/${PROJECT}" &>/dev/null
      PROPERTY_VALUE=$(mvn help:evaluate -Dexpression="${PROPERTY_NAME}" -q -DforceStdout)
      echo "export ${ENV_NAME}=${PROPERTY_VALUE}"
      popd &>/dev/null
      >&2 echo "${ENV_NAME}=${PROPERTY_VALUE}"
    fi
}

while getopts "b:mpxuh" arg; do
    case $arg in
        b)
            B_FLAG_SET="true"
            ;;
        m)
            M_FLAG_B_FLAG_SET="true"
            ;;
        p)
            # git pull after git checkout
            ;;
        x)
            SKIP_EXPORT="true"
            ;;
        u)
            SKIP_UPDATE="true"
            ;;
        h | *)
            usage
            exit 0
            ;;
    esac
done
if [ -n "${B_FLAG_SET}" -a -n "${M_FLAG_B_FLAG_SET}" ]
then
  echo "-m and -b may not both be set"
  exit 1
fi
OPTIND=1
while getopts "b:mpxuh" arg; do
    case $arg in
        b)
            BRANCH="${OPTARG:-master}"
            checkout alfresco-community-repo   "${BRANCH}"
            checkout alfresco-enterprise-repo  "${BRANCH}"
            checkout alfresco-enterprise-share "${BRANCH}"
            checkout acs-packaging             "${BRANCH}"
            checkout acs-community-packaging   "${BRANCH}"
            ;;
        m)
            BRANCH="master"
            checkout alfresco-community-repo   "${BRANCH}"
            checkout alfresco-enterprise-repo  "${BRANCH}"
            checkout alfresco-enterprise-share "${BRANCH}"
            checkout acs-packaging             "${BRANCH}"
            checkout acs-community-packaging   "${BRANCH}"
            ;;
    esac
done
OPTIND=1
while getopts "b:mpxuh" arg; do
    case $arg in
        p)
            pull_latest alfresco-community-repo
            pull_latest alfresco-enterprise-repo
            pull_latest alfresco-enterprise-share
            pull_latest acs-packaging
            pull_latest acs-community-packaging
            ;;
    esac
done

if [ -z ${SKIP_EXPORT+x} ]
then
  exportPomProperty alfresco-community-repo   COM_R_VERSION   project.version                               > .pomLink.env

  exportPomProperty alfresco-enterprise-repo  ENT_R_VERSION   project.version                              >> .pomLink.env
  exportPomProperty alfresco-enterprise-repo  ENT_R_PARENT    project.parent.version                       >> .pomLink.env
  exportPomProperty alfresco-enterprise-repo  ENT_R_DEP_COM_R dependency.alfresco-community-repo.version   >> .pomLink.env

  exportPomProperty alfresco-enterprise-share ENT_S_VERSION   project.version                              >> .pomLink.env
  exportPomProperty alfresco-enterprise-share ENT_S_DEP_COM_R dependency.alfresco-community-repo.version   >> .pomLink.env
  exportPomProperty alfresco-enterprise-share ENT_S_DEP_ENT_R dependency.alfresco-enterprise-repo.version  >> .pomLink.env

  exportPomProperty acs-packaging             ENT_P_PARENT    project.parent.version                       >> .pomLink.env
  exportPomProperty acs-packaging             ENT_P_DEP_ENT_R dependency.alfresco-enterprise-repo.version  >> .pomLink.env
  exportPomProperty acs-packaging             ENT_P_DEP_ENT_S dependency.alfresco-enterprise-share.version >> .pomLink.env

  exportPomProperty acs-community-packaging   COM_P_PARENT    project.parent.version                       >> .pomLink.env
  exportPomProperty acs-community-packaging   COM_P_DEP_COM_R dependency.alfresco-community-repo.version   >> .pomLink.env
  exportPomProperty acs-community-packaging   COM_P_DEP_COM_S dependency.alfresco-community-share.version  >> .pomLink.env
  echo
fi

if [ -z ${SKIP_UPDATE+x} ]
then
  source .pomLink.env

  updatePomParent   alfresco-enterprise-repo  "$COM_R_VERSION"
  updatePomProperty alfresco-enterprise-repo  "$COM_R_VERSION" dependency.alfresco-community-repo.version

  updatePomProperty alfresco-enterprise-share "$COM_R_VERSION" dependency.alfresco-community-repo.version
  updatePomProperty alfresco-enterprise-share "$ENT_R_VERSION" dependency.alfresco-enterprise-repo.version

  updatePomParent   acs-packaging             "$ENT_R_VERSION"
  updatePomProperty acs-packaging             "$ENT_R_VERSION" dependency.alfresco-enterprise-repo.version
  updatePomProperty acs-packaging             "$ENT_S_VERSION" dependency.alfresco-enterprise-share.version

  updatePomParent   acs-community-packaging   "$COM_R_VERSION"
  updatePomProperty acs-community-packaging   "$COM_R_VERSION" dependency.alfresco-community-repo.version
  updatePomProperty acs-community-packaging   "$ENT_S_VERSION" dependency.alfresco-community-share.version
fi
