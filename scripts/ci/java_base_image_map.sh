#!/usr/bin/env bash

if [ -n "$JAVA_RUNTIME_VERSION" ]; then
  declare -A JAVA_IMAGE_MAP=(
    [21]="alfresco/alfresco-base-java:jre21-rockylinux9@sha256:a5bc7626cc86656f88b7f9a84926a407a6bdb34ffef4c26d323f6b047b85258a"
    [25]="alfresco/alfresco-base-java:jre25-rockylinux9@sha256:819765a4b9512a37ce9e1a3d356ed55e260eb308592c48a5894118ed2ed46375"
  )
  BASE_IMAGE="${JAVA_IMAGE_MAP[$JAVA_RUNTIME_VERSION]}"
  export BASE_IMAGE
fi
