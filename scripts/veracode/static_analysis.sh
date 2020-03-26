#!/usr/bin/env bash
set -e
echo "=========================== Starting Static Analysis Script ==========================="

java -jar vosp-api-wrappers-java-$VERACODE_WRAPPER_VERSION.jar -vid $VERACODE_API_ID -vkey $VERACODE_API_KEY \
          -action uploadandscan -appname "ACS Repository" -sandboxname "ACS Repo Team Sandbox" -createprofile false \
           -filepath war/target/alfresco.war -version "$TRAVIS_JOB_ID - $TRAVIS_JOB_NUMBER"


echo "=========================== Finishing Static Analysis Script =========================="