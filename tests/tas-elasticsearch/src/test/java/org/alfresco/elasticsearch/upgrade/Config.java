package org.alfresco.elasticsearch.upgrade;

import org.alfresco.elasticsearch.SearchEngineType;

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

    String getOpensearchImage();

    String getSearchEngineImage();

    SearchEngineType getSearchEngineType();

    default String getElasticsearchHostname()
    {
        return "elasticsearch";
    }

    default String getIndexName()
    {
        return "alfresco";
    }
}
