ARG BASE_IMAGE
### Apply AGS enterprise share AMP to Share image
FROM ${BASE_IMAGE}

### Copy the AMP from build context to the amps_share
COPY target/alfresco-governance-services-enterprise-share-*.amp /usr/local/tomcat/amps_share/
### Install AMP on share
RUN java -jar /usr/local/tomcat/alfresco-mmt/alfresco-mmt*.jar install \
              /usr/local/tomcat/amps_share/alfresco-governance-services-enterprise-share-*.amp /usr/local/tomcat/webapps/share -nobackup -force

###LABEL quay.expires-after=${docker.quay-expires.value}