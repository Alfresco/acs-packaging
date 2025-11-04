ARG BASE_IMAGE
# BUILD STAGE SWAGGERBUILDER
FROM ${BASE_IMAGE} AS SWAGGERBUILDER

ARG JOLOKIA_VER=2.1.2

USER root
RUN yum install -y curl-minimal unzip && \
    mkdir -p /build/{api-explorer,gs-api-explorer,jolokia} && \
    curl -o /build/jolokia.war https://repo1.maven.org/maven2/org/jolokia/jolokia-agent-war-unsecured/${JOLOKIA_VER}/jolokia-agent-war-unsecured-${JOLOKIA_VER}.war && \
    unzip -q /build/jolokia.war -d /build/jolokia

COPY target/wars/api-explorer.war /build
COPY target/wars/gs-api-explorer.war /build

RUN unzip -q /build/api-explorer.war -d /build/api-explorer && \
    unzip -q /build/gs-api-explorer.war -d /build/gs-api-explorer && \
    chmod -R g+r,g-w,o= /build

# ACTUAL IMAGE
FROM ${BASE_IMAGE}

USER root

ARG TOMCAT_DIR=/usr/local/tomcat
ARG GROUPANME=Alfresco

# Copy the amps from build context to the appropriate location for your application server
COPY target/amps/*.amp ${TOMCAT_DIR}/amps/
# Copy the JAR "amps" from build context to the appropriate location for your application server
COPY target/amps/*.jar ${TOMCAT_DIR}/webapps/alfresco/WEB-INF/lib/

# Copy api-explorer
COPY --chown=root:${GROUPANME} --from=SWAGGERBUILDER /build/api-explorer ${TOMCAT_DIR}/webapps/api-explorer

# Copy gs-api-explorer
COPY --chown=root:${GROUPANME} --from=SWAGGERBUILDER /build/gs-api-explorer ${TOMCAT_DIR}/webapps/gs-api-explorer

# Turn on log4j debug frequently needed in the single pipeline
RUN echo -e '\n\
logger.alfresco-repo-content-transform-TransformerDebug.name=org.alfresco.repo.content.transform.TransformerDebug\n\
logger.alfresco-repo-content-transform-TransformerDebug.level=debug\n\
' >> ${TOMCAT_DIR}/shared/classes/alfresco/extension/custom-log4j2.properties

# Grant all security permissions to jolokia and share in order to work properly.
RUN sed -i -e "\$a\grant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/jolokia\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};\ngrant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/share\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};" ${TOMCAT_DIR}/conf/catalina.policy

# Switch between currently installed JRE version and 25
ARG JAVA_VERSION
RUN echo "JAVA_VERSION architecture: ${JAVA_VERSION}";

RUN if [[ "${JAVA_VERSION}" == "25" ]]; then \
      ARCH=$(uname -m | sed s/86_//); \
      echo "Detected architecture: ${ARCH}"; \
      JAVA_RELEASE=25.0.1_8; \
      curl -fsLo java.tar.gz https://github.com/adoptium/temurin${JAVA_VERSION}-binaries/releases/download/jdk-${JAVA_RELEASE/_/+}/OpenJDK${JAVA_VERSION}U-debugimage_${ARCH}_alpine-linux_hotspot_${JAVA_RELEASE}.tar.gz && \
      tar xvfz java.tar.gz && \
      mv jdk-* /usr/lib/jvm/temurin-25-jdk && \
      update-alternatives --install /usr/bin/java java /usr/lib/jvm/temurin-25-jdk/bin/java 20000000 && \
      echo "All Java alternatives before:" && \
      update-alternatives --display java && \
      echo "Removing all non-Java-25 alternatives..." && \
      update-alternatives --display java | grep "bin/java" | grep -v "temurin-25" | awk '{print $1}' | while read -r path; do \
        echo "Removing: ${path}" && \
        update-alternatives --remove java "${path}"; \
      done && \
      echo "All Java alternatives after cleanup:" && \
      update-alternatives --display java; \
    fi

RUN if [ -d "/usr/lib/jvm/" ]; then \
      echo " Path exists: /usr/lib/jvm/"; \
      echo "Contents:"; \
      find /usr/lib/jvm/ -maxdepth 2 -ls; \
    else \
      echo " Path does NOT exist: /usr/lib/jvm/"; \
    fi

#Use the alfresco user
#USER alfresco

ENV TOMCAT_DIR=${TOMCAT_DIR}

ENV ALFRESCO_AMPS=ALL
ENV ALFRESCO_WEBAPP=alfresco
ENV ALFRESCO_AMPS_DIR=$TOMCAT_DIR/amps

COPY docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["catalina.sh", "run", "-security"]

LABEL quay.expires-after=${docker.quay-expires.value}
