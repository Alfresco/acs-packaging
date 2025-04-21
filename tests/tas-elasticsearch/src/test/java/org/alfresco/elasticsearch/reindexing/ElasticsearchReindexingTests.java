package org.alfresco.elasticsearch.reindexing;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.tas.AlfrescoStackInitializer.getImagesConfig;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.alfresco.elasticsearch.SearchQueryService;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.report.log.Step;
import org.apache.http.HttpHost;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.reindex.BulkByScrollResponse;
import org.opensearch.index.reindex.DeleteByQueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * In this test we are verifying end-to-end the reindexer component on Elasticsearch.
 */
@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
                      initializers = AlfrescoStackInitializer.class)

public class ElasticsearchReindexingTests extends AbstractTestNGSpringContextTests
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchReindexingTests.class);

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

    private UserModel testUser;
    private SiteModel testSite;
    private RestHighLevelClient elasticClient;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.isServerReachable();
        serverHealth.assertServerIsOnline();

        Step.STEP("Create a test user and private site containing a document.");

        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createPrivateRandomSite();
        createDocumentWithRandomName();

        Step.STEP("create ES client");
        elasticClient = new RestHighLevelClient(
                RestClient.builder(new HttpHost(AlfrescoStackInitializer.searchEngineContainer.getContainerIpAddress(),
                                                AlfrescoStackInitializer.searchEngineContainer.getFirstMappedPort(),
                                                "http")));
    }

    @Test(groups = TestGroup.SEARCH)
    public void testReindexerFixesBrokenIndex()
    {
        // GIVEN
        cleanUpIndex();
        AlfrescoStackInitializer.liveIndexer.stop();
        String reindexerStartTime = getReindexerStartTimeNow();
        String documentName = createDocumentWithRandomName();
        // Check document not indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        SearchRequest query = req("cm:name:" + documentName + " AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser());

        // WHEN
        // Run reindexer (leaving ALFRESCO_REINDEX_TO_TIME as default).
        AlfrescoStackInitializer.reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                       "ELASTICSEARCH_INDEX_NAME", CUSTOM_ALFRESCO_INDEX,
                       "ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime));

        // THEN
        // Check document indexed.
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);

        // TIDY
        cleanUpIndex();
        AlfrescoStackInitializer.liveIndexer.start();
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndex()
    {
        // GIVEN
        String documentName = createDocumentWithRandomName();
        AlfrescoStackInitializer.liveIndexer.stop();
        cleanUpIndex();

        // WHEN
        // Run reindexer (with default dates to reindex everything).
        AlfrescoStackInitializer.reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                       "ELASTICSEARCH_INDEX_NAME", CUSTOM_ALFRESCO_INDEX));

        // THEN
        // Check document indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        SearchRequest query = req("cm:name:" + documentName + " AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);

        // TIDY
        // Restart ElasticsearchConnector.
        cleanUpIndex();
        AlfrescoStackInitializer.liveIndexer.start();
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndexWithMetadataAndContent()
    {
        // GIVEN
        String reindexerStartTime = getReindexerStartTimeTwentyMinutesAgo();
        AlfrescoStackInitializer.liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        cleanUpIndex();
        // Reindexer requires lifeIndexer to index content.
        AlfrescoStackInitializer.liveIndexer.start();

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        AlfrescoStackInitializer.reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
            "ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime,
            "ALFRESCO_REINDEX_METADATAINDEXINGENABLED", "true",
            "ALFRESCO_REINDEX_CONTENTINDEXINGENABLED", "true",
            "ALFRESCO_REINDEX_PATHINDEXINGENABLED", "false"));

        // THEN
        // Document is still indexed after reindexing.
        SearchRequest query = req("cm:name:'" + documentName + "' AND TEXT:'content'");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndexWithMetadataAndNoContent()
    {
        // GIVEN
        String reindexerStartTime = getReindexerStartTimeTwentyMinutesAgo();
        AlfrescoStackInitializer.liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        cleanUpIndex();

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        AlfrescoStackInitializer.reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
            "ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime,
            "ALFRESCO_REINDEX_METADATAINDEXINGENABLED", "true",
            "ALFRESCO_REINDEX_CONTENTINDEXINGENABLED", "false",
            "ALFRESCO_REINDEX_PATHINDEXINGENABLED", "false"));

        // THEN
        SearchRequest query = req("cm:name:'" + documentName + "' AND TEXT:'content'");
        searchQueryService.expectNoResultsFromQuery(query, dataUser.getAdminUser());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndexWithNoMetadataAndContent()
    {
        // GIVEN
        String reindexerStartTime = getReindexerStartTimeTwentyMinutesAgo();
        AlfrescoStackInitializer.liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        cleanUpIndex();
        // Reindexer requires lifeIndexer to index content.
        AlfrescoStackInitializer.liveIndexer.start();

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        AlfrescoStackInitializer.reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                "ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime,
            "ALFRESCO_REINDEX_METADATAINDEXINGENABLED", "false",
            "ALFRESCO_REINDEX_CONTENTINDEXINGENABLED", "true",
            "ALFRESCO_REINDEX_PATHINDEXINGENABLED", "false"));

        // THEN
        // When not using metadata, document shouldn't be present in Elasticsearch index,
        // since metadata reindexing process is indexing also permissions
        SearchRequest query = req("cm:name:'" + documentName + "' AND cm:name:*");
        searchQueryService.expectNoResultsFromQuery(query, dataUser.getAdminUser());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndexWithMetadataAndNoContentAndPath()
    {
        // GIVEN
        String reindexerStartTime = getReindexerStartTimeTwentyMinutesAgo();
        AlfrescoStackInitializer.liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        cleanUpIndex();

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        AlfrescoStackInitializer.reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                "ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime,
            "ALFRESCO_REINDEX_METADATAINDEXINGENABLED", "true",
            "ALFRESCO_REINDEX_CONTENTINDEXINGENABLED", "false",
            "ALFRESCO_REINDEX_PATHINDEXINGENABLED", "true"));

        // THEN
        SearchRequest query = req("cm:name:'" + documentName + "' AND PATH:'/app:company_home/st:sites/cm:" + testSite + "/cm:documentLibrary/cm:" + documentName + "'");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndexWithMetadataAndContentAndPath()
    {
        // GIVEN
        String reindexerStartTime = getReindexerStartTimeTwentyMinutesAgo();
        AlfrescoStackInitializer.liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        cleanUpIndex();
        // Reindexer requires lifeIndexer to index content.
        AlfrescoStackInitializer.liveIndexer.start();

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        AlfrescoStackInitializer.reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                "ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime,
            "ALFRESCO_REINDEX_METADATAINDEXINGENABLED", "true",
            "ALFRESCO_REINDEX_CONTENTINDEXINGENABLED", "true",
            "ALFRESCO_REINDEX_PATHINDEXINGENABLED", "true"));

        // THEN
        SearchRequest query = req("cm:name:'" + documentName + "' AND TEXT:'content' AND PATH:'/app:company_home/st:sites/cm:" + testSite + "/cm:documentLibrary/cm:" + documentName + "'");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndexWithNoMetadataAndPath()
    {
        // GIVEN
        String reindexerStartTime = getReindexerStartTimeTwentyMinutesAgo();
        AlfrescoStackInitializer.liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        cleanUpIndex();

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        AlfrescoStackInitializer.reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                "ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime,
            "ALFRESCO_REINDEX_METADATAINDEXINGENABLED", "false",
            "ALFRESCO_REINDEX_CONTENTINDEXINGENABLED", "false",
            "ALFRESCO_REINDEX_PATHINDEXINGENABLED", "true"));

        // THEN
        // When not using metadata, document shouldn't be present in Elasticsearch index,
        // since metadata reindexing process is indexing also permissions
        SearchRequest query = req("cm:name:'" + documentName + "' AND cm:name:*");
        searchQueryService.expectNoResultsFromQuery(query, dataUser.getAdminUser());
    }

    @Test (groups = TestGroup.SEARCH)
    public void testPathReindex()
    {
        // GIVEN
        String documentName = createDocumentWithRandomName();
        AlfrescoStackInitializer.liveIndexer.stop();
        cleanUpIndex();

        // WHEN
        // Run reindexer with path indexing enabled (and with default dates to reindex everything).
        AlfrescoStackInitializer.reindex(Map.of("ALFRESCO_REINDEX_PATHINDEXINGENABLED", "true",
                "ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                "ELASTICSEARCH_INDEX_NAME", CUSTOM_ALFRESCO_INDEX));

        // THEN
        // Check path indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        SearchRequest query = req("PATH:\"//" + documentName + "\" AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
        // Also check that the document can be obtained by a path query against the site.
        query = req("PATH:\"//" + testSite.getTitle() + "/documentLibrary/*\" AND cm:name:" + documentName + " AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);

        // TIDY
        AlfrescoStackInitializer.liveIndexer.start();
    }

    @Test (groups = TestGroup.SEARCH)
    public void testPathReindexQueryWithNamespaces()
    {
        // GIVEN
        String documentName = createDocumentWithRandomName();
        AlfrescoStackInitializer.liveIndexer.stop();
        cleanUpIndex();

        // WHEN
        // Run reindexer with path indexing enabled (and with default dates to reindex everything).
        AlfrescoStackInitializer.reindex(Map.of("ALFRESCO_REINDEX_PATHINDEXINGENABLED", "true",
                "ALFRESCO_REINDEX_JOB_NAME", "reindexByDate"));

        // THEN
        // Check path indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        SearchRequest query = req("PATH:\"//cm:" + documentName + "\" AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
        // Also check that the document can be obtained by a path query against the site.
        query = req("PATH:\"//cm:" + testSite.getTitle() + "/cm:documentLibrary/*\" AND cm:name:" + documentName + " AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);

        // TIDY
        AlfrescoStackInitializer.liveIndexer.start();
    }

    private String getReindexerStartTimeNow()
    {
        ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());
        return formatTimeForReindexer(now);
    }

    private String getReindexerStartTimeTwentyMinutesAgo()
    {
        // Initial timestamp for reindexing by date: this will save reindexing time for these tests
        ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());
        // ACS-5044 Increased time to 20 minutes as 10 minutes proved insufficient to prevent intermittent failures
        return formatTimeForReindexer(now.minusMinutes(20));
    }

    private String formatTimeForReindexer(ZonedDateTime zonedDateTime)
    {
        return DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(zonedDateTime);
    }

    private String createDocumentWithRandomName()
    {
        String documentName = "TestFile" + UUID.randomUUID() + ".txt";
        dataContent.usingUser(testUser)
                   .usingSite(testSite)
                   .createContent(new FileModel(documentName, org.alfresco.utility.model.FileType.TEXT_PLAIN, "content"));
        return documentName;
    }

    private void cleanUpIndex()
    {
        try
        {
            DeleteByQueryRequest request = new DeleteByQueryRequest(CUSTOM_ALFRESCO_INDEX);
            request.setQuery(QueryBuilders.matchAllQuery());
            BulkByScrollResponse response = elasticClient.deleteByQuery(request, RequestOptions.DEFAULT);
            LOGGER.debug("deleted {} documents from index", response.getDeleted());
        }
        catch (IOException e)
        {
            fail("Failed to tidy index. " + e);
        }
    }
}
