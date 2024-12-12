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
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.reindex.BulkByScrollResponse;
import org.opensearch.index.reindex.DeleteByQueryRequest;
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
@SuppressWarnings("PMD.JUnit4TestShouldUseTestAnnotation") // these are testng tests
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
    private RestHighLevelClient elasticClient;

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
        elasticClient = new RestHighLevelClient(
                RestClient.builder(new HttpHost(searchEngineContainer.getContainerIpAddress(),
                                                searchEngineContainer.getFirstMappedPort(),
                                                "http")));
    }

    @Test(groups = TestGroup.SEARCH)
    public void testReindexerFixesBrokenIndex()
    {
        // GIVEN
        String reindexerStartTime = getReindexerStartTime();
        cleanUpIndex();
        liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        // Check document not indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        SearchRequest query = req("cm:name:" + documentName + " AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser());

        // WHEN
        // Run reindexer (leaving ALFRESCO_REINDEX_TO_TIME as default).
        reindex(Map.of("ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime));

        // THEN
        // Check document indexed.
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndex()
    {
        // GIVEN
        String reindexerStartTime = getReindexerStartTime();
        liveIndexer.start();
        String documentName = createDocumentWithRandomName();
        liveIndexer.stop();
        cleanUpIndex();

        // WHEN
        // Run reindexer (with default dates to reindex everything).
        reindex(Map.of("ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime));

        // THEN
        // Check document indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        SearchRequest query = req("cm:name:" + documentName + " AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndexWithMetadataAndContent()
    {
        // GIVEN
        String reindexerStartTime = getReindexerStartTime();
        liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        cleanUpIndex();
        // Reindexer requires lifeIndexer to index content.
        liveIndexer.start();

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        reindex(Map.of("ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime,
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
        String reindexerStartTime = getReindexerStartTime();
        liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        cleanUpIndex();

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        reindex(Map.of("ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime,
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
        String reindexerStartTime = getReindexerStartTime();
        liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        cleanUpIndex();
        // Reindexer requires lifeIndexer to index content.
        liveIndexer.start();

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        reindex(Map.of("ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime,
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
        String reindexerStartTime = getReindexerStartTime();
        liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        cleanUpIndex();

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        reindex(Map.of("ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime,
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
        String reindexerStartTime = getReindexerStartTime();
        liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        cleanUpIndex();
        // Reindexer requires lifeIndexer to index content.
        liveIndexer.start();

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        reindex(Map.of("ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime,
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
        String reindexerStartTime = getReindexerStartTime();
        liveIndexer.stop();
        String documentName = createDocumentWithRandomName();
        cleanUpIndex();

        // WHEN
        // Run reindexer leaving ALFRESCO_REINDEX_TO_TIME as default
        reindex(Map.of("ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime,
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
        String reindexerStartTime = getReindexerStartTime();
        liveIndexer.start();
        String documentName = createDocumentWithRandomName();
        liveIndexer.stop();
        cleanUpIndex();

        // WHEN
        // Run reindexer with path indexing enabled (and with default dates to reindex everything).
        reindex(Map.of("ALFRESCO_REINDEX_PATHINDEXINGENABLED",
                "true", "ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime));

        // THEN
        // Check path indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        SearchRequest query = req("PATH:\"//" + documentName + "\" AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
        // Also check that the document can be obtained by a path query against the site.
        query = req("PATH:\"//" + testSite.getTitle() + "/documentLibrary/*\" AND cm:name:" + documentName + " AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
    }

    @Test (groups = TestGroup.SEARCH)
    public void testPathReindexQueryWithNamespaces()
    {
        // GIVEN
        String reindexerStartTime = getReindexerStartTime();
        liveIndexer.start();
        String documentName = createDocumentWithRandomName();
        liveIndexer.stop();
        cleanUpIndex();

        // WHEN
        // Run reindexer with path indexing enabled (and with default dates to reindex everything).
        reindex(Map.of("ALFRESCO_REINDEX_PATHINDEXINGENABLED", "true",
                "ALFRESCO_REINDEX_FROM_TIME", reindexerStartTime));

        // THEN
        // Check path indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        SearchRequest query = req("PATH:\"//cm:" + documentName + "\" AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
        // Also check that the document can be obtained by a path query against the site.
        query = req("PATH:\"//cm:" + testSite.getTitle() + "/cm:documentLibrary/*\" AND cm:name:" + documentName + " AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), documentName);
    }

    private String getReindexerStartTime()
    {
        // Initial timestamp for reindexing by date: this will save reindexing time for these tests
        ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());
        return DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(now.minusMinutes(1));
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
