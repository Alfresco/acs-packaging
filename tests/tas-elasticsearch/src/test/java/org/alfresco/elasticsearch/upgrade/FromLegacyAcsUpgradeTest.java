package org.alfresco.elasticsearch.upgrade;

import static org.alfresco.elasticsearch.upgrade.Config.getUpgradeScenarioConfig;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

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
        try (LegacyAcsUpgradeScenario scenario = new LegacyAcsUpgradeScenario(getUpgradeScenarioConfig()))
        {
            final LegacyACSEnv legacyEnv = scenario.startLegacyEnv();

            legacyEnv.expectNoSearchResult(UUID.randomUUID().toString());
            legacyEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP);
            legacyEnv.expectSearchResult(SEARCH_TERM, FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP);

            final Elasticsearch elasticsearch = scenario.startElasticsearch();
            Assert.assertFalse(elasticsearch.isIndexCreated());

            final long initialReIndexingUpperBound = legacyEnv.getMaxNodeDbId();

            try (ACSEnv mirroredEnv = scenario.startMirroredEnvWitElasticsearchBasedSearchService())
            {
                legacyEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_WHILE_MIRRORING);
                legacyEnv.expectSearchResult(SEARCH_TERM, FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP, FILE_UPLOADED_WHILE_MIRRORING);

                mirroredEnv.expectNoSearchResult(SEARCH_TERM);

                elasticsearch.waitForIndexCreation();
                Assert.assertTrue(elasticsearch.isIndexCreated());
                Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), 0);
                mirroredEnv.expectNoSearchResult(SEARCH_TERM);

                mirroredEnv.startLiveIndexing();
                mirroredEnv.reindexByIds(0, initialReIndexingUpperBound * 2);

                Assert.assertTrue(elasticsearch.getIndexedDocumentCount() > 0);
                mirroredEnv.expectSearchResult(SEARCH_TERM, FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP);
            }

            legacyEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_BEFORE_UPGRADING_LEGACY_ENVIRONMENT);
            legacyEnv.expectSearchResult(SEARCH_TERM,
                    FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP,
                    FILE_UPLOADED_WHILE_MIRRORING,
                    FILE_UPLOADED_BEFORE_UPGRADING_LEGACY_ENVIRONMENT);

            try (final ACSEnv upgradedEnv = scenario.upgradeLegacyEnvironmentToCurrent())
            {
                upgradedEnv.expectSearchResult(SEARCH_TERM, FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP);

                upgradedEnv.startLiveIndexing();

                upgradedEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_AFTER_UPGRADE);
                upgradedEnv.expectSearchResult(SEARCH_TERM, FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP,
                        FILE_UPLOADED_AFTER_UPGRADE);

                upgradedEnv.reindexByIds((long) (initialReIndexingUpperBound * 0.9), 1_000_000_000);
                upgradedEnv.expectSearchResult(SEARCH_TERM,
                        FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP,
                        FILE_UPLOADED_WHILE_MIRRORING,
                        FILE_UPLOADED_BEFORE_UPGRADING_LEGACY_ENVIRONMENT,
                        FILE_UPLOADED_AFTER_UPGRADE);
            }
        }
    }
}
