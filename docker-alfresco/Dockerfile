# Fetch image based on Tomcat 8.5.34 and Java 11
# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-tomcat
FROM alfresco/alfresco-base-tomcat:8.5.34-java-11-openjdk-centos-7

# Set default docker_context. Will / Can be overriden with maven.
ARG resource_path=target

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG USERNAME=alfresco
ARG USERID=33000

# Set default environment args
ARG TOMCAT_DIR=/usr/local/tomcat
ARG JAVA_PROFILER=YourKit-JavaProfiler-2018.04-docker
ARG JAVA_PROFILER_DIR=/usr/local/YourKit-JavaProfiler-2018.04
ARG JAVA_PROFILER_RUNTIME_DIR=/tmp/Alfresco/yourkit

# Create prerequisite to store tools and properties
RUN mkdir -p ${TOMCAT_DIR}/shared/classes/alfresco/extension && \
    mkdir ${TOMCAT_DIR}/alfresco-mmt && \
    touch ${TOMCAT_DIR}/shared/classes/alfresco-global.properties

# You need to run `mvn clean install` in the root of this project to update the following dependencies
# Copy the WAR files to the appropriate location for your application server
# Copy the JDBC drivers for the database you are using to the lib/ directory.
# Copy the alfresco-mmt.jar
COPY ${resource_path}/war ${TOMCAT_DIR}/webapps
COPY ${resource_path}/connector/* ${TOMCAT_DIR}/lib/
COPY ${resource_path}/alfresco-mmt/* ${TOMCAT_DIR}/alfresco-mmt/
RUN rm -rf ${TOMCAT_DIR}/webapps/ROOT
COPY ${resource_path}/server-root/* ${TOMCAT_DIR}/webapps
COPY ${resource_path}/vti-bin/* ${TOMCAT_DIR}/webapps

# Change the value of the shared.loader= property to the following:
# shared.loader=${catalina.base}/shared/classes
RUN sed -i "s/shared.loader=/shared.loader=\${catalina.base}\/shared\/classes/" ${TOMCAT_DIR}/conf/catalina.properties

# Add here configurations for alfresco-global.properties
RUN echo -e '\n\
jodconverter.enabled=true\n\
' >> ${TOMCAT_DIR}/shared/classes/alfresco-global.properties

# Add debug for testing
# RUN echo -e '\n\
# log4j.logger.org.alfresco.repo.content.transform.TransformerDebug=debug\n\
# ' >> ${TOMCAT_DIR}/shared/classes/alfresco/extension/custom-log4j.properties

RUN mkdir -p ${TOMCAT_DIR}/amps

# Copy the amps from build context to the appropriate location for your application server
COPY ${resource_path}/amps ${TOMCAT_DIR}/amps

# Install amps on alfresco.war
RUN java -jar ${TOMCAT_DIR}/alfresco-mmt/alfresco-mmt*.jar install \
              ${TOMCAT_DIR}/amps ${TOMCAT_DIR}/webapps/alfresco -directory -nobackup -force

# Unpack ROOT.war, move the log file and make webapps folder read-only.
RUN mkdir ${TOMCAT_DIR}/webapps/ROOT && cd ${TOMCAT_DIR}/webapps/ROOT && \
    jar -xvf ${TOMCAT_DIR}/webapps/ROOT.war && rm -f ${TOMCAT_DIR}/webapps/ROOT.war && \
    sed -i -e "s_log4j.appender.File.File\=alfresco.log_log4j.appender.File.File\=${TOMCAT_DIR}/logs\/alfresco.log_" \
        ${TOMCAT_DIR}/webapps/alfresco/WEB-INF/classes/log4j.properties && \
    chmod -R =r ${TOMCAT_DIR}/webapps && \

# Add catalina.policy to ROOT.war and alfresco.war
# Grant all security permissions to alfresco webapp because of numerous permissions required in order to work properly.
# Grant only deployXmlPermission to ROOT webapp.
    sed -i -e "\$a\grant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/alfresco\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};\ngrant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/ROOT\/-\" \{\n\    permission org.apache.catalina.security.DeployXmlPermission \"ROOT\";\n\};" ${TOMCAT_DIR}/conf/catalina.policy

# Unpack _vti_bin.war
RUN mkdir ${TOMCAT_DIR}/webapps/_vti_bin && cd ${TOMCAT_DIR}/webapps/_vti_bin && jar -xvf ${TOMCAT_DIR}/webapps/_vti_bin.war && rm -f ${TOMCAT_DIR}/webapps/_vti_bin.war

# The standard configuration is to have all Tomcat files owned by root with group GROUPNAME and whilst owner has read/write privileges, 
# group only has restricted permissions and world has no permissions.
RUN mkdir -p ${TOMCAT_DIR}/conf/Catalina/localhost && \
    mkdir -p ${TOMCAT_DIR}/alf_data && \
    groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${USERNAME} && \
    chgrp -R ${GROUPNAME} ${TOMCAT_DIR} && \
    chmod g+rx ${TOMCAT_DIR}/conf && \
    chmod -R g+r ${TOMCAT_DIR}/conf && \
    find ${TOMCAT_DIR}/webapps -type d -exec chmod 0750 {} \; && \
    find ${TOMCAT_DIR}/webapps -type f -exec chmod 0640 {} \; && \
    chmod -R g+r ${TOMCAT_DIR}/webapps && \
    chmod g+r ${TOMCAT_DIR}/conf/Catalina && \
    chmod g+rwx ${TOMCAT_DIR}/alf_data && \
    chmod g+rwx ${TOMCAT_DIR}/logs && \
    chmod o-w ${TOMCAT_DIR}/logs && \
    chmod g+rwx ${TOMCAT_DIR}/temp && \
    chmod g+rwx ${TOMCAT_DIR}/work && \
    chmod o-w ${TOMCAT_DIR}/work

# To remote debug into this image add: EXPOSE 8000
# Changes are also required to the docker-compose/docker-compose.yml file.
# EXPOSE 8000

USER ${USERNAME}