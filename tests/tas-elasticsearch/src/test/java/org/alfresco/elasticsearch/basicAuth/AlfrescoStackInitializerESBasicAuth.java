package org.alfresco.elasticsearch.basicAuth;

import org.alfresco.elasticsearch.AlfrescoStackInitializer;
import org.alfresco.elasticsearch.SearchEngineType;
import org.testcontainers.containers.GenericContainer;

/**
 * ACS Stack Docker Compose initializer with Basic Authentication for Elasticsearch service.
 */
public class AlfrescoStackInitializerESBasicAuth extends AlfrescoStackInitializer
{

    // Default Elasticsearch credentials
    private static final String ELASTICSEARCH_USERNAME = "admin";
    private static final String ELASTICSEARCH_PASSWORD = "admin";

    @Override
    protected GenericContainer createLiveIndexingContainer()
    {
          GenericContainer container = super.createLiveIndexingContainer();
          container.withEnv("SPRING_ELASTICSEARCH_REST_USERNAME", ELASTICSEARCH_USERNAME);
          container.withEnv("SPRING_ELASTICSEARCH_REST_PASSWORD", ELASTICSEARCH_PASSWORD);
          return container;
    }

    @Override
    protected GenericContainer createSearchEngineContainer()
    {
        SearchEngineType usedEngine = getImagesConfig().getSearchEngineType();

        if(SearchEngineType.OPENSEARCH_ENGINE.equals(usedEngine))
        {
            return super.createOpensearchContainer()
                    .withEnv("plugins.security.disabled", "false")
                    .withEnv("plugins.security.ssl.http.enabled", "false");
        }
        else
        {
            return super.createElasticContainer()
                    .withEnv("xpack.security.enabled", "true")
                    .withEnv("ELASTIC_PASSWORD", ELASTICSEARCH_PASSWORD);
        }
    }

    @Override
    protected GenericContainer createAlfrescoContainer()
    {
        GenericContainer container = super.createAlfrescoContainer();
        String javaOpts = (String) container.getEnvMap().get("JAVA_OPTS");
        javaOpts = javaOpts + " -Delasticsearch.user=" + ELASTICSEARCH_USERNAME + " " +
            "-Delasticsearch.password=" + ELASTICSEARCH_PASSWORD;
        container.getEnvMap().put("JAVA_OPTS", javaOpts);
        return container;
    }
}
