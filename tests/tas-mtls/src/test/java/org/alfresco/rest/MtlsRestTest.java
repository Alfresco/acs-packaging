package org.alfresco.rest;

import java.lang.reflect.Method;

import org.alfresco.rest.core.RestWrapper;
import org.alfresco.utility.LogFactory;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUserAIS;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.network.ServerHealth;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

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
}
