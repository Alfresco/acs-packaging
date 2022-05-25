package org.alfresco.elasticsearch.upgrade;

import static java.time.Duration.ofMinutes;

import static org.alfresco.elasticsearch.AlfrescoStackInitializer.getImagesConfig;

import java.io.IOException;
import java.net.URL;

import org.alfresco.elasticsearch.EnvHelper;
import org.alfresco.elasticsearch.upgrade.AvailabilityProbe.Stats;
import org.testng.Assert;
import org.testng.annotations.Test;

public class FromSolrUpgradeTest
{
    private static final URL TEST_FILE_URL = FromSolrUpgradeTest.class.getResource("babekyrtso.pdf");
    private static final String SEARCH_TERM = "babekyrtso";
    private static final String FILE_UPLOADED_BEFORE_INITIAL_REINDEXING = "before-initial-re-indexing.pdf";
    private static final String FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING = "before-live-indexing.pdf";
    private static final String FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING = "after-live-indexing.pdf";
    private static final String FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH = "before-switch.pdf";
    private static final String FILE_UPLOADED_AFTER_SWITCHING_TO_ELASTICSEARCH = "after-switch.pdf";

    @Test
    public void testZeroDowntimeUpgradeFromSolrToElasticsearch() throws IOException
    {
        try (final UpgradeScenario scenario = new UpgradeScenario(getUpgradeScenarioConfig()))
        {
            final ACSEnv initialEnv = scenario.startInitialEnvWithSolrBasedSearchService();
            initialEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_BEFORE_INITIAL_REINDEXING);
            initialEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM, FILE_UPLOADED_BEFORE_INITIAL_REINDEXING);

            final AvailabilityProbe probe = initialEnv.startSearchAPIAvailabilityProbe();

            final Elasticsearch elasticsearch = scenario.startElasticsearch();
            Assert.assertFalse(elasticsearch.isIndexCreated());

            final long initialReIndexingUpperBound;

            try (ACSEnv mirroredEnv = scenario.startMirroredEnvWitElasticsearchBasedSearchService())
            {
                mirroredEnv.expectNoSearchResult(ofMinutes(1), SEARCH_TERM);
                Assert.assertEquals(initialEnv.getMaxNodeDbId(), mirroredEnv.getMaxNodeDbId());

                Assert.assertTrue(elasticsearch.isIndexCreated());
                Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), 0);
                mirroredEnv.expectNoSearchResult(ofMinutes(1), SEARCH_TERM);

                mirroredEnv.startLiveIndexing();

                initialReIndexingUpperBound = mirroredEnv.getMaxNodeDbId();
                mirroredEnv.reindexByIds(0, initialReIndexingUpperBound);

                Assert.assertTrue(elasticsearch.getIndexedDocumentCount() > 0);
                mirroredEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM, FILE_UPLOADED_BEFORE_INITIAL_REINDEXING);
            }

            final long documentsCount = elasticsearch.getIndexedDocumentCount();
            initialEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING);

            initialEnv.startLiveIndexing();

            initialEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM,
                    FILE_UPLOADED_BEFORE_INITIAL_REINDEXING,
                    FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING);
            //Live indexing was not running so FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING hasn't been indexed
            Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), documentsCount);

            initialEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING);
            initialEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM,
                    FILE_UPLOADED_BEFORE_INITIAL_REINDEXING,
                    FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING);
            //FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING has been indexed, but we still have a gap.
            // FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING is still not indexed
            Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), documentsCount + 1);

            initialEnv.reindexByIds(initialReIndexingUpperBound, 1_000_000_000);
            //Gap has been closed by running reindexing. Both FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING and
            // FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING have been indexed.
            Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), documentsCount + 2);

            initialEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH);
            initialEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM,
                    FILE_UPLOADED_BEFORE_INITIAL_REINDEXING,
                    FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH);
            //Live indexing is still running so FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH should be indexed as well.
            Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), documentsCount + 3);

            initialEnv.setElasticsearchSearchService();

            //Now we use ES. Check if we still have valid result.
            initialEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM,
                    FILE_UPLOADED_BEFORE_INITIAL_REINDEXING,
                    FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH);

            scenario.shutdownSolr();

            //Solr has been stopped. Check if we still have valid result.
            initialEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM,
                    FILE_UPLOADED_BEFORE_INITIAL_REINDEXING,
                    FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH);

            initialEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_AFTER_SWITCHING_TO_ELASTICSEARCH);

            //Check if FILE_UPLOADED_AFTER_SWITCHING_TO_ELASTICSEARCH is part of the search result.
            initialEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM,
                    FILE_UPLOADED_BEFORE_INITIAL_REINDEXING,
                    FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH,
                    FILE_UPLOADED_AFTER_SWITCHING_TO_ELASTICSEARCH);

            final Stats availabilityStats = probe.stop();
            Assert.assertTrue(availabilityStats.getSuccessRatioInPercents() >= 99, "Search was unavailable. Stats: " + availabilityStats);
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

