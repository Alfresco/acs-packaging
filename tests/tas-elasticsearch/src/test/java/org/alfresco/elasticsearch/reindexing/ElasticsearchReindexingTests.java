package org.alfresco.elasticsearch.reindexing;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.tas.AlfrescoStackInitializer.CUSTOM_ALFRESCO_INDEX;
import static org.alfresco.tas.AlfrescoStackInitializer.liveIndexer;
import static org.alfresco.tas.AlfrescoStackInitializer.reindex;
import static org.alfresco.tas.AlfrescoStackInitializer.searchEngineContainer;
import static org.alfresco.utility.model.FileType.TEXT_PLAIN;
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
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch.core.DeleteByQueryRequest;
import org.opensearch.client.opensearch.core.DeleteByQueryResponse;
import org.opensearch.client.opensearch.indices.RefreshRequest;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;
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
@SuppressWarnings({"PMD.JUnit4TestShouldUseTestAnnotation", "PMD.JUnitTestsShouldIncludeAssert"}) // these are testng tests
public class ElasticsearchReindexingTests extends AbstractTestNGSpringContextTests
{
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
    private OpenSearchClient elasticClient;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.isServerReachable();
        serverHealth.assertServerIsOnline();

        STEP("Create a test user and private site containing a document.");

        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createPrivateRandomSite();
        createDocumentWithRandomName();

        STEP("create ES client");

        RestClient httpClient = RestClient.builder(new HttpHost(searchEngineContainer.getContainerIpAddress(),
                                                                searchEngineContainer.getFirstMappedPort(), "http"))
                    .build();
      
        OpenSearchTransport transport = new RestClientTransport(httpClient, new JacksonJsonpMapper());
        elasticClient = new OpenSearchClient(transport);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testReindexerFixesBrokenIndex()
    {
        // GIVEN
        cleanUpIndex();
        liveIndexer.stop();
        String reindexerStartTime = getReindexerStartTimeNow();
        String documentName = createDocumentWithRandomName();
        // Check document not indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        SearchRequest query = req("cm:name:" + documentName + " AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser());

        // WHEN
        // Run reindexer (leaving ALFRESCO_REINDEX_TO_TIME as default).
        reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                "ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime));

        // THEN
        // Check document indexed.
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);

        // TIDY
        cleanUpIndex();
        liveIndexer.start();
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndex()
    {
        // GIVEN
        String documentName = createDocumentWithRandomName();
        liveIndexer.stop();
        cleanUpIndex();

        // WHEN
        // Run reindexer (with default dates to reindex everything).
        reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate"));

        // THEN
        // Check document indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        SearchRequest query = req("cm:name:" + documentName + " AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);

        // TIDY
        // Restart ElasticsearchConnector.
        cleanUpIndex();
        liveIndexer.start();
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndexWithMetadataAndContent()
    {
        // GIVEN
        String reindexerStartTime = getReindexerStartTimeTwentyMinutesAgo();
        liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        cleanUpIndex();
        // Reindexer requires lifeIndexer to index content.
        liveIndexer.start();

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
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
        liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        cleanUpIndex();

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
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
        liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        cleanUpIndex();
        // Reindexer requires lifeIndexer to index content.
        liveIndexer.start();

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
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
        liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        cleanUpIndex();

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                "ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime,
            "ALFRESCO_REINDEX_METADATAINDEXINGENABLED", "true",
            "ALFRESCO_REINDEX_CONTENTINDEXINGENABLED", "false",
            "ALFRESCO_REINDEX_PATHINDEXINGENABLED", "true"));

        // THEN
        SearchRequest query = req("cm:name:'%s' AND PATH:'/app:company_home/st:sites/cm:%s/cm:documentLibrary/cm:%s'".formatted(documentName, testSite, documentName));
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndexWithMetadataAndContentAndPath()
    {
        // GIVEN
        String reindexerStartTime = getReindexerStartTimeTwentyMinutesAgo();
        liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        cleanUpIndex();
        // Reindexer requires lifeIndexer to index content.
        liveIndexer.start();

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                "ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime,
            "ALFRESCO_REINDEX_METADATAINDEXINGENABLED", "true",
            "ALFRESCO_REINDEX_CONTENTINDEXINGENABLED", "true",
            "ALFRESCO_REINDEX_PATHINDEXINGENABLED", "true"));

        // THEN
        SearchRequest query = req("cm:name:'%s' AND TEXT:'content' AND PATH:'/app:company_home/st:sites/cm:%s/cm:documentLibrary/cm:%s'".formatted(documentName, testSite, documentName));
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndexWithNoMetadataAndPath()
    {
        // GIVEN
        String reindexerStartTime = getReindexerStartTimeTwentyMinutesAgo();
        liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        cleanUpIndex();

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
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
        liveIndexer.stop();
        cleanUpIndex();

        // WHEN
        // Run reindexer with path indexing enabled (and with default dates to reindex everything).
        reindex(Map.of("ALFRESCO_REINDEX_PATHINDEXINGENABLED", "true",
                "ALFRESCO_REINDEX_JOB_NAME", "reindexByDate"));

        // THEN
        // Check path indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        SearchRequest query = req("PATH:\"//" + documentName + "\" AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
        // Also check that the document can be obtained by a path query against the site.
        query = req("PATH:\"//" + testSite.getTitle() + "/documentLibrary/*\" AND cm:name:" + documentName + " AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);

        // TIDY
        liveIndexer.start();
    }

    @Test (groups = TestGroup.SEARCH)
    public void testPathReindexQueryWithNamespaces()
    {
        // GIVEN
        String documentName = createDocumentWithRandomName();
        liveIndexer.stop();
        cleanUpIndex();

        // WHEN
        // Run reindexer with path indexing enabled (and with default dates to reindex everything).
        reindex(Map.of("ALFRESCO_REINDEX_PATHINDEXINGENABLED", "true",
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
        liveIndexer.start();
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
                   .createContent(new FileModel(documentName, TEXT_PLAIN, "content"));
        return documentName;
    }

    private void cleanUpIndex()
    {
        try
        {
            RefreshRequest refreshRequest = new RefreshRequest.Builder().index(CUSTOM_ALFRESCO_INDEX).build();
            elasticClient.indices().refresh(refreshRequest);

            DeleteByQueryRequest request = new DeleteByQueryRequest.Builder().index(CUSTOM_ALFRESCO_INDEX)
                    .query(QueryBuilders.matchAll()
                            .build()
                            .toQuery())
                    .build();

            DeleteByQueryResponse response = elasticClient.deleteByQuery(request);
            STEP("Deleted " + response.deleted() + " documents from index");
        }
        catch (IOException e)
        {
            fail("Failed to tidy index. " + e);
        }
    }
}
