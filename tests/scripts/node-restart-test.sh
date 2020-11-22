#!/usr/bin/env bash

export ALFRESCO_HOST=$1
export ALFRESCO_PORT=$2

# Add more checks if required

# validate the custom workflow is deployed

if curl --silent -u admin:admin -X GET http://${ALFRESCO_HOST}:${ALFRESCO_PORT}/alfresco/api/-default-/public/workflow/versions/1/deployments 2>&1 | grep "rfc.bpmn20.xml"
then
   echo "The workflow \"rfc.bpmn20.xml\" was deployed successfully."
   exit 0
else
   echo "The workflow \"rfc.bpmn20.xml\" was not deployed."
   exit 1
fi