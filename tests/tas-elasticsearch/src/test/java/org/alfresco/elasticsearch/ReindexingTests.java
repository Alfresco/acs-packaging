package org.alfresco.elasticsearch;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Duration;
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
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
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
    /** Port number to expose alfresco on. */
    private static final int ALFRESCO_PORT = 8082;
    /** Port number to expose postgres on. */
    private static final int PSQL_PORT = 5432;
    /** Port number to expose elasticsearch on. */
    private static final int ELASTICSEARCH_PORT = 9200;

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
    private DockerComposeContainer environment;

    /** Create a user and a private site and wait for these to be indexed. */
    @BeforeClass (alwaysRun = true)
    public void dataPreparation()
    {
        Step.STEP("Create docker-compose deployment.");
        environment = startDockerCompose();
        serverHealth.assertServerIsOnline();

        Step.STEP("Create a test user and private site containing a document.");
        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createPrivateRandomSite();
        createDocument();
    }

    /** This is run as the first test in the class so that we know that no other test has indexed the system documents. */
    @Test (groups = { TestGroup.SEARCH }, priority = -1)
    public void testReindexerIndexesSystemDocuments()
    {
        LOGGER.info("Starting test");
        // GIVEN
        // Check a particular system document is not indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        String queryString = "=cm:name:budget.xls AND =cm:title:\"Web Site Design - Budget\" AND cm:name:*";
        expectResultsFromQuery(queryString, dataUser.getAdminUser());

        // WHEN
        // Run reindexer against the initial documents.
        reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByIds",
                "ELASTICSEARCH_INDEX_NAME", "custom-alfresco-index",
                "ALFRESCO_REINDEX_FROM_ID", "0",
                "ALFRESCO_REINDEX_TO_ID", "1000"));

        // THEN
        // Check system document is indexed.
        expectResultsFromQuery(queryString, dataUser.getAdminUser(), "budget.xls");
        LOGGER.info("End test");
    }

    @Test (groups = { TestGroup.SEARCH })
    public void testReindexerFixesBrokenIndex()
    {
        // GIVEN

        // Stop Elasticsearch.
        // Create document.
        String testStart = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
        String documentName = createDocument();
        // Start Elasticsearch.
        // Check document not indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        String queryString = "=cm:name:" + documentName + " AND cm:name:*";
        expectResultsFromQuery(queryString, dataUser.getAdminUser());

        // WHEN
        // Run reindexer (leaving ALFRESCO_REINDEX_TO_TIME as default).
        reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                "ELASTICSEARCH_INDEX_NAME", "custom-alfresco-index",
                "ALFRESCO_REINDEX_FROM_TIME", testStart));

        // THEN
        // Check document indexed.
        expectResultsFromQuery(queryString, dataUser.getAdminUser(), documentName);
    }

    @Test (groups = { TestGroup.SEARCH })
    public void testRecreateIndex()
    {
        // GIVEN
        // Create document.
        String documentName = createDocument();
        // Stop ElasticsearchConnector.
        // Stop Alfresco.
        // Delete index.
        // Start Alfresco.

        // WHEN
        // Run reindexer (with default dates to reindex everything).
        reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByDate",
                "ELASTICSEARCH_INDEX_NAME", "custom-alfresco-index"));

        // THEN
        // Check document indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        String queryString = "=cm:name:" + documentName + " AND cm:name:*";
        expectResultsFromQuery(queryString, dataUser.getAdminUser(), documentName);

        // TIDY
        // Restart ElasticsearchConnector.
    }

    /**
     * Start the services defined in the docker-compose file.
     *
     * @return The docker-compose environment.
     */
    private DockerComposeContainer startDockerCompose()
    {
        DockerComposeContainer environment = new DockerComposeContainer(new File("src/test/resources/docker-compose-elasticsearch.yml"))
                .withExposedService("alfresco_1", ALFRESCO_PORT, Wait.forHttp("/alfresco")
                                                                     .forStatusCode(200)
                                                                     .withStartupTimeout(Duration.ofMinutes(5)))
                .withExposedService("postgres_1", PSQL_PORT)
                .withExposedService("elasticsearch_1", ELASTICSEARCH_PORT)
                .withLogConsumer("alfresco_1", new Slf4jLogConsumer(LOGGER))
                .withEnv(Map.of("TRANSFORMERS_TAG", "2.3.10",
                        "TRANSFORM_ROUTER_TAG", "1.3.2",
                        "SFS_TAG", "0.13.0",
                        "SOLR6_TAG", "2.0.1",
                        "POSTGRES_TAG", "13.1",
                        "ACTIVEMQ_TAG", "5.16.1",
                        "AIMS_TAG", "1.2",
                        "SYNC_SERVICE_TAG", "3.4.0",
                        "ES_TAG", "7.10.1",
                        "ES_CONNECTOR_TAG", "3.0.0-M1"));
        environment.start();
        // Expose the ports to the reindexing container.
        Testcontainers.exposeHostPorts(PSQL_PORT, ELASTICSEARCH_PORT);
        return environment;
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
                "SPRING_ELASTICSEARCH_REST_URIS", "http://host.testcontainers.internal:" + ELASTICSEARCH_PORT,
                "SPRING_DATASOURCE_URL", "jdbc:postgresql://host.testcontainers.internal:" + PSQL_PORT + "/alfresco",
                "ELASTICSEARCH_INDEX_NAME", "alfresco"));
        env.putAll(envParam);
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

    /**
     * Create a document using in the test site using the test user.
     * @return The randomly generated name of the new document.
     */
    private String createDocument()
    {
        String documentName = "TestFile" + UUID.randomUUID() + ".txt";
        dataContent.usingUser(testUser).usingSite(testSite).createContent(new FileModel(documentName, FileType.TEXT_PLAIN, "content"));
        return documentName;
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
