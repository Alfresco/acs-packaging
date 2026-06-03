package org.alfresco.elasticsearch;

import static java.util.Arrays.asList;

import static org.springframework.http.HttpMethod.GET;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchNodeModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.tas.AlfrescoStackInitializer;
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

@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
public class ElasticsearchAdminConsoleTests extends AbstractTestNGSpringContextTests
{
    private static final String ADMIN_CONSOLE_URL = "/alfresco/s/enterprise/admin/admin-searchservice";
    private static final String FILE_NAME = "ElasticsearchAdminConsoleTests_" + UUID.randomUUID() + ".txt";
    private static final String UNAVAILABLE = "Unavailable";
    private static final long CACHE_EXPIRY_MS = 65000;

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
     * <p>
     * The repository nodes count (indexable documents) is computed asynchronously:
     * <ul>
     * <li>First page load returns "Unavailable" and triggers a background DB query.</li>
     * <li>Subsequent refresh shows the computed count (cached for ~60 seconds).</li>
     * <li>After cache expiry, the next refresh serves the stale cached value and triggers a new background DB query.</li>
     * <li>The following refresh shows the updated count.</li>
     * </ul>
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

        Step.STEP("Verify that repository nodes count is 'Unavailable' or numeric on first load.");
        String initialRepoNodesText = document.select("#repositoryNodesCount span.value").text();
        assertTrue(UNAVAILABLE.equals(initialRepoNodesText) || isNumeric(initialRepoNodesText),
                "Expected repository nodes count to be '" + UNAVAILABLE + "' or a number, but got: '" + initialRepoNodesText + "'");

        Step.STEP("Wait for repository nodes count to become available (async DB query).");
        int indexableDocs = waitForNumericRepositoryNodesCount();

        Step.STEP("Get the number of indexed documents (synchronous).");
        document = loadSearchAdminPage();
        int indexedDocs = parseCount(document.select("#elasticsearchDocumentCount span.value").text());

        Step.STEP("Create a document and wait for it to be indexed.");
        createDocumentAndEnsureIndexed(FILE_NAME);

        Step.STEP("Wait for the cache to expire before refreshing.");
        Thread.sleep(CACHE_EXPIRY_MS);

        Step.STEP("Refresh page to trigger a new background DB query (still shows old cached value).");
        loadSearchAdminPage();

        Step.STEP("Wait for the updated repository nodes count to appear.");
        int newIndexableDocs = waitForRepositoryNodesCountGreaterThan(indexableDocs);

        Step.STEP("Get the new indexed document count (synchronous).");
        document = loadSearchAdminPage();
        int newIndexedDocs = parseCount(document.select("#elasticsearchDocumentCount span.value").text());

        Step.STEP("Check the document counts have each been increased by one.");
        assertEquals(newIndexedDocs, indexedDocs + 1, "Expected the number of indexed documents to increase by one.");
        assertEquals(newIndexableDocs, indexableDocs + 1, "Expected the number of indexable documents to increase by one.");
    }

    /**
     * Poll the admin console until the repository nodes count becomes a numeric value.
     */
    private int waitForNumericRepositoryNodesCount() throws Exception
    {
        final int[] result = new int[1];
        Utility.sleep(1000, 60000, () -> {
            Document doc = loadSearchAdminPage();
            String text = doc.select("#repositoryNodesCount span.value").text();
            if (!isNumeric(text))
            {
                throw new AssertionError("Repository nodes count is not yet available: '" + text + "'");
            }
            result[0] = parseCount(text);
        });
        return result[0];
    }

    /**
     * Poll the admin console until the repository nodes count is greater than the given baseline.
     */
    private int waitForRepositoryNodesCountGreaterThan(int baseline) throws Exception
    {
        final int[] result = new int[1];
        Utility.sleep(1000, 60000, () -> {
            Document doc = loadSearchAdminPage();
            String text = doc.select("#repositoryNodesCount span.value").text();
            if (!isNumeric(text))
            {
                throw new AssertionError("Repository nodes count is not yet available: '" + text + "'");
            }
            int count = parseCount(text);
            if (count <= baseline)
            {
                throw new AssertionError("Repository nodes count has not yet updated: " + count + " (baseline: " + baseline + ")");
            }
            result[0] = count;
        });
        return result[0];
    }

    private boolean isNumeric(String text)
    {
        String normalized = text.replace("\u00A0", "").replaceAll("[,\\s]", "").trim();
        if (normalized.isEmpty())
        {
            return false;
        }
        return normalized.chars().allMatch(Character::isDigit);
    }

    private int parseCount(String text)
    {
        String normalized = text.replace("\u00A0", "").replaceAll("[,\\s]", "").trim();
        if (normalized.isEmpty())
        {
            throw new AssertionError("Expected a numeric count but got empty/blank text: '" + text + "'");
        }
        try
        {
            return Integer.parseInt(normalized);
        }
        catch (NumberFormatException e)
        {
            throw new AssertionError("Expected a numeric count but got: '" + text + "'", e);
        }
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
     * @param actual
     *            The search results.
     * @param expected
     *            The expected filename.
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
