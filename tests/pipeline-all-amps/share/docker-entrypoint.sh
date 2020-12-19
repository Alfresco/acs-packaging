#!/usr/bin/env bash

set -e

ALFRESCO_WEBAPP_DIR=$TOMCAT_DIR/webapps/$ALFRESCO_WEBAPP
ALFRESCO_MMT_JAR=$TOMCAT_DIR/alfresco-mmt/alfresco-mmt*.jar

echo Available AMPs in $ALFRESCO_AMPS_DIR
ls -l $ALFRESCO_AMPS_DIR

echo Previously installed AMPs in $ALFRESCO_WEBAPP_DIR
INSTALLED_AMPS=$(java -jar $ALFRESCO_MMT_JAR list $ALFRESCO_WEBAPP_DIR)
echo "$INSTALLED_AMPS"
INSTALLED_AMPS=$(echo "$INSTALLED_AMPS" | grep ^Module | cut -f2 -d' ' | tr -d "'")

echo Uninstall previously installed AMPs in $ALFRESCO_AMPS_DIR
for AMP in $INSTALLED_AMPS
do
	echo Uninstalling $AMP
	java -jar $ALFRESCO_MMT_JAR uninstall $AMP $ALFRESCO_WEBAPP_DIR
done

echo Verify no AMPs are previously installed in $ALFRESCO_AMPS_DIR
java -jar $ALFRESCO_MMT_JAR list $ALFRESCO_WEBAPP_DIR

echo Install requested AMPs $ALFRESCO_AMPS
TMP_ALFRESCO_AMPS_DIR=/tmp/amps
mkdir -p $TMP_ALFRESCO_AMPS_DIR

if [ "$ALFRESCO_AMPS" == "ALL" ]
then
 ALFRESCO_AMPS=$(ls $ALFRESCO_AMPS_DIR | xargs)
fi
for AMP in $ALFRESCO_AMPS
do
	echo Installing $AMP
	cp $ALFRESCO_AMPS_DIR/${AMP}* $TMP_ALFRESCO_AMPS_DIR
done
java -jar $ALFRESCO_MMT_JAR install \
	$TMP_ALFRESCO_AMPS_DIR $ALFRESCO_WEBAPP_DIR -directory -nobackup -force -verbose
unset TMP_ALFRESCO_AMPS_DIR

echo Verify requested AMPs $ALFRESCO_AMPS have been installed
java -jar $ALFRESCO_MMT_JAR list $ALFRESCO_WEBAPP_DIR

exec "$@"
