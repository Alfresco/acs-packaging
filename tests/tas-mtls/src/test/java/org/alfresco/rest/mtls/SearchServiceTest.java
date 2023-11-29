package org.alfresco.rest.mtls;

import org.alfresco.rest.MtlsRestTest;
import org.alfresco.rest.model.RestNodeModel;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.LogFactory;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.UserModel;
import org.slf4j.Logger;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

@ContextConfiguration("classpath:alfresco-mtls-context.xml")
public class SearchServiceTest extends MtlsRestTest
{
    private static final String TEST_FILE_NAME = "testing-search-mtls.txt";
    private static final String TEST_FILE_KEYWORD = "incomprehensible";
    private static final String TEST_FILE_CONTENT = "We need to verify indexing working in solr/elasticsearch to do that we need to upload this \n"
                                                    + "text file and search with a word inside it,\n"
                                                    + "like \"" + TEST_FILE_KEYWORD + "\" to verify it has been indexed properly.";

    private static final Logger LOGGER = LogFactory.getLogger();

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
        if (testFile != null && testFile.exists()) {
            testFile.delete();
        }
    }

    @Test
    public void testIndexingWithMTLSEnabledTest() throws InterruptedException
    {
        FolderModel folderModel = selectSharedFolder(adminUser);
        RestNodeModel fileNode = null;
        try
        {
            int initialSearchResultsCount = countSearchResults(TEST_FILE_KEYWORD);

            restClient.authenticateUser(adminUser).configureRequestSpec().addMultiPart("filedata", testFile);
            fileNode = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(folderModel).createNode();

            verifyResultsIncreaseWithRetry(TEST_FILE_KEYWORD, initialSearchResultsCount);
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

    private int countSearchResults(String keyword) {
        RestRequestQueryModel queryModel = new RestRequestQueryModel();
        queryModel.setLanguage("afts");
        queryModel.setQuery(keyword);

        SearchRequest searchRequest = new SearchRequest(queryModel);
        SearchResponse searchResponse = restClient.authenticateUser(adminUser).withSearchAPI().search(searchRequest);

        return searchResponse.getEntries().size();
    }

    private void verifyResultsIncreaseWithRetry(String keyword, int initialSearchWordCount) throws InterruptedException {
        int retryDelay = 5000;
        int retryLimit = 10;
        for (int i = 0; i < retryLimit; i++) {
            LOGGER.info("Attempt: " + (i+1));
            if (countSearchResults(keyword) > initialSearchWordCount) {
                return;
            }
            Thread.sleep(retryDelay);
        }
        Assert.fail("Number of search results didn't increase after uploading a file with keyword");
    }
}
