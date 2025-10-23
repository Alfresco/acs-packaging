#!/usr/bin/env bash

if [ -n "$JAVA_RUNTIME_VERSION" ]; then
  declare -A JAVA_IMAGE_MAP=(
    [21]="alfresco/alfresco-base-java:jre21-rockylinux9@sha256:792079aa36a7a1076e7d48dd800b5de5ffc1cd48e8460fd7e56f7107d375c0cc"
    [25]="alfresco/alfresco-base-java:jre25-rockylinux9@sha256:2e8664a6148157e0fd8c7861120c31768f510b5baaac86fd90c8b58aef5941eb"
  )
  BASE_IMAGE="${JAVA_IMAGE_MAP[$JAVA_RUNTIME_VERSION]}"
  export BASE_IMAGE
fi
