package org.alfresco.rest.mtls;

import static org.alfresco.utility.model.FileType.TEXT_PLAIN;

import org.alfresco.rest.MtlsRestTest;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.UserModel;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@ContextConfiguration ("classpath:alfresco-mtls-context.xml")
public class TransformServiceTest extends MtlsRestTest
{
    private UserModel adminUser;

    @BeforeClass (alwaysRun = true)
    public void dataPreparation()
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

        String status = restClient.withCoreAPI().usingNode(testFile).getNodeRenditionUntilIsCreated("pdf").getStatus();
        Assert.assertEquals(status, "CREATED");

        restClient.authenticateUser(adminUser).withCoreAPI().usingNode(testFile).createNodeRendition("doclib");
        restClient.assertStatusCodeIs(HttpStatus.ACCEPTED);

        status = restClient.withCoreAPI().usingNode(testFile).getNodeRenditionUntilIsCreated("doclib").getStatus();
        Assert.assertEquals(status, "CREATED");
    }
}
