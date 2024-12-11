package org.alfresco.elasticsearch.reindexing;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.tas.AlfrescoStackInitializer.getImagesConfig;
import static org.alfresco.utility.report.log.Step.STEP;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import org.alfresco.elasticsearch.SearchQueryService;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.network.ServerHealth;
import org.apache.http.HttpHost;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.reindex.BulkByScrollResponse;
import org.opensearch.index.reindex.DeleteByQueryRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * In this test we are verifying end-to-end the reindexer component on Elasticsearch.
 */
@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
                      initializers = AlfrescoStackInitializer.class)
@SuppressWarnings("PMD.JUnit4TestShouldUseTestAnnotation") // these are testng tests
public class ElasticsearchReindexingTests extends AbstractTestNGSpringContextTests
{
    public static final String CUSTOM_ALFRESCO_INDEX = "custom-alfresco-index";

    @Autowired
    private ServerHealth serverHealth;
    @Autowired
    private DataUser dataUser;
    @Autowired
    private DataSite dataSite;
    @Autowired
    private DataContent dataContent;
    @Autowired
    protected SearchQueryService searchQueryService;

    private org.alfresco.utility.model.UserModel testUser;

    private org.alfresco.utility.model.SiteModel testSite;

    private RestHighLevelClient elasticClient;

    /**
     * Create a user and a private site and wait for these to be indexed.
     */
    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.isServerReachable();
        serverHealth.assertServerIsOnline();

        STEP("Create a test user and private site containing a document.");

        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createPrivateRandomSite();
        createDocument();

        STEP("create ES client");
        elasticClient = new RestHighLevelClient(
                RestClient.builder(new HttpHost(AlfrescoStackInitializer.searchEngineContainer.getContainerIpAddress(),
                                                AlfrescoStackInitializer.searchEngineContainer.getFirstMappedPort(),
                                                "http")));

    }

    @Test(groups = TestGroup.SEARCH)
    public void testReindexerFixesBrokenIndex()
    {
        // GIVEN

        // Delete all documents inside Elasticsearch.
        cleanUpIndex();
        //stop live indexing
        AlfrescoStackInitializer.liveIndexer.stop();
        // Create document.

        String testStart = DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(ZonedDateTime.now(Clock.systemUTC()));
        String documentName = createDocument();
        // Check document not indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.

        SearchRequest query = req("cm:name:" + documentName + " AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser());

        // WHEN
        // Run reindexer (leaving ALFRESCO_REINDEX_TO_TIME as default).
        try(GenericContainer reindexingComponent = createReindexContainer(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                "ELASTICSEARCH_INDEX_NAME", CUSTOM_ALFRESCO_INDEX,
                "ALFRESCO_REINDEX_FROM_TIME", testStart))) {
            //Reindex
            reindexingComponent.start();

            // THEN
            // Check document indexed.
            searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
        }
        // TIDY
        // Restart ElasticsearchConnector.
        cleanUpIndex();
        AlfrescoStackInitializer.liveIndexer.start();

    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndex()
    {
        // GIVEN
        // Create document.
        String documentName = createDocument();
        // Stop ElasticsearchConnector.
        AlfrescoStackInitializer.liveIndexer.stop();
        // Delete index documents.
        cleanUpIndex();

        // WHEN
        // Run reindexer (with default dates to reindex everything).
        try(GenericContainer reindexingComponent = createReindexContainer(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                       "ELASTICSEARCH_INDEX_NAME", CUSTOM_ALFRESCO_INDEX))) {
            //Reindex
            reindexingComponent.start();

            // THEN
            // Check document indexed.
            // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
            SearchRequest query = req("cm:name:" + documentName + " AND cm:name:*");
            searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
        }

        // TIDY
        // Restart ElasticsearchConnector.
        cleanUpIndex();
        AlfrescoStackInitializer.liveIndexer.start();

    }

    /**
     * Common testing method for reindexing enabled and disabled features tests.
     * @param metadataIndexingEnabled Reindexing metadata is enabled when true, disabled when false
     * @param contentIndexingEnabled Reindexing content is enabled when true, disabled when false
     * @param pathIndexingEnabled Reindexing path is enabled when true, disabled when false
     * @param queryString Verification query string. It may include a <DOCUMENT_NAME> mark that is replaced by the actual document name created.
     * @param expectingDocNameAsResult Result from verification query string is the name of the document created when true, empty result when false.
     */
    private void internalTestEnabledFeatures(
        Boolean metadataIndexingEnabled,
        Boolean contentIndexingEnabled,
        Boolean pathIndexingEnabled,
        String queryString,
        Boolean expectingDocNameAsResult
    )
    {
        // Initial timestamp for reindexing by date: this will save reindexing time for these tests
        ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());
        // ACS-5044 Increased time to 20 minutes as 10 minutes proved insufficient to prevent intermittent failures
        String testStart = DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(now.minusMinutes(20));

        // GIVEN
        // Stop ElasticsearchConnector
        AlfrescoStackInitializer.liveIndexer.stop();
        // Create document
        String documentName = createDocument();
        // Delete index documents
        cleanUpIndex();
        // Restart ElasticsearchConnector to Index Content
        if (contentIndexingEnabled)
        {
            // Reindexer requires lifeIndexer to index content.
            AlfrescoStackInitializer.liveIndexer.start();
        }

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        try(GenericContainer reindexingComponent = createReindexContainer(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
            "ELASTICSEARCH_INDEX_NAME", CUSTOM_ALFRESCO_INDEX,
            "ALFRESCO_REINDEX_FROM_TIME", testStart,
            "ALFRESCO_REINDEX_METADATAINDEXINGENABLED", metadataIndexingEnabled.toString(),
            "ALFRESCO_REINDEX_CONTENTINDEXINGENABLED", contentIndexingEnabled.toString(),
            "ALFRESCO_REINDEX_PATHINDEXINGENABLED", pathIndexingEnabled.toString()))) {
            //Reindex
            reindexingComponent.start();

            // THEN
            SearchRequest query = req(queryString.replace("<DOCUMENT_NAME>", documentName));

            if (expectingDocNameAsResult) {
                searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
            } else {
                searchQueryService.expectNoResultsFromQuery(query, dataUser.getAdminUser());
            }
        }

    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndexWithMetadataAndContent()
    {
        internalTestEnabledFeatures(true, true, false,
            "cm:name:'<DOCUMENT_NAME>' AND TEXT:'content'", true);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndexWithMetadataAndNoContent()
    {
        internalTestEnabledFeatures(true, false, false,
            "cm:name:'<DOCUMENT_NAME>' AND TEXT:'content'", false);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndexWithNoMetadataAndContent()
    {
        // When not using metadata, document shouldn't be present in Elasticsearch index,
        // since metadata reindexing process is indexing also permissions
        internalTestEnabledFeatures(false, true, false,
            "cm:name:'<DOCUMENT_NAME>' AND cm:name:*", false);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndexWithMetadataAndNoContentAndPath()
    {
        internalTestEnabledFeatures(true, false, true,
            "cm:name:'<DOCUMENT_NAME>' AND PATH:'/app:company_home/st:sites/cm:" + testSite + "/cm:documentLibrary/cm:<DOCUMENT_NAME>'", true);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndexWithMetadataAndContentAndPath()
    {
        internalTestEnabledFeatures(true, true, true,
            "cm:name:'<DOCUMENT_NAME>' AND TEXT:'content' " +
                "AND PATH:'/app:company_home/st:sites/cm:" + testSite + "/cm:documentLibrary/cm:<DOCUMENT_NAME>'", true);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndexWithNoMetadataAndPath()
    {
        // When not using metadata, document shouldn't be present in Elasticsearch index,
        // since metadata reindexing process is indexing also permissions
        internalTestEnabledFeatures(false, false, true,
            "cm:name:'<DOCUMENT_NAME>' AND cm:name:*", false);
    }

    @Test (groups = TestGroup.SEARCH)
    public void testPathReindex()
    {
        // GIVEN
        // Create document.
        String documentName = createDocument();
        // Stop ElasticsearchConnector.
        AlfrescoStackInitializer.liveIndexer.stop();
        // Delete index documents.
        cleanUpIndex();

        // WHEN
        // Run reindexer with path indexing enabled (and with default dates to reindex everything).
        try(GenericContainer reindexingComponent = createReindexContainer(Map.of("ALFRESCO_REINDEX_PATHINDEXINGENABLED", "true",
                "ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                "ELASTICSEARCH_INDEX_NAME", CUSTOM_ALFRESCO_INDEX))) {
            //Reindex
            reindexingComponent.start();

            // THEN
            // Check path indexed.
            // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
            SearchRequest query = req("PATH:\"//" + documentName + "\" AND cm:name:*");
            searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
            // Also check that the document can be obtained by a path query against the site.
            query = req("PATH:\"//" + testSite.getTitle() + "/documentLibrary/*\" AND cm:name:" + documentName + " AND cm:name:*");
            searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
        }

        // TIDY
        // Restart ElasticsearchConnector.
        AlfrescoStackInitializer.liveIndexer.start();
    }

    @Test (groups = TestGroup.SEARCH)
    public void testPathReindexQueryWithNamespaces()
    {
        // GIVEN
        // Create document.
        String documentName = createDocument();
        // Stop ElasticsearchConnector.
        AlfrescoStackInitializer.liveIndexer.stop();
        // Delete index documents.
        cleanUpIndex();

        // WHEN
        // Run reindexer with path indexing enabled (and with default dates to reindex everything).
        try(GenericContainer reindexingComponent = createReindexContainer(Map.of("ALFRESCO_REINDEX_PATHINDEXINGENABLED", "true",
                "ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                "ELASTICSEARCH_INDEX_NAME", CUSTOM_ALFRESCO_INDEX))) {
            //Reindex
            reindexingComponent.start();

            // THEN
            // Check path indexed.
            // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
            SearchRequest query = req("PATH:\"//cm:" + documentName + "\" AND cm:name:*");
            searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
            // Also check that the document can be obtained by a path query against the site.
            query = req("PATH:\"//cm:" + testSite.getTitle() + "/cm:documentLibrary/*\" AND cm:name:" + documentName + " AND cm:name:*");
            searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
        }

        // TIDY
        // Restart ElasticsearchConnector.
        AlfrescoStackInitializer.liveIndexer.start();
    }

    /**
     * Run the alfresco-elasticsearch-reindexing container.
     *
     * @param envParam Any environment variables to override from the defaults.
     * @return reindex container
     */
    private GenericContainer createReindexContainer(Map<String, String> envParam)
    {
        // Run the reindexing container.
        Map<String, String> env = AlfrescoStackInitializer.getReindexEnvBasic();
        env.putAll(envParam);

        return new GenericContainer(getImagesConfig().getReIndexingImage())
                                    .withEnv(env)
                                    .withNetwork(AlfrescoStackInitializer.network)
                                    .withStartupCheckStrategy(new IndefiniteWaitOneShotStartupCheckStrategy());
    }

    /**
     * Create a document using in the test site using the test user.
     *
     * @return The randomly generated name of the new document.
     */
    private String createDocument()
    {
        String documentName = "TestFile" + UUID.randomUUID() + ".txt";
        dataContent.usingUser(testUser)
                   .usingSite(testSite)
                   .createContent(new org.alfresco.utility.model.FileModel(documentName, org.alfresco.utility.model.FileType.TEXT_PLAIN, "content"));
        return documentName;
    }

    private void cleanUpIndex()
    {
        try
        {
            DeleteByQueryRequest request = new DeleteByQueryRequest(CUSTOM_ALFRESCO_INDEX);
            request.setQuery(QueryBuilders.matchAllQuery());
            BulkByScrollResponse response = elasticClient.deleteByQuery(request, RequestOptions.DEFAULT);
            STEP("Deleted " + response.getDeleted() + " documents from index");
        }
        catch (IOException e)
        {
            fail("Failed to tidy index. " + e);
        }
    }

}
