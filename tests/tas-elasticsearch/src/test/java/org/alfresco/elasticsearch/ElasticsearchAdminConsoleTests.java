package org.alfresco.elasticsearch;

import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchNodeModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.Utility;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.report.log.Step;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.springframework.http.HttpMethod.GET;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
                      initializers = AlfrescoStackInitializer.class)
public class ElasticsearchAdminConsoleTests extends AbstractTestNGSpringContextTests
{
    private static final String ADMIN_CONSOLE_URL = "/alfresco/s/enterprise/admin/admin-searchservice";
    private static final String FILE_NAME = "ElasticsearchAdminConsoleTests_" + UUID.randomUUID() + ".txt";

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

    private UserModel user;
    private SiteModel siteModel;

    /**
     * Create a user and a private site and wait for these to be indexed.
     */
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        serverHealth.assertServerIsOnline();

        Step.STEP("Create a test user and private site.");
        user = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(user).createPrivateRandomSite();
        Step.STEP("Create a document in the site and wait for it to be indexed");
        // We wait for this file to be indexed as a proxy for indexing probably being up to date.
        createDocumentAndEnsureIndexed("DummyFile" + UUID.randomUUID() + ".txt");
    }

    /**
     * Check the "elasticsearch" subsystem is selected and that the creation of a new document is reflected in the document counts.
     */
    @TestRail(section = TestGroup.SEARCH,
              executionType = ExecutionType.REGRESSION,
              description = "Verify that the Elasticsearch subsystem is selected and that the creation of a new document is reflected in the document counts.")
    @Test(groups = TestGroup.SEARCH)
    public void elasticsearchAdminConsolePage() throws Exception
    {
        Step.STEP("Load the Search admin page.");
        Document document = loadSearchAdminPage();

        Step.STEP("Check that elasticsearch is selected and displayed.");
        assertEquals(document.select("option[value=elasticsearch]").attr("selected"), "selected",
                     "Expected elasticsearch subsystem to be shown as selected.");
        assertFalse(document.select("#elasticsearchSearch").hasClass("hidden"), "Expected elasticsearch section to be displayed.");

        Step.STEP("Get the number of indexed and indexable documents.");
        int indexedDocs = Integer.valueOf(document.select("#elasticsearchDocumentCount span.value").text());
        int indexableDocs = Integer.valueOf(document.select("#repositoryNodesCount span.value").text());

        Step.STEP("Create a document and wait for it to be indexed.");
        createDocumentAndEnsureIndexed(FILE_NAME);

        Step.STEP("Refresh the admin console.");
        document = loadSearchAdminPage();
        Step.STEP("Get the new number of indexed and indexable documents.");
        int newIndexedDocs = Integer.valueOf(document.select("#elasticsearchDocumentCount span.value").text());
        int newIndexableDocs = Integer.valueOf(document.select("#repositoryNodesCount span.value").text());

        Step.STEP("Check the document counts have each been increased by one.");
        assertEquals(newIndexedDocs, indexedDocs + 1, "Expected the number of indexed documents to increase by one.");
        assertEquals(newIndexableDocs, indexableDocs + 1, "Expected the number of indexable documents to increase by one.");
    }

    /**
     * Create a text document with the given file name and wait until we can query for it.
     */
    private void createDocumentAndEnsureIndexed(String fileName) throws Exception
    {
        dataContent.usingUser(user).usingSite(siteModel)
                   .createContent(new FileModel(fileName, FileType.TEXT_PLAIN, "content"));
        Utility.sleep(1000, 20000, () -> {
            RestRequestQueryModel queryReq = new RestRequestQueryModel();
            queryReq.setQuery("cm:name:\"" + fileName + "\"");
            SearchResponse search = client.authenticateUser(user).withSearchAPI().search(new SearchRequest(queryReq));
            checkFileIndexed(search, fileName);
        });
    }

    /**
     * Load the HTML representation of the Search Admin Console page.
     */
    private Document loadSearchAdminPage()
    {
        RestRequest restRequest = RestRequest.simpleRequest(GET, ADMIN_CONSOLE_URL);
        RestResponse response = client.clearBasePath().authenticateUser(dataUser.getAdminUser()).process(restRequest);
        response.assertThat().statusCode(200);
        return Jsoup.parse(response.getResponse().asString());
    }

    /**
     * Check that the search response contains exactly the expected file.
     *
     * @param actual   The search results.
     * @param expected The expected filename.
     */
    private void checkFileIndexed(SearchResponse actual, String expected)
    {
        client.assertStatusCodeIs(HttpStatus.OK);
        List<String> result = actual.getEntries().stream()
                                    .map(SearchNodeModel::getModel)
                                    .map(SearchNodeModel::getName).collect(Collectors.toList());
        assertEquals(result, asList(expected), "Didn't find unique instance of indexed file.");
    }
}
