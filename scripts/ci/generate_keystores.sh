#! /bin/bash
#! /bin/bash

# SETTINGS
# Alfresco Format: "classic" / "current" is supported only from 7.0
ALFRESCO_FORMAT=current

#Contains directory settings
source ${CI_WORKSPACE}/alfresco-ssl-generator/ssl-tool/utils.sh

# Cleanup previous output of script
rm -rd $CA_DIR
rm -rd $KEYSTORES_DIR
rm -rd $CERTIFICATES_DIR

# SETTINGS
# Alfresco Format: "classic" / "current" is supported only from 7.0
ALFRESCO_FORMAT=current

#CA
${CI_WORKSPACE}/alfresco-ssl-generator/ssl-tool/run_ca.sh -keysize 2048 -keystorepass password -certdname "/C=GB/ST=UK/L=Maidenhead/O=Alfresco Software Ltd./OU=Unknown/CN=Custom Alfresco CA" -servername localhost -validityduration 1
#Alfresco
${CI_WORKSPACE}/alfresco-ssl-generator/ssl-tool/run_additional.sh -servicename alfresco -rootcapass password -keysize 2048 -keystoretype JCEKS -keystorepass password -truststoretype JCEKS -truststorepass password -certdname "/C=GB/ST=UK/L=Maidenhead/O=Alfresco Software Ltd./OU=Unknown/CN=Custom Alfresco Repository" -servername localhost,alfresco -alfrescoformat $ALFRESCO_FORMAT
#Alfresco Metadata encryption
${CI_WORKSPACE}/alfresco-ssl-generator/ssl-tool/run_encryption.sh -subfoldername alfresco -servicename encryption -encstorepass mp6yc0UD9e -encmetadatapass oKIWzVdEdA -alfrescoformat $ALFRESCO_FORMAT
#T-Engine AIO
${CI_WORKSPACE}/alfresco-ssl-generator/ssl-tool/run_additional.sh -servicename tengineAIO -rootcapass password -keysize 2048 -keystoretype JCEKS -keystorepass password -truststoretype JCEKS -truststorepass password -certdname "/C=GB/ST=UK/L=Maidenhead/O=Alfresco Software Ltd./OU=Unknown/CN=T-Engine AIO" -servername localhost,transform-core-aio -alfrescoformat $ALFRESCO_FORMAT
#Shared file store
${CI_WORKSPACE}/alfresco-ssl-generator/ssl-tool/run_additional.sh -servicename sharedFileStore -rootcapass password -keysize 2048 -keystoretype JCEKS -keystorepass password -truststoretype JCEKS -truststorepass password -certdname "/C=GB/ST=UK/L=Maidenhead/O=Alfresco Software Ltd./OU=Unknown/CN=Shared File Store" -servername localhost,shared-file-store -alfrescoformat $ALFRESCO_FORMAT
#Transform Router
${CI_WORKSPACE}/alfresco-ssl-generator/ssl-tool/run_additional.sh -servicename transformRouter -rootcapass password -keysize 2048 -keystoretype JCEKS -keystorepass password -truststoretype JCEKS -truststorepass password -certdname "/C=GB/ST=UK/L=Maidenhead/O=Alfresco Software Ltd./OU=Unknown/CN=Transform Router" -servername localhost,transform-router -alfrescoformat $ALFRESCO_FORMAT
