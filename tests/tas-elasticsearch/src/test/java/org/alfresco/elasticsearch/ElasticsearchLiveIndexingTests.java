package org.alfresco.elasticsearch;

import org.alfresco.dataprep.AlfrescoHttpClient;
import org.alfresco.dataprep.AlfrescoHttpClientFactory;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchNodeModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.Utility;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
                      initializers = AlfrescoStackInitializer.class)
/**
 * In this test we are verifying end-to-end the indexing and search in Elasticsearch.
 * In order to test ACLs we created 2 sites and 3 users. 
 */
public class ElasticsearchLiveIndexingTests extends AbstractTestNGSpringContextTests
{
    private static final String FILE_0_NAME = "test.txt";
    private static final String FILE_1_NAME = "another.txt";
    private static final String FILE_2_NAME = "user1.txt";
    private static final String FILE_3_NAME = "user1Old.txt";
    public static final String BEFORE_1970_TXT = "before1970.txt";

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
    private RestWrapper client;

    private UserModel userSite1;
    private UserModel userSite2;
    private UserModel userMultiSite;
    private SiteModel siteModel1;
    private SiteModel siteModel2;

    /**
     * Data will be prepared using the schema below:
     * <p>
     * Site1:
     * - Users: userSite1, userMultiSite
     * - Documents: FILE_0_NAME (owner: userSite1), FILE_1_NAME (owner: userSite1), FILE_3_NAME (owner: userSite2)
     * <p>
     * Site2:
     * - Users: userSite2, userMultiSite
     * - Documents: FILE_2_NAME (owner: userSite2)
     */
    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();

        userSite1 = dataUser.createRandomTestUser();
        userSite2 = dataUser.createRandomTestUser();
        userMultiSite = dataUser.createRandomTestUser();

        siteModel1 = dataSite.usingUser(userSite1).createPrivateRandomSite();
        siteModel2 = dataSite.usingUser(userSite2).createPrivateRandomSite();

        dataUser.addUserToSite(userSite2, siteModel1, UserRole.SiteContributor);
        dataUser.addUserToSite(userMultiSite, siteModel1, UserRole.SiteContributor);
        dataUser.addUserToSite(userMultiSite, siteModel2, UserRole.SiteContributor);

        createContent(FILE_0_NAME, "This is the first test", siteModel1, userSite1);
        createContent(FILE_1_NAME, "This is another TEST file", siteModel1, userSite1);
        createContent(FILE_2_NAME, "This is another test file", siteModel2, userSite2);
        createContent(FILE_3_NAME, "This is another Test file", siteModel1, userSite2);
        //remove the user from site, but he keeps ownership on FILE_3_NAME 
        dataUser.removeUserFromSite(userSite2, siteModel1);
    }

    @TestRail(section = TestGroup.SEARCH,
              executionType = ExecutionType.REGRESSION,
              description = "Verify that the include parameter work with Elasticsearch search as expected.")
    @Test(groups = TestGroup.SEARCH)
    public void searchCanFindAFileUsingIncludeParameter() throws Exception
    {
        Utility.sleep(1000, 10000, () -> {
            RestRequestQueryModel queryReq = new RestRequestQueryModel();
            queryReq.setQuery("first");

            SearchRequest queryWithoutIncludes = new SearchRequest();
            queryWithoutIncludes.setQuery(queryReq);

            SearchResponse searchWithoutIncludes = client.authenticateUser(userSite1).withSearchAPI().search(queryWithoutIncludes);

            //Verify before that fields are absent if not included
            SearchNodeModel searchNodeModelWithoutIncludes = searchWithoutIncludes.getEntries().get(0).getModel();
            assertNull(searchNodeModelWithoutIncludes.getProperties());
            assertNull(searchNodeModelWithoutIncludes.getPath());
            assertNull(searchNodeModelWithoutIncludes.getAspectNames());
            assertNull(searchNodeModelWithoutIncludes.getAllowableOperations());
            assertNull(searchNodeModelWithoutIncludes.getPermissions());
            assertNull(searchNodeModelWithoutIncludes.getAssociation());
            assertNull(searchNodeModelWithoutIncludes.isLocked());
            assertNull(searchNodeModelWithoutIncludes.isLink());

            SearchRequest queryWithIncludes = new SearchRequest();
            // A full list of all fields that can be included is declared in constant:
            // org.alfresco.rest.api.search.impl.SearchMapper.PERMITTED_INCLUDES
            queryWithIncludes.setInclude(asList("properties", "path", "aspectNames", "isLocked", "allowableOperations",
                                                "permissions", "isLink", "association"));
            queryWithIncludes.setQuery(queryReq);

            SearchResponse searchWithIncludes = client.authenticateUser(userSite1).withSearchAPI().search(queryWithIncludes);

            //Verify that all fields in the include parameter are in the search response
            SearchNodeModel searchNodeModelWithIncludes = searchWithIncludes.getEntries().get(0).getModel();
            assertNotNull(searchNodeModelWithIncludes.getProperties());
            assertNotNull(searchNodeModelWithIncludes.getPath());
            assertNotNull(searchNodeModelWithIncludes.getAspectNames());
            assertNotNull(searchNodeModelWithIncludes.getAllowableOperations());
            assertNotNull(searchNodeModelWithIncludes.getPermissions());
            assertNotNull(searchNodeModelWithIncludes.getAssociation());
            assertNotNull(searchNodeModelWithIncludes.isLocked());
            assertNotNull(searchNodeModelWithIncludes.isLink());
        });
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
            assertResponseAndResult(search, FILE_0_NAME);
        });
    }

    @TestRail(section = TestGroup.SEARCH,
              executionType = ExecutionType.REGRESSION,
              description = "Verify that Elasticsearch search works as expected using a user that has access to only one site.")
    @Test(groups = TestGroup.SEARCH)
    public void searchCanFindFilesOnASite() throws Exception
    {
        Utility.sleep(1000, 10000, () -> {
            SearchRequest query = new SearchRequest();
            RestRequestQueryModel queryReq = new RestRequestQueryModel();
            queryReq.setQuery("test");
            query.setQuery(queryReq);

            SearchResponse search = client.authenticateUser(userSite1).withSearchAPI().search(query);

            assertResponseAndResult(search, FILE_0_NAME, FILE_1_NAME, FILE_3_NAME);

        });
    }

    @TestRail(section = TestGroup.SEARCH,
              executionType = ExecutionType.REGRESSION,
              description = "Verify that Elasticsearch search works as expected when the user can search a file because he is the owner.")
    @Test(groups = TestGroup.SEARCH)
    public void searchCanFindAFileOnMultipleSitesWithOwner() throws Exception
    {
        Utility.sleep(1000, 10000, () -> {
            SearchRequest query = new SearchRequest();
            RestRequestQueryModel queryReq = new RestRequestQueryModel();
            queryReq.setQuery("test");
            query.setQuery(queryReq);

            SearchResponse search = client.authenticateUser(userSite2).withSearchAPI().search(query);

            //even if the user has access only to a site with 1 document the search will returns two documents 
            //because he is the owner of a document on a site where he hasn't any permission
            assertResponseAndResult(search, FILE_3_NAME, FILE_2_NAME);
        });
    }

    @TestRail(section = TestGroup.SEARCH,
              executionType = ExecutionType.REGRESSION,
              description = "Verify that Elasticsearch search works as expected when a user has permission on multiple sites.")
    @Test(groups = TestGroup.SEARCH)
    public void searchCanFindAFileOnMultipleSites() throws Exception
    {
        Utility.sleep(1000, 10000, () -> {
            SearchRequest query = new SearchRequest();
            RestRequestQueryModel queryReq = new RestRequestQueryModel();
            queryReq.setQuery("test");
            query.setQuery(queryReq);

            SearchResponse search = client.authenticateUser(userMultiSite).withSearchAPI().search(query);

            assertResponseAndResult(search, FILE_0_NAME, FILE_1_NAME, FILE_3_NAME, FILE_2_NAME);
        });
    }

    @TestRail(section = TestGroup.SEARCH,
              executionType = ExecutionType.REGRESSION,
              description = "Verify that the simpler Elasticsearch search works as expected.")
    @Test(groups = TestGroup.SEARCH)
    public void indexAndSearchForDateBefore1970() throws Exception
    {
        //Elasticsearch doesn't accept numbers for dates before 1970, so we create and search for a specific document in order to verify that.
        createNodeWithProperties(siteModel1, new FileModel(BEFORE_1970_TXT, FileType.TEXT_PLAIN), userSite1,
                                 Map.of("cm:from", -2637887000L));

        Utility.sleep(1000, 10000, () -> {
            SearchRequest query = new SearchRequest();
            RestRequestQueryModel queryReq = new RestRequestQueryModel();
            queryReq.setQuery("cm:from:1969-12-01T11:15:13Z");
            query.setQuery(queryReq);

            SearchResponse search = client.authenticateUser(userSite1).withSearchAPI().search(query);

            assertResponseAndResult(search, BEFORE_1970_TXT);
        });
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

    public <T> boolean listEqualsIgnoreOrder(List<T> list1, List<T> list2)
    {
        return new HashSet<>(list1).equals(new HashSet<>(list2));
    }

    private void assertResponseAndResult(SearchResponse actual, String... expected)
    {
        client.assertStatusCodeIs(HttpStatus.OK);

        List<SearchNodeModel> entries = actual.getEntries();
        client.assertStatusCodeIs(HttpStatus.OK);
        assertEquals(entries.size(), expected.length);
        List<String> result = entries.stream().map(SearchNodeModel::getModel).peek(item -> assertTrue(item.isFile()))
                                     .map(SearchNodeModel::getName).collect(Collectors.toList());
        List<String> expectedList = asList(expected);
        assertTrue(listEqualsIgnoreOrder(result, expectedList),
                   "Result " + result + " doesn't contain " + expectedList);
    }

}
