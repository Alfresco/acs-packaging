package org.alfresco.elasticsearch.upgrade;

import static java.time.Duration.ofMinutes;

import static org.alfresco.elasticsearch.AlfrescoStackInitializer.getImagesConfig;
import static org.alfresco.elasticsearch.upgrade.Utils.createNetwork;
import static org.alfresco.elasticsearch.upgrade.Utils.createTempContentStoreDirectory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.UUID;

import org.alfresco.elasticsearch.EnvHelper;
import org.springframework.util.FileSystemUtils;
import org.testcontainers.containers.Network;
import org.testng.Assert;
import org.testng.annotations.Test;

public class FromLegacyAcsUpgradeTest
{
    private static final URL TEST_FILE_URL = FromSolrUpgradeTest.class.getResource("babekyrtso.pdf");
    private static final String SEARCH_TERM = "babekyrtso";
    public static final String FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP = "after-startup.pdf";
    public static final String FILE_UPLOADED_WHILE_MIRRORING = "while-mirroring.pdf";
    public static final String FILE_UPLOADED_BEFORE_UPGRADING_LEGACY_ENVIRONMENT = "before-upgrade.pdf";
    public static final String FILE_UPLOADED_AFTER_UPGRADE = "after-upgrade.pdf";

    @Test
    public void testLegacyUpgrade() throws IOException
    {
        try (LegacyAcsUpgradeScenario scenario = new LegacyAcsUpgradeScenario(getUpgradeScenarioConfig())) {
            final LegacyACSEnv legacyEnv = scenario.startLegacyEnv();

            legacyEnv.expectNoSearchResult(ofMinutes(5), UUID.randomUUID().toString());
            legacyEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP);
            legacyEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM, FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP);

            final Elasticsearch elasticsearch = scenario.startElasticsearch();
            Assert.assertFalse(elasticsearch.isIndexCreated());

            final long initialReIndexingUpperBound = legacyEnv.getMaxNodeDbId();

            try (ACSEnv mirroredEnv = scenario.startMirroredEnvWitElasticsearchBasedSearchService())
            {
                legacyEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_WHILE_MIRRORING);
                legacyEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM, FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP, FILE_UPLOADED_WHILE_MIRRORING);

                mirroredEnv.expectNoSearchResult(ofMinutes(1), SEARCH_TERM);

                Assert.assertTrue(elasticsearch.isIndexCreated());
                Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), 0);
                mirroredEnv.expectNoSearchResult(ofMinutes(1), SEARCH_TERM);

                mirroredEnv.startLiveIndexing();
                mirroredEnv.reindexByIds(0, initialReIndexingUpperBound);

                Assert.assertTrue(elasticsearch.getIndexedDocumentCount() > 0);
                mirroredEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM, FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP);
            }

            legacyEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_BEFORE_UPGRADING_LEGACY_ENVIRONMENT);
            legacyEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM,
                    FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP,
                    FILE_UPLOADED_WHILE_MIRRORING,
                    FILE_UPLOADED_BEFORE_UPGRADING_LEGACY_ENVIRONMENT);

            try (final ACSEnv upgradedEnv = scenario.upgradeLegacyEnvironmentToCurrent())
            {
                upgradedEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM, FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP);

                upgradedEnv.startLiveIndexing();

                upgradedEnv.reindexByIds(initialReIndexingUpperBound, 1_000_000_000);
                upgradedEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM,
                        FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP,
                        FILE_UPLOADED_WHILE_MIRRORING,
                        FILE_UPLOADED_BEFORE_UPGRADING_LEGACY_ENVIRONMENT);

                upgradedEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_AFTER_UPGRADE);
                upgradedEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM,
                        FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP,
                        FILE_UPLOADED_WHILE_MIRRORING,
                        FILE_UPLOADED_BEFORE_UPGRADING_LEGACY_ENVIRONMENT,
                        FILE_UPLOADED_AFTER_UPGRADE);
            }
        }
    }

    private Config getUpgradeScenarioConfig()
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
