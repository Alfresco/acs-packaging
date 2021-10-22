#!/usr/bin/env bash
set -o errexit

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="${SCRIPT_DIR}/../../.."

source "$(dirname "${BASH_SOURCE[0]}")/dev_functions.sh"

usage() {
    echo "Reverts changes made by linkPoms.sh using values stored in .pomLink.env" 1>&2;
    echo 1>&2;
    echo "Usage: $0 [-h]" 1>&2;
    echo "  -h: Display this help" 1>&2;
    exit 1;
}

while getopts "lh" arg; do
    case $arg in
        l)
            LOGGING_OUT=`tty`
            ;;
        h | *)
            usage
            exit 0
            ;;
    esac
done

source .pomLink.env

updatePomParent   alfresco-enterprise-repo  "$ENT_R_PARENT"
updatePomProperty alfresco-enterprise-repo  "$ENT_R_DEP_COM_R" dependency.alfresco-community-repo.version

updatePomProperty alfresco-enterprise-share "$ENT_S_DEP_COM_R" dependency.alfresco-community-repo.version
updatePomProperty alfresco-enterprise-share "$ENT_S_DEP_ENT_R" dependency.alfresco-enterprise-repo.version

updatePomParent   acs-packaging             "$ENT_P_DEP_ENT_R"
updatePomProperty acs-packaging             "$ENT_P_DEP_ENT_R" dependency.alfresco-enterprise-repo.version
updatePomProperty acs-packaging             "$ENT_P_DEP_ENT_S" dependency.alfresco-enterprise-share.version

updatePomParent   acs-community-packaging   "$COM_P_PARENT"
updatePomProperty acs-community-packaging   "$COM_P_DEP_COM_R" dependency.alfresco-community-repo.version
updatePomProperty acs-community-packaging   "$COM_P_DEP_COM_S" dependency.alfresco-community-share.version

