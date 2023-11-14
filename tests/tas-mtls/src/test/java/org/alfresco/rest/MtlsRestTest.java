package org.alfresco.rest;

import javax.net.ssl.SSLHandshakeException;
import java.lang.reflect.Method;

import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchNodeModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.LogFactory;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUserAIS;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
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

@ContextConfiguration ("classpath:alfresco-mtls-context.xml")
public abstract class MtlsRestTest extends AbstractTestNGSpringContextTests
{
    protected static final String TEXT_FILE = "testing.txt";
    private static Logger LOGGER = LogFactory.getLogger();

    @Autowired
    protected ServerHealth serverHealth;

    @Autowired
    protected RestWrapper restClient;

    @Autowired
    protected DataUserAIS dataUser;

    @Autowired
    protected DataSite dataSite;

    @Autowired
    protected DataContent dataContent;

    protected SiteModel testSiteModel;

    String keystoreLocation = "/Users/Marcin.Strankowski/Projects/acs-packaging/keystores/testClient/testClient.keystore";
    String keystorePassword = "password";
    String keystoreType = "JCEKS";
    String truststoreLocation = "/Users/Marcin.Strankowski/Projects/acs-packaging/keystores/testClient/testClient.truststore";
    String truststorePassword = "password";
    String truststoreType = "JCEKS";

    CloseableHttpClient client = HttpClients.createMinimal();

    @BeforeSuite (alwaysRun = true)
    public void checkServerHealth() throws Exception
    {
        super.springTestContextPrepareTestInstance();

        //Needed to communicate with mTLS Repository
        SSLConfig sslConfig = SSLConfig.sslConfig()
                .keyStore(keystoreLocation, keystorePassword)
                .keystoreType(keystoreType)
                .trustStore(truststoreLocation, truststorePassword)
                .trustStoreType(truststoreType);
        RestAssured.config = RestAssured.config().sslConfig(sslConfig);
    }

    @BeforeMethod (alwaysRun=true)
    public void showStartTestInfo(Method method)
    {
        LOGGER.info(String.format("*** STARTING Test: [%s] ***",method.getName()));
    }

    @AfterMethod (alwaysRun=true)
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

    protected FolderModel selectSharedFolder(UserModel user) {
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
}
