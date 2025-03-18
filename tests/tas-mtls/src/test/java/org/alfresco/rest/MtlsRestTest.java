package org.alfresco.rest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.net.ssl.SSLHandshakeException;

import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchNodeModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.LogFactory;
import org.alfresco.utility.data.DataUserAIS;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.UserModel;

@ContextConfiguration("classpath:alfresco-mtls-context.xml")
public abstract class MtlsRestTest extends AbstractTestNGSpringContextTests
{
    private static final Logger LOGGER = LogFactory.getLogger();

    @Autowired
    protected MtlsTestProperties mtlsTestProperties;
    @Autowired
    protected DataUserAIS dataUser;
    @Autowired
    protected RestWrapper restClient;

    private CloseableHttpClient client = HttpClients.createMinimal();

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
        Assert.assertThrows(SSLHandshakeException.class, () -> client.execute(new HttpGet("https://localhost:8083/solr")));
    }

    protected FolderModel selectSharedFolder(UserModel user)
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

    protected File createTestFile(String fileName, String fileContent) throws IOException
    {
        Path filePath = Paths.get(fileName);
        try (BufferedWriter fileWriter = Files.newBufferedWriter(Paths.get(fileName)))
        {
            fileWriter.write(fileContent);
        }

        return filePath.toFile();
    }
}
