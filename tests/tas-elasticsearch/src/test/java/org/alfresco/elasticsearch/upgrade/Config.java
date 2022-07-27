package org.alfresco.elasticsearch.upgrade;

import static org.alfresco.elasticsearch.AlfrescoStackInitializer.getImagesConfig;

import org.alfresco.elasticsearch.EnvHelper;

interface Config
{
    String getRepositoryImage();

    String getSearchServiceImageName();

    String getPostgreSQLImage();

    String getActiveMqImage();

    String getSharedFileStoreImage();

    String getTransformCoreAIOImage();

    String getTransformRouterImage();

    String getReIndexingImage();

    String getLiveIndexingImage();

    String getElasticsearchImage();

    default String getElasticsearchHostname()
    {
        return "elasticsearch";
    }

    default String getIndexName()
    {
        return "alfresco";
    }

    static Config getUpgradeScenarioConfig()
    {
        return new Config()
        {
            @Override
            public String getRepositoryImage()
            {
                return getImagesConfig().getRepositoryImage();
            }

            @Override
            public String getSearchServiceImageName()
            {
                return "quay.io/alfresco/insight-engine:" + EnvHelper.getEnvProperty("SOLR6_TAG");
            }

            @Override
            public String getPostgreSQLImage()
            {
                return getImagesConfig().getPostgreSQLImage();
            }

            @Override
            public String getActiveMqImage()
            {
                return getImagesConfig().getActiveMqImage();
            }

            @Override
            public String getSharedFileStoreImage()
            {
                return getImagesConfig().getSharedFileStoreImage();
            }

            @Override
            public String getTransformCoreAIOImage()
            {
                return getImagesConfig().getTransformCoreAIOImage();
            }

            @Override
            public String getTransformRouterImage()
            {
                return getImagesConfig().getTransformRouterImage();
            }

            @Override
            public String getReIndexingImage()
            {
                return getImagesConfig().getReIndexingImage();
            }

            @Override
            public String getLiveIndexingImage()
            {
                return getImagesConfig().getLiveIndexingImage();
            }

            @Override
            public String getElasticsearchImage()
            {
                return getImagesConfig().getElasticsearchImage();
            }
        };
    }
}
