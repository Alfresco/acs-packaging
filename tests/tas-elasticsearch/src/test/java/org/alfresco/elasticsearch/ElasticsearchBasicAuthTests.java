package org.alfresco.elasticsearch;

import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.Utility;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.*;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.alfresco.elasticsearch.ElasticsearchLiveIndexingTests.assertResponseAndResult;

/**
 * Basic test for Elasticsearch server with Basic Authentication.
 * The aim of this class is to test that Basic Authentication is working as expected,
 * feature testing is expected to be covered by ElasticsearchLiveIndexingTests class.
 */
@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
    initializers = AlfrescoStackInitializerESBasicAuth.class)
public class ElasticsearchBasicAuthTests extends AbstractTestNGSpringContextTests
{

    @Autowired
    private ServerHealth serverHealth;

    @Autowired
    private DataUser dataUser;

    @Autowired
    private DataContent dataContent;

    @Autowired
    private DataSite dataSite;

    @Autowired
    private RestWrapper client;

    private UserModel userSite1;
    private SiteModel siteModel1;

    private static final String FILE_0_NAME = "test.txt";

    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();
        userSite1 = dataUser.createRandomTestUser();
        siteModel1 = dataSite.usingUser(userSite1).createPrivateRandomSite();
        createContent(FILE_0_NAME, "This is the first test", siteModel1, userSite1);
    }

    private FileModel createContent(String filename, String content, SiteModel site, UserModel user)
    {
        FileModel fileModel = new FileModel(filename, FileType.TEXT_PLAIN, content);
        return dataContent.usingUser(user).usingSite(site)
            .createContent(fileModel);
    }

    @TestRail(section = TestGroup.SEARCH,
        executionType = ExecutionType.REGRESSION,
        description = "Verify that the simpler Elasticsearch search works as expected.")
    @Test(groups = TestGroup.SEARCH)
    public void searchCanFindAFile() throws Exception
    {

        Utility.sleep(1000, 10000, () -> {
          SearchRequest query = new SearchRequest();
          RestRequestQueryModel queryReq = new RestRequestQueryModel();
          queryReq.setQuery("first");
          query.setQuery(queryReq);

          SearchResponse search = client.authenticateUser(userSite1).withSearchAPI().search(query);

          // this test must found only one documents, while documents in the system are four because
          // only one contains the word "first".
          assertResponseAndResult(client, search, FILE_0_NAME);
      });
    }

}
