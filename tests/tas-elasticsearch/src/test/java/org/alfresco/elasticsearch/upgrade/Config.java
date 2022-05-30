package org.alfresco.elasticsearch.upgrade;

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
}
