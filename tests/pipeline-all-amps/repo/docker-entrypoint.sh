#!/usr/bin/env bash

set -e

# Switch to Java 25 if it has been installed
[ -d "/usr/lib/jvm/temurin-25-jdk" ] && export JAVA_HOME=/usr/lib/jvm/temurin-25-jdk

ALFRESCO_WEBAPP_DIR=$TOMCAT_DIR/webapps/$ALFRESCO_WEBAPP
ALFRESCO_MMT_JAR=$TOMCAT_DIR/alfresco-mmt/alfresco-mmt*.jar
ROOT_WEBAPP_DIR=$TOMCAT_DIR/webapps/ROOT

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

# Copy context.xml for ALFRESCO webapp
ALFRESCO_CTX_SRC="$ALFRESCO_WEBAPP_DIR/META-INF/context.xml"
mkdir -p "$TOMCAT_DIR/conf/Catalina/localhost"
echo "Copying context.xml for ALFRESCO webapp"
[ -f "$ALFRESCO_CTX_SRC" ] && cp "$ALFRESCO_CTX_SRC" "$TOMCAT_DIR/conf/Catalina/localhost/$ALFRESCO_WEBAPP.xml" \
    || { echo "No context.xml found at $ALFRESCO_CTX_SRC!"; exit 1; }

# Explode ROOT.war and copy context.xml for ROOT webapp
if [ -f "$TOMCAT_DIR/webapps/ROOT.war" ]; then
  echo "Exploding ROOT.war for ROOT webapp"
  unzip -q "$TOMCAT_DIR/webapps/ROOT.war" -d "$ROOT_WEBAPP_DIR"

  echo "Copying context.xml for ROOT webapp"
  if [ -f "$ROOT_WEBAPP_DIR/META-INF/context.xml" ]; then
    cp "$ROOT_WEBAPP_DIR/META-INF/context.xml" "$TOMCAT_DIR/conf/Catalina/localhost/ROOT.xml"
  else
    echo "No ROOT context.xml found in $ROOT_WEBAPP_DIR/META-INF/context.xml"
  fi
else
  echo "ROOT.war not found at $TOMCAT_DIR/webapps/ROOT.war"
fi

exec "$@"
