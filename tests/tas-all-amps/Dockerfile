FROM alfresco/alfresco-content-repository:latest

ARG TOMCAT_DIR=/usr/local/tomcat
ARG ALF_GROUP=Alfresco
ARG ALF_USER=alfresco

USER root

RUN rm -rf ${TOMCAT_DIR}/webapps/alfresco

COPY target/war ${TOMCAT_DIR}/webapps

COPY custom-content-service-context.xml ${TOMCAT_DIR}/shared/classes/alfresco/extension/

# set the correct path for alfresco.log
RUN sed -i -e "s_appender.rolling.fileName\=alfresco.log_appender.rolling.fileName\=${TOMCAT_DIR}/logs\/alfresco.log_" \
        ${TOMCAT_DIR}/webapps/alfresco/WEB-INF/classes/log4j2.properties && \
    sed -i -e "s_appender.rolling.filePattern=alfresco.log.%d{yyyy-MM-dd}_appender.rolling.filePattern\=${TOMCAT_DIR}/logs\/alfresco.log.%d{yyyy-MM-dd}_" \
        ${TOMCAT_DIR}/webapps/alfresco/WEB-INF/classes/log4j2.properties && \
    # Grant all security permissions (Tomcat <= 10 only; SecurityManager removed in Tomcat 11)
    if [ -f "${TOMCAT_DIR}/conf/catalina.policy" ]; then \
        sed -i -e "\$a\
        grant codeBase \"file:${catalina.base}/webapps/jolokia/-\" {\n\
            permission java.security.AllPermission;\n\
        };\n\
        grant codeBase \"file:${catalina.base}/webapps/_vti_bin/-\" {\n\
            permission java.security.AllPermission;\n\
        };\n\
        grant codeBase \"file:${catalina.base}/webapps/share/-\" {\n\
            permission java.security.AllPermission;\n\
        };" ${TOMCAT_DIR}/conf/catalina.policy ; \
    else \
       echo \"Skipping catalina.policy modification (Tomcat 11+)\" ; \
    fi && \
    # Restore permissions
    chgrp -R ${ALF_GROUP} ${TOMCAT_DIR}/webapps && \
    find ${TOMCAT_DIR}/webapps -type d -exec chmod 0750 {} \; && \
    find ${TOMCAT_DIR}/webapps -type f -exec chmod 0640 {} \; && \
    find ${TOMCAT_DIR}/shared -type d -exec chmod 0750 {} \; && \
    find ${TOMCAT_DIR}/shared -type f -exec chmod 0640 {} \; && \
    chmod -R g+r ${TOMCAT_DIR}/webapps && \
    chgrp -R ${ALF_GROUP} ${TOMCAT_DIR}

USER ${ALF_USER}
