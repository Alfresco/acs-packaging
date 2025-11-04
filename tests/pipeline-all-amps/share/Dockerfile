ARG BASE_IMAGE
FROM ${BASE_IMAGE}

ARG TOMCAT_DIR=/usr/local/tomcat
COPY target/amps_share/*.amp /usr/local/tomcat/amps_share/

ENV TOMCAT_DIR=${TOMCAT_DIR}

ENV ALFRESCO_AMPS=ALL
ENV ALFRESCO_WEBAPP=share
ENV ALFRESCO_AMPS_DIR=$TOMCAT_DIR/amps_share

# Switch between currently installed JRE version and 25
ARG JAVA_VERSION

RUN if [[ "${JAVA_VERSION}" == "25" ]]; then \
      ARCH=$(uname -m | sed s/86_//); \
      echo "Detected architecture: ${ARCH}"; \
      JAVA_RELEASE=25.0.1_8; \
      curl -fsLo java.tar.gz https://github.com/adoptium/temurin${JAVA_VERSION}-binaries/releases/download/jdk-${JAVA_RELEASE/_/+}/OpenJDK${JAVA_VERSION}U-jdk_${ARCH}_alpine-linux_hotspot_${JAVA_RELEASE}.tar.gz && \
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

COPY docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["/usr/local/tomcat/shared/classes/alfresco/substituter.sh", "catalina.sh run"]

LABEL quay.expires-after=${docker.quay-expires.value}
