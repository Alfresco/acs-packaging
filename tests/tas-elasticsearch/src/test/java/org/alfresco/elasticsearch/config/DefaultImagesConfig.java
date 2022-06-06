package org.alfresco.elasticsearch.config;

import java.util.function.Function;

import org.alfresco.elasticsearch.AlfrescoStackInitializer;
import org.alfresco.elasticsearch.EnvHelper;
import org.alfresco.elasticsearch.MavenPropertyHelper;

public final class DefaultImagesConfig implements ImagesConfig
{
    public static final DefaultImagesConfig INSTANCE = new DefaultImagesConfig(EnvHelper::getEnvProperty, MavenPropertyHelper::getMavenProperty);
    private final Function<String, String> envProperties;
    private final Function<String, String> mavenProperties;

    DefaultImagesConfig(Function<String, String> envProperties, Function<String, String> mavenProperties)
    {
        this.envProperties = envProperties;
        this.mavenProperties = mavenProperties;
    }

    @Override
    public String getReIndexingImage()
    {
        return "quay.io/alfresco/alfresco-elasticsearch-reindexing:" + getElasticsearchConnectorImageTag();
    }

    @Override
    public String getLiveIndexingImage()
    {
        return "quay.io/alfresco/alfresco-elasticsearch-live-indexing:" + getElasticsearchConnectorImageTag();
    }

    @Override
    public String getElasticsearchImage()
    {
        return "docker.elastic.co/elasticsearch/elasticsearch:" + envProperties.apply("ES_TAG");
    }

    @Override
    public String getActiveMqImage()
    {
        return "alfresco/alfresco-activemq:" + envProperties.apply("ACTIVEMQ_TAG");
    }

    @Override
    public String getTransformRouterImage()
    {
        return "quay.io/alfresco/alfresco-transform-router:" + envProperties.apply("TRANSFORM_ROUTER_TAG");
    }

    @Override
    public String getTransformCoreAIOImage()
    {
        return "alfresco/alfresco-transform-core-aio:" + envProperties.apply("TRANSFORMERS_TAG");
    }

    @Override
    public String getSharedFileStoreImage()
    {
        return "quay.io/alfresco/alfresco-shared-file-store:" + envProperties.apply("SFS_TAG");
    }

    @Override
    public String getPostgreSQLImage()
    {
        return "postgres:" + envProperties.apply("POSTGRES_TAG");
    }

    @Override
    public String getRepositoryImage()
    {
        return "alfresco/alfresco-content-repository:latest";
    }

    @Override
    public String getKibanaImage()
    {
        return "kibana:7.10.1";
    }

    private String getElasticsearchConnectorImageTag()
    {
        final String fromEnv = envProperties.apply("ES_CONNECTOR_TAG");
        if (fromEnv != null && !fromEnv.isBlank())
        {
            return fromEnv;
        }
        return mavenProperties.apply("dependency.elasticsearch-shared.version");
    }
}
