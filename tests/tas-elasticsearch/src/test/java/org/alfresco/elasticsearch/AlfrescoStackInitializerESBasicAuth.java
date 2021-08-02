package org.alfresco.elasticsearch;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.util.Properties;

/**
 * ACS Stack Docker Compose initializer with Basic Authentication for Elasticsearch service.
 */
public class AlfrescoStackInitializerESBasicAuth extends AlfrescoStackInitializer
{

    // Default Elasticsearch credentials
    private static final String ELASTICSEARCH_USERNAME = "elastic";
    private static final String ELASTICSEARCH_PASSWORD = "bob123";

    @Override
    protected GenericContainer createLiveIndexingContainer(Properties env)
    {
          GenericContainer container = super.createLiveIndexingContainer(env);
          container.withEnv("SPRING_ELASTICSEARCH_REST_USERNAME", ELASTICSEARCH_USERNAME);
          container.withEnv("SPRING_ELASTICSEARCH_REST_PASSWORD", ELASTICSEARCH_PASSWORD);
          return container;
    }

    @Override
    protected ElasticsearchContainer createElasticContainer(Properties env)
    {
        return new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:" + env.getProperty("ES_TAG"))
            .withNetwork(network)
            .withNetworkAliases("elasticsearch")
            .withExposedPorts(9200)
            .withEnv("xpack.security.enabled", "true")
            .withEnv("discovery.type", "single-node")
            .withEnv("ES_JAVA_OPTS", "\"-Xms2g -Xmx2g\"")
            .withEnv("ELASTIC_PASSWORD", ELASTICSEARCH_PASSWORD);
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
