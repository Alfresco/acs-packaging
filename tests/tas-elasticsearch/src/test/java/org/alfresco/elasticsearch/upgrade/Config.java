package org.alfresco.elasticsearch.upgrade;

interface Config
{
    default String getElasticsearchHostname()
    {
        return "elasticsearch";
    }

    default String getElasticsearchImage()
    {
        return "elasticsearch:7.10.1";
    }

    default String getPostgreSQLImage()
    {
        return "postgres:13.3";
    }

    default String getActiveMqImage()
    {
        return "alfresco/alfresco-activemq:5.16.4-jre11-centos7";
    }

    default String getSharedFileStoreImage()
    {
        return "quay.io/alfresco/alfresco-shared-file-store:0.16.1";
    }

    default String getTransformCoreAIOImage()
    {
        return "alfresco/alfresco-transform-core-aio:2.5.7";
    }

    default String getTransformRouterImage()
    {
        return "quay.io/alfresco/alfresco-transform-router:1.5.2";
    }

    default String getRepositoryImage()
    {
        return "quay.io/alfresco/alfresco-content-repository:7.2.0";
    }

    default String getReIndexingImage()
    {
        return "quay.io/alfresco/alfresco-elasticsearch-reindexing:3.1.1";
    }

    default String getLiveIndexingImage()
    {
        return "quay.io/alfresco/alfresco-elasticsearch-live-indexing:3.1.1";
    }

    default String getIndexName()
    {
        return "alfresco";
    }

    default String getSearchServiceImageName()
    {
        return "alfresco/alfresco-search-services:2.0.3";
    }
}
