# Fetch image based on Tomcat 8.5.28 and Java 8
# `docker login [OPTIONS] [SERVER]` is currently required
# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-tomcat
FROM quay.io/alfresco/alfresco-base-tomcat:8.5.28-java-8-oracle-centos-7-f7b1278cc0eb

# Create prerequisite to store tools and properties
RUN mkdir -p /usr/local/tomcat/shared/classes && \
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
RUN echo -e '\n\
jodconverter.enabled=true\n\
' >> /usr/local/tomcat/shared/classes/alfresco-global.properties

RUN mkdir -p /usr/local/tomcat/amps

# Copy the amps from build context to the appropriate location for your application server
COPY target/amps /usr/local/tomcat/amps

# Install amps on alfresco.war
RUN java -jar /usr/local/tomcat/alfresco-mmt/alfresco-mmt*.jar install \
              /usr/local/tomcat/amps /usr/local/tomcat/webapps/alfresco -directory -nobackup -force

# Docker CMD from parent image starts the server
