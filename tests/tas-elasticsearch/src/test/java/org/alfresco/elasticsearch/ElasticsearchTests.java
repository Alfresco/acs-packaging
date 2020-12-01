package org.alfresco.elasticsearch;

import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchNodeModel;
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
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

import static org.testng.Assert.*;

@ContextConfiguration("classpath:alfresco-elasticsearch-context.xml")
public class ElasticsearchTests extends AbstractTestNGSpringContextTests
{
    public static final String INDEX_NAME = "alfresco";

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

    private UserModel testUser;
    private FileModel content;
    private SiteModel siteModel;
    private RestHighLevelClient elasticClient;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();

        elasticClient = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
        initIndex();
        testUser = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(testUser).createPublicRandomSite();
        content = dataContent.usingUser(testUser).usingSite(siteModel)
                          .createContent(new FileModel("test.txt", FileType.TEXT_PLAIN, "This is a test"));
    }

    @TestRail(section = { TestGroup.SEARCH },
              executionType = ExecutionType.REGRESSION,
              description = "Verify that Elasticsearch indexing works as expected.")
    @Test(groups = { TestGroup.SEARCH })
    public void fileIndexed() throws Exception
    {
        Utility.sleep(1000, 10000, () -> {
            GetRequest request = new GetRequest(INDEX_NAME);
            request.id(content.getNodeRef());
            GetResponse documentResponse = elasticClient.get(request, RequestOptions.DEFAULT);

            assertTrue(documentResponse.isExists());
            assertEquals(documentResponse.getSource().get("content"), "This is a test");
        });
    }

    @TestRail(section = { TestGroup.SEARCH },
              executionType = ExecutionType.REGRESSION,
              description = "Verify that Elasticsearch search works as expected.")
    @Test(groups = { TestGroup.SEARCH })
    public void searchCanFindAFile() throws Exception
    {
        Utility.sleep(1000, 10000, () -> {
            SearchRequest query = new SearchRequest();
            RestRequestQueryModel queryReq = new RestRequestQueryModel();
            queryReq.setQuery("test");
            query.setQuery(queryReq);

            SearchResponse search = client.authenticateUser(testUser)
                                            .withSearchAPI()
                                            .search(query);

            client.assertStatusCodeIs(HttpStatus.OK);

            List<SearchNodeModel> entries = search.getEntries();
            assertEquals(entries.size(), 1);
            SearchNodeModel model = search.getEntries().get(0);

            assertTrue(model.getModel().isFile());
        });
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
        } catch (IOException e)
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
        } catch (IOException e)
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
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
