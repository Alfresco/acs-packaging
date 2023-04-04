package org.alfresco.mtls;

import static org.alfresco.utility.model.FileType.TEXT_PLAIN;

import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestRenditionInfoModel;
import org.alfresco.utility.LogFactory;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUserAIS;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

@ContextConfiguration ("classpath:alfresco-mtls-context.xml")
public class TransformServiceTest extends AbstractTestNGSpringContextTests
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
    private DataContent dataContent;

    protected SiteModel testSiteModel;

    private UserModel adminUser;

    @BeforeSuite (alwaysRun = true)
    public void checkServerHealth() throws Exception
    {
        super.springTestContextPrepareTestInstance();
        serverHealth.assertServerIsOnline();
        testSiteModel = dataSite.createPublicRandomSite();
    }

    @BeforeClass (alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
    }

    @Test
    public void testRenditionWithMTLSEnabledTest()
    {
        FolderModel folderModel = FolderModel.getRandomFolderModel();
        FolderModel testFolder = dataContent.usingUser(adminUser).usingSite(testSiteModel).createFolder(folderModel);

        FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
        FileModel testFile = dataContent.usingUser(adminUser).usingResource(testFolder).createContent(fileModel);

        restClient.authenticateUser(adminUser).withCoreAPI().usingNode(testFile).createNodeRendition("pdf");
        restClient.assertStatusCodeIs(HttpStatus.ACCEPTED);

        Assert.assertEquals(restClient.withCoreAPI().usingNode(testFile).getNodeRenditionUntilIsCreated("pdf").getStatus(), "CREATED");
    }
}
