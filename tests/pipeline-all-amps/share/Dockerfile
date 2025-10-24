ARG BASE_IMAGE=alfresco/alfresco-base-java:jre21-rockylinux9@sha256:792079aa36a7a1076e7d48dd800b5de5ffc1cd48e8460fd7e56f7107d375c0cc
FROM ${BASE_IMAGE}

ARG TOMCAT_DIR=/usr/local/tomcat
COPY target/amps_share/*.amp /usr/local/tomcat/amps_share/

ENV TOMCAT_DIR=${TOMCAT_DIR}

ENV ALFRESCO_AMPS=ALL
ENV ALFRESCO_WEBAPP=share
ENV ALFRESCO_AMPS_DIR=$TOMCAT_DIR/amps_share

# Switch between currently installed JRE version and 21
ARG JAVA_VERSION
RUN if [[ "$JAVA_VERSION" == "21" ]]; then \
      ARCH=$(uname -m | sed s/86_//); \
      JAVA_RELEASE=21.0.4_7; \
      curl -fsLo java.tar.gz https://github.com/adoptium/temurin${JAVA_VERSION}-binaries/releases/download/jdk-${JAVA_RELEASE/_/+}/OpenJDK${JAVA_VERSION}U-jre_${ARCH}_linux_hotspot_${JAVA_RELEASE}.tar.gz && \
      tar xvfz java.tar.gz && \
      mv jdk-* /usr/lib/jvm/temurin-21-jdk && \
      update-alternatives --install /usr/bin/java java /usr/lib/jvm/temurin-21-jdk/bin/java 1 && \
      update-alternatives --remove java $(update-alternatives --display java | head -2 | tail -1 | cut -d " " -f6); \
    fi

COPY docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["/usr/local/tomcat/shared/classes/alfresco/substituter.sh", "catalina.sh run"]

LABEL quay.expires-after=${docker.quay-expires.value}
