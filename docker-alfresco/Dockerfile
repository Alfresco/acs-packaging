FROM alfresco/alfresco-enterprise-repo-base:${upstream.image.tag}

# Set default docker_context. Will / Can be overriden with maven.
ARG resource_path=target

# Set default environment args
ARG TOMCAT_DIR=/usr/local/tomcat

USER root

# Copy api-explorer war
COPY ${resource_path}/war/* ${TOMCAT_DIR}/webapps/

# Copy in the share-services amp
COPY ${resource_path}/amps/* ${TOMCAT_DIR}/amps/

# Install amps on alfresco.war
RUN java -jar ${TOMCAT_DIR}/alfresco-mmt/alfresco-mmt*.jar install \
              ${TOMCAT_DIR}/amps \
              ${TOMCAT_DIR}/webapps/alfresco -directory -nobackup

# Make webapps folder read-only.
RUN chmod -R =r /usr/local/tomcat/webapps && \
# Add catalina.policy to ROOT.war and alfresco.war
# Grant all security permissions to alfresco webapp because of numerous permissions required in order to work properly.
# Grant only deployXmlPermission to ROOT webapp.
    sed -i -e "\$a\grant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/alfresco\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};\ngrant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/ROOT\/-\" \{\n\    permission org.apache.catalina.security.DeployXmlPermission \"ROOT\";\n\};" /usr/local/tomcat/conf/catalina.policy
