package org.alfresco.elasticsearch.reindexing;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.utility.model.FileType.TEXT_PLAIN;

import java.util.List;

import org.alfresco.elasticsearch.SearchQueryService;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestCategoryLinkBodyModel;
import org.alfresco.rest.model.RestCategoryModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.report.log.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests to verify live indexing of paths using Elasticsearch.
 */
@ContextConfiguration (locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
public class CategoryReindexingTests extends AbstractTestNGSpringContextTests
{
    private static final String TEST_PREFIX = RandomData.getRandomAlphanumeric() + "_";
    private static final String CATEGORY_A_NAME = TEST_PREFIX + "CategoryA";
    private static final String CATEGORY_B_NAME = TEST_PREFIX + "CategoryB";
    private static final String ROOT_CATEGORY_ID = "-root-";
    private static final RestCategoryModel ROOT_CATEGORY = RestCategoryModel.builder().id(ROOT_CATEGORY_ID).create();

    @Autowired
    private ServerHealth serverHealth;
    @Autowired
    private DataUser dataUser;
    @Autowired
    private DataSite dataSite;
    @Autowired
    private DataContent dataContent;
    @Autowired
    protected SearchQueryService searchQueryService;
    @Autowired
    protected RestWrapper restClient;

    private RestCategoryModel categoryA;
    private RestCategoryModel categoryB;
    private UserModel testUser;
    private SiteModel testSite;
    private FileModel testFile;
    private FolderModel testFolder;

    /** Create a user, private site and two categories. Create a folder (in category B) containing a document (in category A and category B). */
    @BeforeClass (alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.isServerReachable();
        serverHealth.assertServerIsOnline();

        // Before we start testing the live indexing we need to use the reindexing component to index the system nodes.
        Step.STEP("Index system nodes.");
        AlfrescoStackInitializer.reindexEverything();

        Step.STEP("Create two categories under the root.");
        List<RestCategoryModel> categories = List.of(RestCategoryModel.builder().name(CATEGORY_A_NAME).create(),
                                                     RestCategoryModel.builder().name(CATEGORY_B_NAME).create());
        List<RestCategoryModel> categoryList = restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI()
                                                    .usingCategory(ROOT_CATEGORY).createCategoriesList(categories).getEntries();
        categoryA = categoryList.get(0).onModel();
        categoryB = categoryList.get(1).onModel();

        Step.STEP("Create a test user and use them to create a private site with a folder containing a document.");
        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createPrivateRandomSite();
        FolderModel folderModel = FolderModel.getRandomFolderModel();
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder(folderModel);
        FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
        testFile = dataContent.usingUser(testUser).usingResource(testFolder).createContent(fileModel);

        Step.STEP("Assign the document to both categories and the folder to category B.");
        RestCategoryLinkBodyModel categoryALink = RestCategoryLinkBodyModel.builder().categoryId(categoryA.getId()).create();
        RestCategoryLinkBodyModel categoryBLink = RestCategoryLinkBodyModel.builder().categoryId(categoryB.getId()).create();
        restClient.authenticateUser(testUser).withCoreAPI().usingNode(testFile).linkToCategory(categoryALink);
        restClient.authenticateUser(testUser).withCoreAPI().usingNode(testFile).linkToCategory(categoryBLink);
        restClient.authenticateUser(testUser).withCoreAPI().usingNode(testFolder).linkToCategory(categoryBLink);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Step.STEP("Run the reindexer before starting the tests.");
        AlfrescoStackInitializer.reindexEverything();
    }

    /** Check we can find the document assigned to a category. */
    @Test (groups = TestGroup.SEARCH)
    public void testFindDocumentByCategory() {
        SearchRequest query = req("cm:categories:\"" + categoryA.getId() + "\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFile.getName());
    }

    /** Check we can find the folder and document assigned to the other category. */
    @Test (groups = TestGroup.SEARCH)
    public void testFindFolderByCategory() {
        SearchRequest query = req("cm:categories:\"" + categoryB.getId() + "\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFile.getName(), testFolder.getName());
    }

    /** Check we can find the document by the pseudo-path created for the category. */
    @Test (groups = TestGroup.SEARCH)
    public void testQueryByCategoryPseudoPath() {
        SearchRequest query = req("PATH:\"/cm:categoryRoot/cm:generalclassifiable/cm:" + CATEGORY_A_NAME + "/*\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFile.getName());
    }

    /** Check we can find the document by a partial path match for the category. */
    @Test (groups = TestGroup.SEARCH)
    public void testQueryByPartialCategoryPathA() {
        SearchRequest query = req("PATH:\"//cm:" + CATEGORY_A_NAME + "/*\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFile.getName());
    }

    /** Check we can find the document and folder by a partial path match for the second category. */
    @Test (groups = TestGroup.SEARCH)
    public void testQueryByPartialCategoryPathB() {
        SearchRequest query = req("PATH:\"//cm:" + CATEGORY_B_NAME + "/*\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFile.getName(), testFolder.getName());
    }
}
