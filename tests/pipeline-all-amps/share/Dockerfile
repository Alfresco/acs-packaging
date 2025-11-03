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
RUN set -x; echo "JAVA_VERSION architecture: ${JAVA_VERSION:-NOT_SET}";

RUN if [[ "25" == "25" ]]; then \
      ARCH=$(uname -m); \
      JAVA_RELEASE=25.0.1_8; \
      curl -fsLo java.tar.gz https://github.com/adoptium/temurin${JAVA_VERSION}-binaries/releases/download/jdk-${JAVA_RELEASE/_/+}/OpenJDK${JAVA_VERSION}U-debugimage_aarch64_alpine-linux_hotspot_${JAVA_RELEASE}.tar.gz && \
      tar xvfz java.tar.gz && \
      mv jdk-* /usr/lib/jvm/temurin-25-jdk && \
      echo "All Java alternatives before:" && \
      update-alternatives --display java && \
      update-alternatives --install /usr/bin/java java /usr/lib/jvm/temurin-25-jdk/bin/java 1 && \
      echo "All Java alternatives:" && \
      echo "All Java alternatives after :" && \
      update-alternatives --remove java $(update-alternatives --display java | head -2 | tail -1 | cut -d " " -f6); \
    fi

COPY docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["/usr/local/tomcat/shared/classes/alfresco/substituter.sh", "catalina.sh run"]

LABEL quay.expires-after=${docker.quay-expires.value}
