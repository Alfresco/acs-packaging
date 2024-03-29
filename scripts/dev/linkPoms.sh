#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="${SCRIPT_DIR}/../../.."
ENV_FILE=".linkPoms.env"
ENV_PATH="${ROOT_DIR}/${ENV_FILE}"
PROJECT_LIST="alfresco-community-repo alfresco-enterprise-repo alfresco-enterprise-share acs-packaging acs-community-packaging"

source "$(dirname "${BASH_SOURCE[0]}")/dev_functions.sh"

usage() {
    echo "Updates the downstream projects with the versions of the upstream projects. Reversed by unlinkPoms.sh" 1>&2;
    echo 1>&2;
    echo "Usage: $0 [-b <branch>] [-mpfxuh]" 1>&2;
    echo "  -m: Checkout master of each project" 1>&2;
    echo "  -b: Checkout the <branch> of each project or master if <branch> is blank" 1>&2;
    echo "  -f: Fetch before checking out the branch" 1>&2;
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

function update_repository() {
    local UPDATE_COMMAND="${1}"
    local PROJECT="${2}"

    if [ -d "${ROOT_DIR}/${PROJECT}" ]
    then
      pushd "${ROOT_DIR}/${PROJECT}" &>/dev/null
      git "${UPDATE_COMMAND}" &>/tmp/$$.log
      if [ $? -ne 0 ]
      then
        echo
        echo "\"git ${UPDATE_COMMAND}\" failed on ${PROJECT}"
        cat "/tmp/$$.log"
        exit 1
      fi
      echo "${PROJECT} has been updated using \"git ${UPDATE_COMMAND}\""
      popd &>/dev/null
    fi
}

function readTopLevelTag() {
  local TAG_NAME="${1}"
  local POM_FILE="${2}"
  # Might be possible to generalise this function to accept an XPath so it could be used in place of sed commands

  # Read the file with an IFS (Input Field Separator) of the start of XML tag character <
  local IFS=\>
  local DEPTH=-99
  while read -d \< ENTITY CONTENT
  do
    if [[ $ENTITY == project\ * ]] # outer <project> tag
    then
      DEPTH=0
    elif [[ $ENTITY == /* ]] # end tag
    then
      ((DEPTH=DEPTH-1))
    else                     # start tag
      ((DEPTH=DEPTH+1))
    fi

    if [[ $ENTITY = "${TAG_NAME}" ]] && [[ $DEPTH == 1 ]] ; then
        echo $CONTENT
        exit
    fi
  done < $POM_FILE
  exit 1
}

function exportPomVersion() {
  local PROJECT="${1}"
  local ENV_NAME="${2}"

  if [ -d "${ROOT_DIR}/${PROJECT}" ]
    then
    pushd "${ROOT_DIR}/${PROJECT}" &>/dev/null
    # Same as slower/simpler: "mvn help:evaluate -Dexpression=project.version"
    VERSION=$(readTopLevelTag version pom.xml)
    if [ $? -ne 0 ]
    then
      echo
      echo "\"readTopLevelTagContent version pom.xml\" failed on ${PROJECT}"
      exit 1
    fi
    echo "export ${ENV_NAME}=${VERSION}" >> "${ENV_PATH}"
    popd &>/dev/null
  fi
}

function exportPomParent() {
  local PROJECT="${1}"
  local ENV_NAME="${2}"

  if [ -d "${ROOT_DIR}/${PROJECT}" ]
    then
    pushd "${ROOT_DIR}/${PROJECT}" &>/dev/null
    # Same as slower/simpler: "mvn help:evaluate -Dexpression=project.parent.version"
    PROPERTY_VALUE=$(sed -n '/<parent>/,/<\/parent>/p' pom.xml | sed -n "s/.*<version>\(.*\)<\/version>/\1/p" | sed 's/\r//g')
    if [ $? -ne 0 ]
    then
      echo
      echo "\"sed -n '/<parent>/,/<\/parent>/p' pom.xml | sed -n \\\"s/.*<version>\(.*\)<\/version>/\1/p\\\" | sed 's/\r//g'\" failed on ${PROJECT}"
      exit 1
    fi
    echo "export ${ENV_NAME}=${PROPERTY_VALUE}" >> "${ENV_PATH}"
    popd &>/dev/null
  fi
}

# Original version was simpler/slower: exportPomPropertyOrig <project> <env_name> project.parent.version
function exportPomProperty() {
    local PROJECT="${1}"
    local ENV_NAME="${2}"
    local PROPERTY_NAME="${3}"

  if [ -d "${ROOT_DIR}/${PROJECT}" ]
    then
    pushd "${ROOT_DIR}/${PROJECT}" &>/dev/null
    # Same as slower/simpler: "mvn help:evaluate -Dexpression=${PROPERTY_NAME}"
    PROPERTY_VALUE=$(sed -n '/<properties>/,/<\/properties>/p' pom.xml | sed -n "s/.*<${PROPERTY_NAME}>\(.*\)<\/${PROPERTY_NAME}>/\1/p" | sed 's/\r//g')
    if [ $? -ne 0 ]
    then
      echo
      echo "\"sed -n '/<properties>/,/<\/properties>/p' pom.xml | sed -n \\\"s/.*<${PROPERTY_NAME}>\(.*\)<\/${PROPERTY_NAME}>/\1/p\\\" | sed 's/\r//g'\" failed on ${PROJECT}"
      exit 1
    fi
    echo "export ${ENV_NAME}=${PROPERTY_VALUE}" >> "${ENV_PATH}"
    popd &>/dev/null
  fi
}

while getopts "b:mpfxuh" arg; do
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
        f)
            for project in ${PROJECT_LIST}
            do
                update_repository fetch "${project}"
            done
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
while getopts "b:mpfxuh" arg; do
    case $arg in
        b)
            BRANCH="${OPTARG:-master}"
            ;;
        m)
            BRANCH="master"
            ;;
    esac
done
if [ -n "${BRANCH}" ]
then
    for project in ${PROJECT_LIST}
    do
        checkout "${project}" "${BRANCH}"
    done
fi
OPTIND=1
while getopts "b:mpfxuh" arg; do
    case $arg in
        p)
            for project in ${PROJECT_LIST}
            do
                update_repository pull "${project}"
            done
            ;;
    esac
done

if [ -z ${SKIP_EXPORT+x} ]
then
  rm -f "${ENV_FILE}"

  exportPomVersion  alfresco-community-repo   COM_R_VERSION

  exportPomVersion  alfresco-enterprise-repo  ENT_R_VERSION
  exportPomParent   alfresco-enterprise-repo  ENT_R_PARENT
  exportPomProperty alfresco-enterprise-repo  ENT_R_DEP_COM_R dependency.alfresco-community-repo.version

  exportPomVersion  alfresco-enterprise-share ENT_S_VERSION
  exportPomProperty alfresco-enterprise-share ENT_S_DEP_COM_R dependency.alfresco-community-repo.version
  exportPomProperty alfresco-enterprise-share ENT_S_DEP_ENT_R dependency.alfresco-enterprise-repo.version

  exportPomVersion  acs-packaging             ENT_P_VERSION
  exportPomParent   acs-packaging             ENT_P_PARENT
  exportPomProperty acs-packaging             ENT_P_DEP_ENT_R dependency.alfresco-enterprise-repo.version
  exportPomProperty acs-packaging             ENT_P_DEP_ENT_S dependency.alfresco-enterprise-share.version

  exportPomVersion  acs-packaging             COM_P_VERSION
  exportPomParent   acs-community-packaging   COM_P_PARENT
  exportPomProperty acs-community-packaging   COM_P_DEP_COM_R dependency.alfresco-community-repo.version
  exportPomProperty acs-community-packaging   COM_P_DEP_COM_S dependency.alfresco-community-share.version

  cat "${ENV_FILE}"
fi

if [ -z ${SKIP_UPDATE+x} ]
then
  if [ ! -f "${ENV_FILE}" ]
  then
    echo ""${ENV_FILE}" does not exist."
    exit 1
  fi

  source "${ENV_FILE}"

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
