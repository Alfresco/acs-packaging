package org.alfresco.rest.mtls;

import org.alfresco.rest.MtlsRestTest;
import org.alfresco.rest.model.RestNodeModel;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.Utility;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.UserModel;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@ContextConfiguration("classpath:alfresco-mtls-context.xml")
public class SearchServiceTest extends MtlsRestTest
{
    public static final String TEXT_FILE = "testing-search-mtls.txt";

    private UserModel adminUser;

    @BeforeClass(alwaysRun = true) public void dataPreparation()
    {
        adminUser = dataUser.getAdminUser();
    }

    @Test public void testRenditionWithMTLSEnabledTest() throws InterruptedException
    {
        FolderModel folderModel = selectSharedFolder(adminUser);
        RestNodeModel fileNode = null;
        try
        {
            int initialSearchWordCount = searchWordCount();

            restClient.authenticateUser(adminUser).configureRequestSpec().addMultiPart("filedata", Utility.getTestResourceFile(TEXT_FILE));
            fileNode = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(folderModel).createNode();

            Thread.sleep(10000);

            int postUploadSearchWordCount = searchWordCount();

            Assert.assertEquals(postUploadSearchWordCount, initialSearchWordCount + 1);
        }
        finally
        {
            //Clean up file for easier local retries of test
            if (fileNode != null)
            {
                restClient.authenticateUser(adminUser).withCoreAPI().usingNode(folderModel).deleteNode(fileNode.getId());
            }
        }
    }

    private int searchWordCount() {
        RestRequestQueryModel queryModel = new RestRequestQueryModel();
        queryModel.setLanguage("afts");
        queryModel.setQuery("incomprehensible");

        SearchRequest searchRequest = new SearchRequest(queryModel);
        SearchResponse searchResponse = restClient.authenticateUser(adminUser).withSearchAPI().search(searchRequest);

        return searchResponse.getEntries().size();
    }
}
