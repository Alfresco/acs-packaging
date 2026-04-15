package org.alfresco.rest;

import static org.awaitility.Awaitility.await;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;
import javax.net.ssl.SSLHandshakeException;

import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestNodeModel;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchNodeModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.LogFactory;
import org.alfresco.utility.data.DataUserAIS;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.UserModel;

public abstract class MtlsRestTest extends AbstractTestNGSpringContextTests
{
    private static final Logger LOGGER = LogFactory.getLogger();

    private static final String SEARCH_TEST_FILE_NAME = "testing-search-mtls.txt";
    private static final String SEARCH_TEST_FILE_KEYWORD = "incomprehensible";
    private static final String SEARCH_TEST_FILE_CONTENT = "We need to verify indexing working to do that we need to upload this \n"
            + "text file and search with a word inside it,\n"
            + "like \"" + SEARCH_TEST_FILE_KEYWORD + "\" to verify it has been indexed properly.";

    private static final String TRANSFORM_TEST_FILE_NAME = "testing-transform-mtls.txt";
    private static final String TRANSFORM_TEST_FILE_CONTENT = "Random text for transform tests";

    private static final Duration INDEXING_POLL_INTERVAL = Duration.ofSeconds(5);
    private static final Duration INDEXING_TIMEOUT = Duration.ofMinutes(1);

    @Autowired
    protected MtlsTestProperties mtlsTestProperties;
    @Autowired
    protected DataUserAIS dataUser;
    @Autowired
    protected RestWrapper restClient;

    private static final CloseableHttpClient client = HttpClients.createMinimal();
    private UserModel adminUser;
    private File searchTestFile;
    private File transformTestFile;

    @BeforeSuite(alwaysRun = true)
    public void setupSSLConfig() throws Exception
    {
        super.springTestContextPrepareTestInstance();

        // Needed to communicate with mTLS Repository
        SSLConfig sslConfig = SSLConfig.sslConfig()
                .keyStore(mtlsTestProperties.getKeystoreLocation(), mtlsTestProperties.getKeystorePassword())
                .keystoreType(mtlsTestProperties.getKeystoreType())
                .trustStore(mtlsTestProperties.getTruststoreLocation(), mtlsTestProperties.getTruststorePassword())
                .trustStoreType(mtlsTestProperties.getTruststoreType());

        if (mtlsTestProperties.isDisableHostnameVerification())
        {
            sslConfig = sslConfig.allowAllHostnames();
        }

        RestAssured.config = RestAssured.config().sslConfig(sslConfig);
    }

    @AfterSuite(alwaysRun = true)
    public void closeHttpClient() throws Exception
    {
        client.close();
    }

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws IOException
    {
        adminUser = dataUser.getAdminUser();
        searchTestFile = createTestFile(UUID.randomUUID() + SEARCH_TEST_FILE_NAME, SEARCH_TEST_FILE_CONTENT);
        transformTestFile = createTestFile(TRANSFORM_TEST_FILE_NAME, TRANSFORM_TEST_FILE_CONTENT);
    }

    @AfterClass(alwaysRun = true)
    public void dataCleanup()
    {
        deleteIfExists(searchTestFile);
        deleteIfExists(transformTestFile);
    }

    @BeforeMethod(alwaysRun = true)
    public void showStartTestInfo(Method method)
    {
        LOGGER.info(String.format("*** STARTING Test: [%s] ***", method.getName()));
    }

    @AfterMethod(alwaysRun = true)
    public void showEndTestInfo(Method method)
    {
        LOGGER.info(String.format("*** ENDING Test: [%s] ***", method.getName()));
    }

    @Test
    public void checkIfMtlsIsEnabledForRepository()
    {
        Assert.assertThrows(SSLHandshakeException.class, () -> client.execute(new HttpGet("https://localhost:8443")));
    }

    @Test
    public void checkIfMtlsIsEnabledForTransformService()
    {
        Assert.assertThrows(SSLHandshakeException.class, () -> client.execute(new HttpGet("https://localhost:8090")));
    }

    @Test
    public void checkIfMtlsIsEnabledForTransformRouter()
    {
        Assert.assertThrows(SSLHandshakeException.class, () -> client.execute(new HttpGet("https://localhost:8095")));
    }

    @Test
    public void checkIfMtlsIsEnabledForSharedFileStorage()
    {
        Assert.assertThrows(SSLHandshakeException.class, () -> client.execute(new HttpGet("https://localhost:8099")));
    }

    @Test
    public void checkIfMtlsIsEnabledForSearchEngine()
    {
        Assert.assertThrows(SSLHandshakeException.class, () -> client.execute(new HttpGet(mtlsTestProperties.getSearchEngineMtlsUrl())));
    }

    @Test
    public void testIndexingWithMTLSEnabled()
    {
        FolderModel folderModel = selectSharedFolder(adminUser);
        RestNodeModel fileNode = null;
        try
        {
            int initialSearchResultsCount = countSearchResults(SEARCH_TEST_FILE_KEYWORD);

            restClient.authenticateUser(adminUser).configureRequestSpec().addMultiPart("filedata", searchTestFile);
            fileNode = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(folderModel).createNode();

            verifyResultsIncreaseWithRetry(SEARCH_TEST_FILE_KEYWORD, initialSearchResultsCount);
        }
        finally
        {
            if (fileNode != null && fileNode.getId() != null)
            {
                restClient.authenticateUser(adminUser).withCoreAPI().usingNode(folderModel).deleteNode(fileNode.getId());
            }
        }
    }
    
    @Test
    public void testRenditionWithMTLSEnabled()
    {
        FolderModel testFolder = selectSharedFolder(adminUser);
        FileModel testFileModel = new FileModel(transformTestFile.getName());

        try
        {
            restClient.authenticateUser(adminUser).configureRequestSpec().addMultiPart("filedata", transformTestFile);
            RestNodeModel rnm = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(testFolder).createNode();
            testFileModel.setNodeRef(rnm.getId());

            restClient.authenticateUser(adminUser).withCoreAPI().usingNode(testFileModel).createNodeRendition("pdf");
            restClient.assertStatusCodeIs(HttpStatus.ACCEPTED);

            String status = restClient.withCoreAPI().usingNode(testFileModel).getNodeRenditionUntilIsCreated("pdf").getStatus();
            Assert.assertEquals(status, "CREATED");

            restClient.authenticateUser(adminUser).withCoreAPI().usingNode(testFileModel).createNodeRendition("doclib");
            restClient.assertStatusCodeIs(HttpStatus.ACCEPTED);

            status = restClient.withCoreAPI().usingNode(testFileModel).getNodeRenditionUntilIsCreated("doclib").getStatus();
            Assert.assertEquals(status, "CREATED");
        }
        finally
        {
            if (testFileModel.getNodeRef() != null)
            {
                restClient.authenticateUser(adminUser).withCoreAPI().usingNode(testFolder).deleteNode(testFileModel.getNodeRef());
            }
        }
    }

    private FolderModel selectSharedFolder(UserModel user)
    {
        FolderModel folderModel = new FolderModel("Shared");

        RestRequestQueryModel rrqm = new RestRequestQueryModel();
        rrqm.setLanguage("afts");
        rrqm.setQuery("TYPE:\"cm:folder\" AND =name:\"Shared\"");
        SearchResponse searchResponse = restClient.authenticateUser(user).withSearchAPI().search(new SearchRequest(rrqm));
        SearchNodeModel folderEntry = searchResponse.getEntries().get(0);
        String folderNodeRef = folderEntry.getModel().getId();
        folderModel.setNodeRef(folderNodeRef);

        return folderModel;
    }

    private int countSearchResults(String keyword)
    {
        RestRequestQueryModel queryModel = new RestRequestQueryModel();
        queryModel.setLanguage("afts");
        queryModel.setQuery(keyword);

        SearchRequest searchRequest = new SearchRequest(queryModel);
        SearchResponse searchResponse = restClient.authenticateUser(adminUser).withSearchAPI().search(searchRequest);

        return searchResponse.getEntries() != null ? searchResponse.getEntries().size() : 0;
    }

    private void verifyResultsIncreaseWithRetry(String keyword, int initialSearchWordCount)
    {
        await().atMost(INDEXING_TIMEOUT)
                .pollInterval(INDEXING_POLL_INTERVAL)
                .conditionEvaluationListener(condition -> LOGGER.info("Elapsed: {} ms, remaining: {} ms", condition.getElapsedTimeInMS(), condition.getRemainingTimeInMS()))
                .untilAsserted(() -> Assert.assertTrue(countSearchResults(keyword) > initialSearchWordCount,
                        "Number of search results didn't increase after uploading a file with keyword"));
    }

    private File createTestFile(String fileName, String fileContent) throws IOException
    {
        Path filePath = Paths.get(fileName);
        try (BufferedWriter fileWriter = Files.newBufferedWriter(filePath))
        {
            fileWriter.write(fileContent);
        }

        return filePath.toFile();
    }

    private void deleteIfExists(File file)
    {
        if (file != null && file.exists())
        {
            file.delete();
        }
    }
}
