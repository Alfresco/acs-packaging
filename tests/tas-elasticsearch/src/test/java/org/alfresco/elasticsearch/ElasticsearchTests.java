package org.alfresco.elasticsearch;

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
import org.alfresco.utility.model.*;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@ContextConfiguration("classpath:alfresco-elasticsearch-context.xml")
/**
 * In this test we are verifying end-to-end the indexing and search in Elasticsearch.
 * In order to test ACLs we created 2 sites and 3 users. 
 */ 
public class ElasticsearchTests extends AbstractTestNGSpringContextTests
{
    private static final String INDEX_NAME = "alfresco";
    private static final String FILE_0_NAME = "test.txt";
    private static final String FILE_1_NAME = "another.txt";
    private static final String FILE_2_NAME = "user1.txt";
    private static final String FILE_3_NAME = "user1Old.txt";

    @Autowired
    public DataUser dataUser;
    @Autowired
    public DataContent dataContent;
    @Autowired
    public DataSite dataSite;

    @Autowired
    protected ServerHealth serverHealth;

    @Autowired
    protected RestWrapper client;

    private UserModel userSite1;
    private RestHighLevelClient elasticClient;
    private UserModel userSite2;
    private UserModel userMultiSite;
    private FileModel sampleContent;
    private SiteModel siteModel1;
    private SiteModel siteModel2;

    /**
     * Data will be prepared using the schema below:
     * 
     * Site1:
     *  - Users: userSite1, userMultiSite
     *  - Documents: FILE_0_NAME (owner: userSite2), FILE_1_NAME (owner: userSite2), FILE_3_NAME (owner: userSite1)
     *  
     * Site2:
     *  - Users: userSite2, userMultiSite
     *  - Documents: FILE_2_NAME (owner: userSite2)
     *  
     */
    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();

        elasticClient = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
        initIndex();

        userSite1 = dataUser.createRandomTestUser();
        userSite2 = dataUser.createRandomTestUser();
        userMultiSite = dataUser.createRandomTestUser();

        siteModel1 = dataSite.usingUser(userSite1).createPrivateRandomSite();
        siteModel2 = dataSite.usingUser(userSite2).createPrivateRandomSite();

        dataUser.addUserToSite(userSite2, siteModel1, UserRole.SiteContributor);
        dataUser.addUserToSite(userMultiSite, siteModel1, UserRole.SiteContributor);
        dataUser.addUserToSite(userMultiSite, siteModel2, UserRole.SiteContributor);

        sampleContent = createContent(FILE_0_NAME, "This is a test", siteModel1, userSite1);
        createContent(FILE_1_NAME, "This is another file", siteModel1, userSite1);
        createContent(FILE_2_NAME, "This is another file", siteModel2, userSite2);
        createContent(FILE_3_NAME, "This is another file", siteModel1, userSite2);
        //remove the user from site, but he keeps ownership on FILE_3_NAME 
        dataUser.removeUserFromSite(userSite2, siteModel1);
    }
    
    @AfterClass(alwaysRun=true)
    public void cleanup(){
        deleteIndex(INDEX_NAME);
        dataSite.deleteSite(siteModel1);
        dataSite.deleteSite(siteModel2);
        dataUser.deleteUser(userSite1);
        dataUser.deleteUser(userSite2);
        dataUser.deleteUser(userMultiSite);
    }

    @TestRail(section = {
            TestGroup.SEARCH }, executionType = ExecutionType.REGRESSION, description = "Verify that Elasticsearch indexing works as expected.")
    @Test(groups = { TestGroup.SEARCH })
    public void fileIndexed() throws Exception
    {
        Utility.sleep(1000, 10000, () -> {
            GetRequest request = new GetRequest(INDEX_NAME);
            request.id(sampleContent.getNodeRef());
            GetResponse documentResponse = elasticClient.get(request, RequestOptions.DEFAULT);

            assertTrue(documentResponse.isExists());
            assertEquals(documentResponse.getSource().get("content"), "This is a test");
        });
    }

    @TestRail(section = {
            TestGroup.SEARCH }, executionType = ExecutionType.REGRESSION, description = "Verify that the simpler Elasticsearch search works as expected.")
    @Test(groups = { TestGroup.SEARCH }, enabled = false)
    public void searchCanFindAFile() throws Exception
    {
        Utility.sleep(1000, 10000, () -> {
            SearchRequest query = new SearchRequest();
            RestRequestQueryModel queryReq = new RestRequestQueryModel();
            queryReq.setQuery("test");
            query.setQuery(queryReq);

            SearchResponse search = client.authenticateUser(userSite1).withSearchAPI().search(query);

            // this test must found only one documents, while documents in the system are four because 
            // only one contains the word "test".
            assertResponseAndResult(search, FILE_0_NAME);
        });
    }

    @TestRail(section = {
            TestGroup.SEARCH }, executionType = ExecutionType.REGRESSION, description = "Verify that Elasticsearch search works as expected using a user that has access to only one site.")
    @Test(groups = { TestGroup.SEARCH }, enabled = false)
    public void searchCanFindAFileAsOwner() throws Exception
    {
        Utility.sleep(1000, 10000, () -> {
            SearchRequest query = new SearchRequest();
            RestRequestQueryModel queryReq = new RestRequestQueryModel();
            queryReq.setQuery("This");
            query.setQuery(queryReq);

            SearchResponse search = client.authenticateUser(userSite1).withSearchAPI().search(query);

            assertResponseAndResult(search, FILE_0_NAME, FILE_1_NAME, FILE_3_NAME);

        });
    }

    @TestRail(section = {
            TestGroup.SEARCH }, executionType = ExecutionType.REGRESSION, description = "Verify that Elasticsearch search works as expected when the user can search a file because he is the owenr.")
    @Test(groups = { TestGroup.SEARCH }, enabled = false)
    public void searchCanFindAFileOnMultipleSites() throws Exception
    {
        Utility.sleep(1000, 10000, () -> {
            SearchRequest query = new SearchRequest();
            RestRequestQueryModel queryReq = new RestRequestQueryModel();
            queryReq.setQuery("This");
            query.setQuery(queryReq);

            SearchResponse search = client.authenticateUser(userSite2).withSearchAPI().search(query);

            //even if the user has access only to a site with 1 document the search will returns two documents 
            //because he is the owner of a document on a site where he hasn't any permission
            assertResponseAndResult(search, FILE_3_NAME, FILE_2_NAME);
        });
    }

    @TestRail(section = {
            TestGroup.SEARCH }, executionType = ExecutionType.REGRESSION, description = "Verify that Elasticsearch search works as expected when a user has permission on multiple sites.")
    @Test(groups = { TestGroup.SEARCH }, enabled = false)
    public void searchCanFindAFilePermission() throws Exception
    {
        Utility.sleep(1000, 10000, () -> {
            SearchRequest query = new SearchRequest();
            RestRequestQueryModel queryReq = new RestRequestQueryModel();
            queryReq.setQuery("This");
            query.setQuery(queryReq);

            SearchResponse search = client.authenticateUser(userMultiSite).withSearchAPI().search(query);

            assertResponseAndResult(search, FILE_0_NAME, FILE_1_NAME, FILE_3_NAME, FILE_2_NAME);
        });
    }

    private FileModel createContent(String filename, String content, SiteModel site, UserModel user)
    {
        return dataContent.usingUser(user).usingSite(site)
                       .createContent(new FileModel(filename, FileType.TEXT_PLAIN, content));
    }

    private void initIndex()
    {
        if (existsIndex(INDEX_NAME))
        {
            deleteIndex(INDEX_NAME);
        }
        createIndex(INDEX_NAME);
    }

    private boolean deleteIndex(String indexName)
    {
        DeleteIndexRequest request = new DeleteIndexRequest(indexName);
        try
        {
            return elasticClient.indices().delete(request, RequestOptions.DEFAULT).isAcknowledged();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private boolean existsIndex(String indexName)
    {
        GetIndexRequest request = new GetIndexRequest(indexName);
        try
        {
            return elasticClient.indices().exists(request, RequestOptions.DEFAULT);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private boolean createIndex(String indexName)
    {
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        try
        {
            return elasticClient.indices().create(request, RequestOptions.DEFAULT).isAcknowledged();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
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
