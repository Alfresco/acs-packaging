package org.alfresco.rest;

import javax.net.ssl.SSLHandshakeException;
import java.lang.reflect.Method;

import org.alfresco.rest.core.RestWrapper;
import org.alfresco.utility.LogFactory;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUserAIS;
import org.alfresco.utility.model.SiteModel;
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

    CloseableHttpClient client = HttpClients.createMinimal();

    @BeforeSuite (alwaysRun = true)
    public void checkServerHealth() throws Exception
    {
        super.springTestContextPrepareTestInstance();
        serverHealth.isServerReachable();
        serverHealth.assertServerIsOnline();
        testSiteModel = dataSite.createPublicRandomSite();
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
    public void checkIfMtlsIsEnabledForTransformService()
    {
        Assert.assertThrows(SSLHandshakeException.class, () ->client.execute(new HttpGet("https://localhost:8090")));
    }

    @Test
    public void checkIfMtlsIsEnabledForTransformRouter()
    {
        Assert.assertThrows(SSLHandshakeException.class, () ->client.execute(new HttpGet("https://localhost:8095")));
    }

    @Test
    public void checkIfMtlsIsEnabledForSharedFileStorage()
    {
        Assert.assertThrows(SSLHandshakeException.class, () -> client.execute(new HttpGet("https://localhost:8099")));
    }
}
