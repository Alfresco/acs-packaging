package org.alfresco.elasticsearch.upgrade;

import static java.time.Duration.ofMinutes;

import static org.alfresco.elasticsearch.upgrade.Config.getUpgradeScenarioConfig;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.alfresco.elasticsearch.upgrade.AvailabilityProbe.Stats;

public class FromSolrUpgradeTest
{
    private static final Duration MAX_TIMEOUT = ofMinutes(5);
    private static final URL TEST_FILE_URL = FromSolrUpgradeTest.class.getResource("babekyrtso.pdf");
    private static final String SEARCH_TERM = "babekyrtso";
    private static final String FILE_UPLOADED_BEFORE_INITIAL_REINDEXING = "before-initial-re-indexing.pdf";
    private static final String FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING = "before-live-indexing.pdf";
    private static final String FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING = "after-live-indexing.pdf";
    private static final String FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH = "before-switch.pdf";
    private static final String FILE_UPLOADED_AFTER_SWITCHING_TO_ELASTICSEARCH = "after-switch.pdf";

    @Test
    public void testZeroDowntimeUpgradeFromSolrToElasticsearch() throws IOException, InterruptedException
    {
        try (final UpgradeScenario scenario = new UpgradeScenario(getUpgradeScenarioConfig()))
        {
            final ACSEnv initialEnv = scenario.startInitialEnvWithSolrBasedSearchService();
            initialEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_BEFORE_INITIAL_REINDEXING);
            initialEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM, FILE_UPLOADED_BEFORE_INITIAL_REINDEXING);

            final AvailabilityProbe probe = initialEnv.getRunningSearchAPIAvailabilityProbe();

            final Elasticsearch elasticsearch = scenario.startElasticsearch();
            Assert.assertFalse(elasticsearch.isIndexCreated());

            final long initialReIndexingUpperBound = initialEnv.getMaxNodeDbId();

            try (ACSEnv mirroredEnv = scenario.startMirroredEnvWitElasticsearchBasedSearchService())
            {
                mirroredEnv.expectNoSearchResult(MAX_TIMEOUT, SEARCH_TERM);
                Assert.assertTrue(mirroredEnv.getMaxNodeDbId() >= initialEnv.getMaxNodeDbId());
                elasticsearch.waitForIndexCreation(MAX_TIMEOUT);
                Assert.assertTrue(elasticsearch.isIndexCreated());
                Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), 0);
                mirroredEnv.expectNoSearchResult(MAX_TIMEOUT, SEARCH_TERM);

                mirroredEnv.startLiveIndexing();
                mirroredEnv.reindexByIds(0, initialReIndexingUpperBound * 2);

                Assert.assertTrue(elasticsearch.getIndexedDocumentCount() > 0);
                mirroredEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM, FILE_UPLOADED_BEFORE_INITIAL_REINDEXING);
            }

            final long documentsCount = elasticsearch.getIndexedDocumentCount();
            initialEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING);

            initialEnv.startLiveIndexing();

            initialEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM,
                    FILE_UPLOADED_BEFORE_INITIAL_REINDEXING,
                    FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING);
            // Live indexing was not running so FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING hasn't been indexed
            Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), documentsCount);

            initialEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING);
            initialEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM,
                    FILE_UPLOADED_BEFORE_INITIAL_REINDEXING,
                    FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING);
            // FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING has been indexed, but we still have a gap.
            // FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING is still not indexed
            Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), documentsCount + 1);

            initialEnv.reindexByIds((long) (initialReIndexingUpperBound * 0.9), 1_000_000_000);
            // Gap has been closed by running reindexing. Both FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING and
            // FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING have been indexed.
            Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), documentsCount + 2);

            initialEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH);
            initialEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM,
                    FILE_UPLOADED_BEFORE_INITIAL_REINDEXING,
                    FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH);
            // Live indexing is still running so FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH should be indexed as well.
            Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), documentsCount + 3);

            initialEnv.setElasticsearchSearchService();

            // Now we use ES. Check if we still have valid result.
            initialEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM,
                    FILE_UPLOADED_BEFORE_INITIAL_REINDEXING,
                    FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH);

            scenario.shutdownSolr();

            // Solr has been stopped. Check if we still have valid result.
            initialEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM,
                    FILE_UPLOADED_BEFORE_INITIAL_REINDEXING,
                    FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH);

            initialEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_AFTER_SWITCHING_TO_ELASTICSEARCH);

            // Check if FILE_UPLOADED_AFTER_SWITCHING_TO_ELASTICSEARCH is part of the search result.
            initialEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM,
                    FILE_UPLOADED_BEFORE_INITIAL_REINDEXING,
                    FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH,
                    FILE_UPLOADED_AFTER_SWITCHING_TO_ELASTICSEARCH);

            final Stats availabilityStats = probe.stop();
            Assert.assertTrue(availabilityStats.getSuccessRatioInPercents() >= 99, "Search was unavailable. Stats: " + availabilityStats);
        }
    }
}
