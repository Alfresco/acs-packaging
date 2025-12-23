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

# ------------------ AOS AMP: Install into ROOT webapps -----------------
AOS_AMP=$(ls $ALFRESCO_AMPS_DIR/alfresco-aos-module-*.amp 2>/dev/null | head -n 1)
if [ -f "$AOS_AMP" ]; then
    echo "Installing AOS AMP into ROOT webapps..."
    java -jar $ALFRESCO_MMT_JAR install "$AOS_AMP" "$ROOT_WEBAPP_DIR" -directory -nobackup -force -verbose
else
    echo "No AOS AMP found; skipping AOS AMP installation"
fi

# ------------------ BAKERY APPROACH: Provide explicit Tomcat context file ------------------
echo "Copy context.xml to conf/Catalina/localhost for Bakery pattern safety"
mkdir -p $TOMCAT_DIR/conf/Catalina/localhost
if [ -f "$TOMCAT_DIR/webapps/ROOT.war" ] && unzip -l "$TOMCAT_DIR/webapps/ROOT.war" | grep "META-INF/context.xml" > /dev/null; then
    unzip -p "$TOMCAT_DIR/webapps/ROOT.war" META-INF/context.xml > "$TOMCAT_DIR/conf/Catalina/localhost/ROOT.xml"
fi
# ------------------ END CONTEXT COPY ------------------

exec "$@"
