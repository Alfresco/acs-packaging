package org.alfresco.rest.mtls.elasticsearch;

import java.io.File;
import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.rest.MtlsRestTest;
import org.alfresco.rest.model.RestNodeModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.UserModel;

@ContextConfiguration("classpath:alfresco-mtls-elasticsearch-context.xml")
public class TransformServiceTest extends MtlsRestTest
{
    private static final String TEST_FILE_NAME = "testing-transform-elasticsearch-mtls.txt";
    private static final String TEST_FILE_CONTENT = "Random text for transform tests with Elasticsearch mTLS";

    private UserModel adminUser;
    private File testFile;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws IOException
    {
        adminUser = dataUser.getAdminUser();
        testFile = createTestFile(TEST_FILE_NAME, TEST_FILE_CONTENT);
    }

    @AfterClass(alwaysRun = true)
    public void dataCleanup()
    {
        if (testFile != null && testFile.exists())
        {
            testFile.delete();
        }
    }

    @Test
    public void testRenditionWithMTLSEnabledTest()
    {
        FolderModel testFolder = selectSharedFolder(adminUser);
        FileModel testFileModel = new FileModel(testFile.getName());

        try
        {
            restClient.authenticateUser(adminUser).configureRequestSpec().addMultiPart("filedata", testFile);
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
}

