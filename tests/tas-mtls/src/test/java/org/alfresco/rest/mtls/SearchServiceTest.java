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

    @Test public void testIndexingWithMTLSEnabledTest() throws InterruptedException
    {
        FolderModel folderModel = selectSharedFolder(adminUser);
        RestNodeModel fileNode = null;
        try
        {
            int initialSearchResultsCount = countSearchResults();

            restClient.authenticateUser(adminUser).configureRequestSpec().addMultiPart("filedata", Utility.getTestResourceFile(TEXT_FILE));
            fileNode = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(folderModel).createNode();

            verifyResultsIncreaseWithRetry(initialSearchResultsCount);
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

    private int countSearchResults() {
        RestRequestQueryModel queryModel = new RestRequestQueryModel();
        queryModel.setLanguage("afts");
        queryModel.setQuery("incomprehensible");

        SearchRequest searchRequest = new SearchRequest(queryModel);
        SearchResponse searchResponse = restClient.authenticateUser(adminUser).withSearchAPI().search(searchRequest);

        return searchResponse.getEntries().size();
    }

    private void verifyResultsIncreaseWithRetry(int initialSearchWordCount) throws InterruptedException {
        int retryDelay = 5000;
        int retryLimit = 10;
        for (int i = 0; i < retryLimit; i++) {
            LOGGER.info("Attempt: " + (i+1));
            if (countSearchResults() == initialSearchWordCount + 1) {
                return;
            }
            Thread.sleep(retryDelay);
        }
        Assert.fail("Number of search results didn't increase after uploading a file with keyword");
    }
}
