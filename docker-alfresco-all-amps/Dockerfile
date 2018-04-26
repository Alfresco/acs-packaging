# Fetch image based on Tomcat 8.5.28 and Java 8
# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-tomcat
FROM alfresco/alfresco-base-tomcat:8.5.28-java-8-oracle-centos-7

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
centera.url=168.159.214.26?d:\\kit\\CenteraSDK3.3\\c2armtesting.pea\n\
' >> /usr/local/tomcat/shared/classes/alfresco-global.properties

# Add debug for testing
# RUN echo -e '\n\
# log4j.logger.org.alfresco.repo.content.transform.TransformerDebug=debug\n\
# ' >> /usr/local/tomcat/shared/classes/alfresco/extension/custom-log4j.properties

RUN mkdir -p /usr/local/tomcat/amps
RUN mkdir -p /usr/local/tomcat/amps_share

# Copy the amps from build context to the appropriate location for your application server
COPY target/amps /usr/local/tomcat/amps
COPY target/amps_share /usr/local/tomcat/amps_share

# Install amps on alfresco.war
RUN java -jar /usr/local/tomcat/alfresco-mmt/alfresco-mmt*.jar install \
              /usr/local/tomcat/amps /usr/local/tomcat/webapps/alfresco -directory -nobackup -force

# Install amps on share.war
RUN java -jar /usr/local/tomcat/alfresco-mmt/alfresco-mmt*.jar install \
              /usr/local/tomcat/amps_share /usr/local/tomcat/webapps/share -directory -nobackup -force

# Docker CMD from parent image starts the server

# To remote debug into this image add: EXPOSE 8000
# Changes are also required to the docker-compose/docker-compose.yml file.
# EXPOSE 8000
