package org.alfresco.elasticsearch;

import com.google.common.collect.Sets;
import org.alfresco.dataprep.AlfrescoHttpClient;
import org.alfresco.dataprep.AlfrescoHttpClientFactory;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchNodeModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.Utility;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.report.log.Step;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;

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
    protected RestWrapper client;
    @Autowired
    private AlfrescoHttpClientFactory alfrescoHttpClientFactory;

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

        Step.STEP("Create a test user and private site containing a document.");

        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createPrivateRandomSite();
        createDocument();

        Step.STEP("create ES client");
        elasticClient = new RestHighLevelClient(
                RestClient.builder(new HttpHost(AlfrescoStackInitializer.elasticsearch.getContainerIpAddress(),
                                                AlfrescoStackInitializer.elasticsearch.getFirstMappedPort(),
                                                "http")));

    }

    /**
     * This is run as the first test in the class so that we know that no other test has indexed the system documents.
     */
    @Test(groups = TestGroup.SEARCH, priority = -1)
    public void testReindexerIndexesSystemDocuments() throws Exception
    {
        // GIVEN
        // Check a particular system document is NOT indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        String queryString = "=cm:name:budget.xls AND =cm:title:\"Web Site Design - Budget\" AND cm:name:*";
        expectResultsFromQuery(queryString, dataUser.getAdminUser());

        // WHEN
        // Run reindexer against the initial documents.
        reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByIds",
                       "ELASTICSEARCH_INDEX_NAME", CUSTOM_ALFRESCO_INDEX,
                       "ALFRESCO_REINDEX_FROM_ID", "0",
                       "ALFRESCO_REINDEX_TO_ID", "1000"));

        // THEN
        // Check system document is indexed.
        expectResultsFromQuery(queryString, dataUser.getAdminUser(), "budget.xls");
    }

    @Test(groups = TestGroup.SEARCH)
    public void testReindexerFixesBrokenIndex() throws Exception
    {
        // GIVEN

        // Delete all documents inside Elasticsearch.
        cleanUpIndex();
        //stop live indexing
        AlfrescoStackInitializer.mediationLiveIndexer.stop();
        // Create document.

        String testStart = DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(ZonedDateTime.now(Clock.systemUTC()));
        String documentName = createDocument().getName();
        // Check document not indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.

        String queryString = "=cm:name:" + documentName + " AND cm:name:*";
        expectResultsFromQuery(queryString, dataUser.getAdminUser());

        // WHEN
        // Run reindexer (leaving ALFRESCO_REINDEX_TO_TIME as default).
        reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                       "ELASTICSEARCH_INDEX_NAME", CUSTOM_ALFRESCO_INDEX,
                       "ALFRESCO_REINDEX_FROM_TIME", testStart));

        // THEN
        // Check document indexed.
        expectResultsFromQuery(queryString, dataUser.getAdminUser(), documentName);

        // TIDY
        // Restart ElasticsearchConnector.
        AlfrescoStackInitializer.mediationLiveIndexer.start();

    }

    @Test(groups = TestGroup.SEARCH)
    public void testRecreateIndex() throws Exception
    {
        // GIVEN
        // Create document.
        String documentName = createDocument().getName();
        // Stop ElasticsearchConnector.
        AlfrescoStackInitializer.mediationLiveIndexer.stop();
        // Delete index documents.
        cleanUpIndex();

        // WHEN
        // Run reindexer (with default dates to reindex everything).
        reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                       "ELASTICSEARCH_INDEX_NAME", CUSTOM_ALFRESCO_INDEX));

        // THEN
        // Check document indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        String queryString = "=cm:name:" + documentName + " AND cm:name:*";
        expectResultsFromQuery(queryString, dataUser.getAdminUser(), documentName);

        // TIDY
        // Restart ElasticsearchConnector.
        AlfrescoStackInitializer.mediationLiveIndexer.start();
    }


    @Test(groups = TestGroup.SEARCH)
    public void testContentReindex() throws Exception
    {
        // GIVEN
        // Create document.
        String documentName = createDocument().getName();
        // Stop ElasticsearchConnector.
        AlfrescoStackInitializer.mediationLiveIndexer.stop();
        // Delete index documents.
        cleanUpIndex();

        // WHEN
        // Run reindexer (with default dates to reindex everything).
        reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                "ELASTICSEARCH_INDEX_NAME", CUSTOM_ALFRESCO_INDEX));

        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        String contentQueryString = "=cm:name:" + documentName + " AND =cm:content:content AND cm:name:*";
        expectResultsFromQuery(contentQueryString, dataUser.getAdminUser(), documentName);


        // TIDY
        // Restart ElasticsearchConnector.
        AlfrescoStackInitializer.mediationLiveIndexer.start();
    }


    @Test(groups = TestGroup.SEARCH)
    public void testReindexerUpdateOldContent() throws Exception
    {
        // GIVEN
        // Create document.
        FileModel document = createDocument();
        String documentName = document.getName();
        // Stop ElasticsearchConnector.
        AlfrescoStackInitializer.mediationLiveIndexer.stop();
        updateContent("new", document);
        // Delete index documents.

        // WHEN
        // Run reindexer (with default dates to reindex everything).
        reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                "ELASTICSEARCH_INDEX_NAME", CUSTOM_ALFRESCO_INDEX));

        // THEN
        // Check document indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        String contentQueryString = "=cm:name:" + documentName + " AND =cm:content:new AND cm:name:*";
        expectResultsFromQuery(contentQueryString, dataUser.getAdminUser(), documentName);

        // TIDY
        // Restart ElasticsearchConnector.
        AlfrescoStackInitializer.mediationLiveIndexer.start();
    }



    /**
     * Run the alfresco-elasticsearch-reindexing container.
     *
     * @param envParam Any environment variables to override from the defaults.
     */
    private void reindex(Map<String, String> envParam)
    {
        // Run the reindexing container.
        Map<String, String> env = new HashMap<>(
                Map.of("SPRING_ELASTICSEARCH_REST_URIS", "http://elasticsearch:9200",
                       "SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/alfresco",
                        "SPRING_ACTIVEMQ_BROKERURL", "nio://activemq:61616",
                        "ELASTICSEARCH_INDEX_NAME", CUSTOM_ALFRESCO_INDEX));
        env.putAll(envParam);

        try (GenericContainer reindexingComponent = new GenericContainer("quay.io/alfresco/alfresco-elasticsearch-reindexing:latest")
                                                            .withEnv(env)
                                                            .withNetwork(AlfrescoStackInitializer.network)
                                                            .withStartupCheckStrategy(
                                                                    new IndefiniteWaitOneShotStartupCheckStrategy()))
        {
            reindexingComponent.start();
        }

    }

    /**
     * Create a document using in the test site using the test user.
     *
     * @return The randomly generated name of the new document.
     */
    private FileModel createDocument()
    {
        return createDocument("content");
    }

    private FileModel createDocument(String content)
    {
        String documentName = "TestFile" + UUID.randomUUID() + ".txt";
        return  dataContent.usingUser(testUser)
                .usingSite(testSite)
                .createContent(new org.alfresco.utility.model.FileModel(documentName, org.alfresco.utility.model.FileType.TEXT_PLAIN, content));
    }




    private void updateContent(String content, FileModel fileModel)
    {

        AlfrescoHttpClient httpClient = alfrescoHttpClientFactory.getObject();

        fileModel.setContent(content);
        dataContent.usingUser(testUser)
                .usingSite(testSite)
                .updateContent(httpClient, fileModel);

    }




    private void expectResultsFromQuery(String queryString, org.alfresco.utility.model.UserModel user, String... expected) throws Exception
    {
        Utility.sleep(1000, 10000, () -> {
            SearchRequest query = new SearchRequest();
            RestRequestQueryModel queryReq = new RestRequestQueryModel();
            queryReq.setQuery(queryString);
            query.setQuery(queryReq);
            SearchResponse response = client.authenticateUser(user)
                                            .withSearchAPI()
                                            .search(query);
            assertSearchResults(response, expected);
        });
    }

    private void assertSearchResults(SearchResponse actual, String... expected)
    {
        Set<String> result = actual.getEntries().stream()
                                   .map(SearchNodeModel::getModel)
                                   .map(SearchNodeModel::getName)
                                   .collect(Collectors.toSet());
        Set<String> expectedList = Sets.newHashSet(expected);
        assertEquals(result, expectedList, "Unexpected search results.");
    }


    private void cleanUpIndex() throws IOException
    {
        DeleteByQueryRequest request = new DeleteByQueryRequest("custom-alfresco-index");
        request.setQuery(QueryBuilders.matchAllQuery());
        BulkByScrollResponse response = elasticClient.deleteByQuery(request, RequestOptions.DEFAULT);
        LOGGER.debug("deleted {} documents from index", response.getDeleted());
    }

}
