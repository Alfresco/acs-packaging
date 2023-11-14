package org.alfresco.rest.mtls;

import static org.alfresco.utility.model.FileType.TEXT_PLAIN;

import org.alfresco.rest.MtlsRestTest;
import org.alfresco.rest.model.RestNodeModel;
import org.alfresco.utility.Utility;
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
        FolderModel testFolder = selectSharedFolder(adminUser);

        restClient.authenticateUser(adminUser).configureRequestSpec().addMultiPart("filedata", Utility.getTestResourceFile(TEXT_FILE));
        RestNodeModel rnm = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(testFolder).createNode();

        FileModel testFile = new FileModel(TEXT_FILE);
        testFile.setNodeRef(rnm.getId());

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
