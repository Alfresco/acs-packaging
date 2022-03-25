package org.alfresco.elasticsearch;

import static org.alfresco.elasticsearch.SearchQueryService.req;

import java.util.Map;

import org.alfresco.dataprep.AlfrescoHttpClient;
import org.alfresco.dataprep.AlfrescoHttpClientFactory;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
                      initializers = AlfrescoStackInitializer.class)
/**
 * In this test we are verifying end-to-end the indexing and CMIS queries against Elasticsearch.
 * In order to test ACLs we created 2 sites and 3 users.
 */
public class ElasticsearchCMISTests extends AbstractTestNGSpringContextTests
{
    private static final String FILE_0_NAME = "test.txt";
    private static final String FILE_1_NAME = "another.txt";
    private static final String FILE_2_NAME = "user1.txt";
    private static final String FILE_3_NAME = "user1Old.txt";

    @Autowired
    private DataUser dataUser;

    @Autowired
    private DataContent dataContent;

    @Autowired
    private DataSite dataSite;

    @Autowired
    private AlfrescoHttpClientFactory alfrescoHttpClientFactory;

    @Autowired
    private ServerHealth serverHealth;

    @Autowired
    protected SearchQueryService searchQueryService;

    private UserModel user1;
    private UserModel user2;
    private UserModel userMultiSite;
    private SiteModel siteModel1;
    private SiteModel siteModel2;

    /**
     * Data will be prepared using the schema below:
     * <p>
     * Site1:
     * - Users: user1, userMultiSite
     * - Documents: FILE_0_NAME (owner: user1), FILE_1_NAME (owner: user1), FILE_3_NAME (owner: user2)
     * <p>
     * Site2:
     * - Users: user2, userMultiSite
     * - Documents: FILE_2_NAME (owner: user2)
     */
    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();

        user1 = dataUser.createRandomTestUser();
        user2 = dataUser.createRandomTestUser();
        userMultiSite = dataUser.createRandomTestUser();

        siteModel1 = dataSite.usingUser(user1).createPrivateRandomSite();
        siteModel2 = dataSite.usingUser(user2).createPrivateRandomSite();

        dataUser.addUserToSite(user2, siteModel1, UserRole.SiteContributor);
        dataUser.addUserToSite(userMultiSite, siteModel1, UserRole.SiteContributor);
        dataUser.addUserToSite(userMultiSite, siteModel2, UserRole.SiteContributor);

        createContent(FILE_0_NAME, "This is the first test", siteModel1, user1);
        createContent(FILE_1_NAME, "This is another TEST file", siteModel1, user1);
        createContent(FILE_2_NAME, "This is another test file", siteModel2, user2);
        createContent(FILE_3_NAME, "This is another Test file", siteModel1, user2);
        //remove the user from site, but he keeps ownership on FILE_3_NAME 
        dataUser.removeUserFromSite(user2, siteModel1);
    }

    @TestRail (section = TestGroup.SEARCH,
            executionType = ExecutionType.REGRESSION,
            description = "Verify that we can perform a basic CMIS query against Elasticsearch.")
    @Test (groups = TestGroup.SEARCH)
    public void basicCMISQuery()
    {
        SearchRequest query = req("cmis", "SELECT cmis:name FROM cmis:document WHERE CONTAINS('*')");
        searchQueryService.expectSomeResultsFromQuery(query, user1);
    }

    @TestRail(section = TestGroup.SEARCH,
              executionType = ExecutionType.REGRESSION,
              description = "Verify that we can perform a basic CMIS query against the DB.")
    @Test(groups = TestGroup.SEARCH)
    public void basicCMISQueryAgainstDB()
    {
        // This query will be handled by the DB rather than ES.
        SearchRequest query = req("cmis", "SELECT cmis:name FROM cmis:document");
        searchQueryService.expectSomeResultsFromQuery(query, user1);
    }

    private FileModel createContent(String filename, String content, SiteModel site, UserModel user)
    {
        FileModel fileModel = new FileModel(filename, FileType.TEXT_PLAIN, content);
        return dataContent.usingUser(user).usingSite(site)
                          .createContent(fileModel);
    }

    private void createNodeWithProperties(SiteModel parentSite, FileModel fileModel, UserModel currentUser, Map<String, Object> properties)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqUrl = client.getApiVersionUrl() + "nodes/" + parentSite.getGuid() + "/children";
        String name = fileModel.getName();

        HttpPost post = new HttpPost(reqUrl);
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("nodeType", "cm:content");

        JSONObject jsonProperties = new JSONObject();
        jsonProperties.putAll(properties);
        body.put("properties", jsonProperties);

        post.setEntity(client.setMessageBody(body));

        // Send Request
        logger.info(String.format("POST: '%s'", reqUrl));
        HttpResponse response = client.execute(currentUser.getUsername(), currentUser.getPassword(), post);
        if (org.apache.http.HttpStatus.SC_CREATED != response.getStatusLine().getStatusCode())
        {
            throw new RuntimeException("Could not create file. Request response: " + client.getParameterFromJSON(response, "briefSummary", "error"));
        }
    }
}
