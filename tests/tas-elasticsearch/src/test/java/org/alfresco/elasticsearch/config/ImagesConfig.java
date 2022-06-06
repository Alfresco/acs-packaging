package org.alfresco.elasticsearch.config;

public interface ImagesConfig
{
    String getReIndexingImage();

    String getLiveIndexingImage();

    String getElasticsearchImage();

    String getActiveMqImage();

    String getTransformRouterImage();

    String getTransformCoreAIOImage();

    String getSharedFileStoreImage();

    String getPostgreSQLImage();

    String getRepositoryImage();

    String getKibanaImage();
}
