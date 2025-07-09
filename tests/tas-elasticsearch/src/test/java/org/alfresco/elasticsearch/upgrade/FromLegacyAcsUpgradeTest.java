package org.alfresco.elasticsearch.upgrade;

import static java.time.Duration.ofMinutes;

import static org.alfresco.elasticsearch.upgrade.Config.getUpgradeScenarioConfig;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.Test;

public class FromLegacyAcsUpgradeTest
{
    private static final Duration MAX_TIMEOUT = ofMinutes(10);
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

            legacyEnv.expectNoSearchResult(MAX_TIMEOUT, UUID.randomUUID().toString());
            legacyEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP);
            legacyEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM, FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP);

            final Elasticsearch elasticsearch = scenario.startElasticsearch();
            Assert.assertFalse(elasticsearch.isIndexCreated());

            final long initialReIndexingUpperBound = legacyEnv.getMaxNodeDbId();

            try (ACSEnv mirroredEnv = scenario.startMirroredEnvWitElasticsearchBasedSearchService())
            {
                legacyEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_WHILE_MIRRORING);
                legacyEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM, FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP, FILE_UPLOADED_WHILE_MIRRORING);

                mirroredEnv.expectNoSearchResult(MAX_TIMEOUT, SEARCH_TERM);

                elasticsearch.waitForIndexCreation(MAX_TIMEOUT);
                Assert.assertTrue(elasticsearch.isIndexCreated());
                Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), 0);
                mirroredEnv.expectNoSearchResult(MAX_TIMEOUT, SEARCH_TERM);

                mirroredEnv.startLiveIndexing();
                mirroredEnv.reindexByIds(0, initialReIndexingUpperBound * 2);

                Assert.assertTrue(elasticsearch.getIndexedDocumentCount() > 0);
                mirroredEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM, FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP);
            }

            legacyEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_BEFORE_UPGRADING_LEGACY_ENVIRONMENT);
            legacyEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM,
                    FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP,
                    FILE_UPLOADED_WHILE_MIRRORING,
                    FILE_UPLOADED_BEFORE_UPGRADING_LEGACY_ENVIRONMENT);

            try (final ACSEnv upgradedEnv = scenario.upgradeLegacyEnvironmentToCurrent())
            {
                upgradedEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM, FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP);

                upgradedEnv.startLiveIndexing();

                upgradedEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_AFTER_UPGRADE);
                upgradedEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM, FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP,
                        FILE_UPLOADED_AFTER_UPGRADE);

                upgradedEnv.reindexByIds((long) (initialReIndexingUpperBound * 0.9), 1_000_000_000);
                upgradedEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM,
                        FILE_UPLOADED_AFTER_LEGACY_ENVIRONMENT_STARTUP,
                        FILE_UPLOADED_WHILE_MIRRORING,
                        FILE_UPLOADED_BEFORE_UPGRADING_LEGACY_ENVIRONMENT,
                        FILE_UPLOADED_AFTER_UPGRADE);
            }
        }
    }
}
