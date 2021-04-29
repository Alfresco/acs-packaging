package org.alfresco.elasticsearch;

import static org.testng.Assert.assertEquals;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchNodeModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.report.log.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** TODO Decide whether this test class needs to be in a maven submodule of its own. */
@ContextConfiguration ("classpath:alfresco-elasticsearch-context.xml")
public class ReindexingTests extends AbstractTestNGSpringContextTests
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ReindexingTests.class);
    /** The maximum time to let the reindex process run for in milliseconds. */
    private static final long MAX_REINDEX_DURATION = 60 * 1000;
    /** The maximum time to wait for the reindex process to shutdown in milliseconds. */
    private static final long MAX_SHUTDOWN_DURATION = 5 * 1000;
    /** A unique name for a document. */
    private static final String DOCUMENT_NAME = "TestFile" + UUID.randomUUID() + ".txt";

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
    private UserModel testUser;
    private SiteModel testSite;

    /** Create a user and a private site and wait for these to be indexed. */
    @BeforeClass (alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();

        Step.STEP("Create a test user and private site containing a document.");
        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createPrivateRandomSite();
        dataContent.usingUser(testUser).usingSite(testSite).createContent(new FileModel(DOCUMENT_NAME, FileType.TEXT_PLAIN, "content"));
    }

    /** Warning - this test looks for the system documents and so can only be run once against a system before the documents are indexed and it starts to fail. */
    @Test (groups = { TestGroup.SEARCH })
    public void testReindexerIndexesSystemDocuments()
    {
        LOGGER.error("Starting test");
        // GIVEN
        // Check a particular system document is not indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        String queryString = "=cm:name:budget.xls AND =cm:title:\"Web Site Design - Budget\" AND cm:name:*";
        expectResultsFromQuery(queryString, dataUser.getAdminUser());

        // WHEN
        // Run reindexer against the initial documents.
        reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByIds",
                "ALFRESCO_REINDEX_FROM_ID", "0",
                "ALFRESCO_REINDEX_TO_ID", "1000"));

        // THEN
        // Check system document is indexed.
        expectResultsFromQuery(queryString, dataUser.getAdminUser(), "budget.xls");
        LOGGER.error("End test");
    }

    @Test
    public void testReindexerFixesBrokenIndex()
    {
        // GIVEN
        // Stop Elasticsearch.
        // Create document.
        // Start Elasticsearch.
        // Check document not indexed.

        // WHEN
        // Run reindexer.

        // THEN
        // Check document indexed.
    }

    @Test
    public void testRecreateIndex()
    {
        // GIVEN
        // Create document.
        // Stop ElasticsearchConnector.
        // Stop Alfresco.
        // Delete index.
        // Start Alfresco.

        // WHEN
        // Run reindexer.

        // THEN
        // Check document indexed.

        // TIDY
        // Restart ElasticsearchConnector.
    }

    /**
     * Run the alfresco-elasticsearch-reindexing container.
     *
     * @param envParam Any environment variables to override from the defaults.
     */
    private void reindex(Map<String, String> envParam)
    {
        // Run the reindexing container.
        Map<String, String> env = new HashMap<>(Map.of(
                "SPRING_ELASTICSEARCH_REST_URIS", "http://host.testcontainers.internal:9200",
                "SPRING_DATASOURCE_URL", "jdbc:postgresql://host.testcontainers.internal:5432/alfresco",
                "ELASTICSEARCH_INDEX_NAME", "alfresco"));
        env.putAll(envParam);
        Testcontainers.exposeHostPorts(5432, 9200);
        Testcontainers.exposeHostPorts(5432, 9200);
        GenericContainer reindexingComponent = new GenericContainer("quay.io/alfresco/alfresco-elasticsearch-reindexing:latest")
                        .withEnv(env)
                        .withLogConsumer(new Slf4jLogConsumer(LOGGER));
        reindexingComponent.start();
        long startTime = new Date().getTime();

        // Wait for the reindexing process to finish.
        while (reindexingComponent.isRunning() && (new Date().getTime() - startTime < MAX_REINDEX_DURATION))
        {
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }

        // Ensure the container is shutdown before continuing.
        if (reindexingComponent.isRunning())
        {
            reindexingComponent.stop();
            while (reindexingComponent.isRunning() && (new Date().getTime() - startTime < MAX_REINDEX_DURATION + MAX_SHUTDOWN_DURATION))
            {
                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void expectResultsFromQuery(String queryString, UserModel user,  String... expected)
    {
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery(queryString);
        query.setQuery(queryReq);
        SearchResponse response = client.authenticateUser(user).withSearchAPI().search(query);
        assertSearchResults(response, expected);
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
}
