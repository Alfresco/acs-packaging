# Fetch image based on Tomcat 8.5.28 and Java 8
# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-tomcat
FROM alfresco/alfresco-base-tomcat:8.5.34-java-11-openjdk-centos-7

# Create prerequisite to store tools and properties
RUN mkdir -p /usr/local/tomcat/shared/classes/alfresco/extension && \
    mkdir /usr/local/tomcat/alfresco-mmt
RUN touch /usr/local/tomcat/shared/classes/alfresco-global.properties

# You need to run `mvn clean install` in the root of this project to update the following dependencies
# Copy the WAR files to the appropriate location for your application server
# Copy the JDBC drivers for the database you are using to the lib/ directory.
# Copy the alfresco-mmt.jar
COPY target/war /usr/local/tomcat/webapps
COPY target/connector/* /usr/local/tomcat/lib/
COPY target/alfresco-mmt/* /usr/local/tomcat/alfresco-mmt/
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY target/server-root/* /usr/local/tomcat/webapps
COPY target/vti-bin/* /usr/local/tomcat/webapps

# Change the value of the shared.loader= property to the following:
# shared.loader=${catalina.base}/shared/classes
RUN sed -i "s/shared.loader=/shared.loader=\${catalina.base}\/shared\/classes/" /usr/local/tomcat/conf/catalina.properties

# Add here configurations for alfresco-global.properties
# Lowercase the bucket name as bamboo will pass in a bucket name with the build plan key in it
RUN echo -e '\n\
jodconverter.enabled=true\n\
s3.accessKey=${aws-access-key-id}\n\
s3.secretKey=${aws-secret-access-key}\n\
s3.bucketName=${s3.bucketName}\n\
s3.autoLowerCaseBucketName=true\n\
centera.url=168.159.214.26?/binaries/bamboo-agent-config/Centera/c2armtesting.pea\n\
saml.sp.isEnforced=false\n\
dsync.service.uris=http://127.0.0.1:8080/alfresco\n\
messaging.broker.url=vm:broker:(tcp://localhost:61616)?persistent=false\n\
' >> /usr/local/tomcat/shared/classes/alfresco-global.properties

# Add debug for testing
# RUN echo -e '\n\
# log4j.logger.org.alfresco.repo.content.transform.TransformerDebug=debug\n\
# ' >> /usr/local/tomcat/shared/classes/alfresco/extension/custom-log4j.properties

# Amps are already applied to exploded war

# Docker CMD from parent image starts the server

# Unpack ROOT.war and make webapps folder read-only.
RUN mkdir /usr/local/tomcat/webapps/ROOT && cd /usr/local/tomcat/webapps/ROOT && jar -xvf /usr/local/tomcat/webapps/ROOT.war && rm -f /usr/local/tomcat/webapps/ROOT.war && chmod -R =r /usr/local/tomcat/webapps && \
# Add catalina.policy to ROOT.war and alfresco.war
# Grant all security permissions to alfresco webapp because of numerous permissions required in order to work properly.
# Grant only deployXmlPermission to ROOT webapp.
# Grant all security permissions to jolokia and share in order to work properly.
    sed -i -e "\$a\grant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/alfresco\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};\ngrant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/ROOT\/-\" \{\n\    permission org.apache.catalina.security.DeployXmlPermission \"ROOT\";\n\};\ngrant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/jolokia\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};\ngrant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/share\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};" /usr/local/tomcat/conf/catalina.policy

# Unpack _vti_bin.war
RUN mkdir /usr/local/tomcat/webapps/_vti_bin && cd /usr/local/tomcat/webapps/_vti_bin && jar -xvf /usr/local/tomcat/webapps/_vti_bin.war && rm -f /usr/local/tomcat/webapps/_vti_bin.war
