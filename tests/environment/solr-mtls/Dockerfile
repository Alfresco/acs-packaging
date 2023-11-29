ARG SOLR_TAG
FROM quay.io/alfresco/insight-engine:$SOLR_TAG

# Create search_config_setup.sh if it does not exist (e.g. on SearchServices 1.2.x or earlier).
USER root
RUN touch ${DIST_DIR}/solr/bin/search_config_setup.sh \
    && chown solr:solr ${DIST_DIR}/solr/bin/search_config_setup.sh
USER solr

RUN replacementPairs=("alfresco.secureComms=none/alfresco.secureComms=https" "alfresco.encryption.ssl.keystore.location=.*/alfresco.encryption.ssl.keystore.location=\\\/opt\\\/alfresco-search-services\\\/keystore\\\/searchEngine.keystore" "alfresco.encryption.ssl.keystore.type=.*/alfresco.encryption.ssl.keystore.type=JCEKS" "alfresco.encryption.ssl.truststore.location=.*/alfresco.encryption.ssl.truststore.location=\\\/opt\\\/alfresco-search-services\\\/keystore\\\/searchEngine.truststore" "alfresco.encryption.ssl.truststore.type=.*/alfresco.encryption.ssl.truststore.type=JCEKS"); \
    for replacementPair in ${replacementPairs[@]}; \
    do \
        sed -i '/^bash.*/i sed -i "'"s/$replacementPair/g"'" ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties\n' \
        ${DIST_DIR}/solr/bin/search_config_setup.sh; \
    done; \
    if [[ "" != "" ]]; \
    then \
        sed -i '/^bash.*/i echo "\n" >> ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties\n' \
        ${DIST_DIR}/solr/bin/search_config_setup.sh; \
    fi

USER root
RUN mkdir -p /opt/alfresco-search-services/keystore \
    && chown -R solr:solr /opt/alfresco-search-services/keystore
USER solr

# Set the search log level if requested.
RUN if [ "WARN" != "" ] ; then \
  sed -i '/^bash.*/i sed -i "'"s/log4j.rootLogger=WARN, file, CONSOLE/log4j.rootLogger=WARN, file, CONSOLE/g"'" ${DIST_DIR}/logs/log4j.properties\n' \
  ${DIST_DIR}/solr/bin/search_config_setup.sh; \
fi

# Enable cross locale configuration if requested.
RUN if [[ "False" == "True" ]] ; then \
  sed -i '/^bash.*/i sed -i "'"/alfresco.cross.locale.datatype/s/^#//g"'" ${DIST_DIR}/solrhome/conf/shared.properties\n' \
  ${DIST_DIR}/solr/bin/search_config_setup.sh; \
fi
