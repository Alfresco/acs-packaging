package org.alfresco.elasticsearch.parallel;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.utility.model.FileType.TEXT_PLAIN;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.alfresco.elasticsearch.SearchQueryService;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestCategoryLinkBodyModel;
import org.alfresco.rest.model.RestCategoryModel;
import org.alfresco.rest.model.RestNodeBodyMoveCopyModel;
import org.alfresco.rest.model.RestNodeModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.report.log.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests to check that paths are updated correctly.
 * <p>
 * Tests in this class require waiting for index refresh to happen and so should be run in parallel
 * (see {@link #waitForTwoSeconds}).
 */
@ContextConfiguration (locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
public class PathUpdateTests extends AbstractTestNGSpringContextTests
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PathUpdateTests.class);

    @Autowired
    private ServerHealth serverHealth;
    @Autowired
    private DataUser dataUser;
    @Autowired
    private DataSite dataSite;
    @Autowired
    private DataContent dataContent;
    @Autowired
    protected RestWrapper restClient;
    @Autowired
    protected SearchQueryService searchQueryService;

    private UserModel testUser;
    private SiteModel testSite;

    /**
     * Create a user and a private site containing some nested folders with a document in.
     */
    @BeforeClass (alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.isServerReachable();
        serverHealth.assertServerIsOnline();

        // Before we start testing the live indexing we need to use the reindexing component to index the system nodes.
        Step.STEP("Index system nodes.");
        AlfrescoStackInitializer.reindexEverything();

        Step.STEP("Create a test user and private site.");
        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createPrivateRandomSite();
    }

    @Test
    public void testChangeFileNameUpdatesPath()
    {
        Step.STEP("Create a file in the site.");
        FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
        FileModel testFile = dataContent.usingUser(testUser).usingSite(testSite).createContent(fileModel);

        waitForTwoSeconds();

        Step.STEP("Update the filename.");
        RestNodeModel updatedFile = renameNode(testFile);

        waitForTwoSeconds();

        Step.STEP("Check the path is updated");
        SearchRequest query = req("PATH:\"" + pathInSite(testSite, updatedFile.getName()) + "\"");
        searchQueryService.expectResultsFromQuery(query, testUser, updatedFile.getName());
    }

    @Test
    public void testChangeFileParentUpdatesPath()
    {
        Step.STEP("Create a file next to a folder.");
        FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
        FileModel testFile = dataContent.usingUser(testUser).usingSite(testSite).createContent(fileModel);
        FolderModel testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();

        waitForTwoSeconds();

        Step.STEP("Move the file into the folder and check the path is updated.");
        moveNode(testFile, testFolder);

        waitForTwoSeconds();

        Step.STEP("Check the path is updated");
        SearchRequest query = req("PATH:\"" + pathInSite(testSite, testFolder.getName(), testFile.getName()) + "\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFile.getName());
    }

    @Test
    public void testChangeFolderNameUpdatesPath()
    {
        Step.STEP("Create a file in a folder.");
        FolderModel testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
        FileModel testFile = dataContent.usingUser(testUser).usingResource(testFolder).createContent(fileModel);

        waitForTwoSeconds();

        Step.STEP("Update the folder's name.");
        RestNodeModel updatedFolder = renameNode(testFolder);

        waitForTwoSeconds();

        Step.STEP("Check the path is updated");
        SearchRequest query = req("PATH:\"" + pathInSite(testSite, updatedFolder.getName(), testFile.getName()) + "\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFile.getName());
    }

    @Test
    public void testChangeFolderParentUpdatesPath()
    {
        Step.STEP("Create two folders with a file in the first.");
        FolderModel firstFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        FolderModel secondFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
        FileModel testFile = dataContent.usingUser(testUser).usingResource(firstFolder).createContent(fileModel);

        waitForTwoSeconds();

        Step.STEP("Move the first folder into the second.");
        moveNode(firstFolder, secondFolder);

        waitForTwoSeconds();

        Step.STEP("Check the path is updated");
        SearchRequest query = req("PATH:\"" + pathInSite(testSite, secondFolder.getName(), firstFolder.getName(), testFile.getName()) + "\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFile.getName());
    }

    @Test
    public void testChangeCategoriesUpdatesPath()
    {
        Step.STEP("Create two categories and a file in the first.");
        RestCategoryModel firstCategory = createCategory("-root-");
        RestCategoryModel secondCategory = createCategory("-root-");
        FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
        FileModel testFile = dataContent.usingUser(testUser).usingSite(testSite).createContent(fileModel);
        restClient.authenticateUser(testUser).withCoreAPI().usingNode(testFile)
                  .linkToCategory(RestCategoryLinkBodyModel.builder().categoryId(firstCategory.getId()).create());

        waitForTwoSeconds();

        Step.STEP("Remove the file from the first category and add it to the second.");
        restClient.authenticateUser(testUser).withCoreAPI().usingNode(testFile)
                  .unlinkFromCategory(firstCategory.getId());
        restClient.authenticateUser(testUser).withCoreAPI().usingNode(testFile)
                  .linkToCategory(RestCategoryLinkBodyModel.builder().categoryId(secondCategory.getId()).create());

        waitForTwoSeconds();

        Step.STEP("Check there is no path for the first category.");
        SearchRequest query = req("PATH:\"" + categoryPath(firstCategory.getName(), testFile.getName()) + "\"");
        searchQueryService.expectNoResultsFromQuery(query, testUser);

        Step.STEP("Check there is a path for the second category.");
        query = req("PATH:\"" + categoryPath(secondCategory.getName(), testFile.getName()) + "\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFile.getName());
    }

    @Test
    public void testChangeCategoryNameUpdatesPath()
    {
        Step.STEP("Create a category and a file in it.");
        RestCategoryModel category = createCategory("-root-");
        FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
        FileModel testFile = dataContent.usingUser(testUser).usingSite(testSite).createContent(fileModel);
        restClient.authenticateUser(testUser).withCoreAPI().usingNode(testFile)
                  .linkToCategory(RestCategoryLinkBodyModel.builder().categoryId(category.getId()).create());

        waitForTwoSeconds();

        Step.STEP("Update the name of the category.");
        String newCategoryName = category.getName() + "_updated";
        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI()
                  .usingCategory(category)
                  .updateCategory(RestCategoryModel.builder().name(newCategoryName).create());

        waitForTwoSeconds();

        Step.STEP("Check there is a path with the updated category name.");
        SearchRequest query = req("PATH:\"" + categoryPath(newCategoryName, testFile.getName()) + "\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFile.getName());
    }

    @Test
    public void testChangeCategoryPathUpdatesPath()
    {
        Step.STEP("Create two nested categories and assign a file to the child.");
        RestCategoryModel parentCategory = createCategory("-root-");
        RestCategoryModel childCategory = createCategory(parentCategory.getId());
        FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
        FileModel testFile = dataContent.usingUser(testUser).usingSite(testSite).createContent(fileModel);
        restClient.authenticateUser(testUser).withCoreAPI().usingNode(testFile)
                  .linkToCategory(RestCategoryLinkBodyModel.builder().categoryId(childCategory.getId()).create());

        waitForTwoSeconds();

        Step.STEP("Update the parent category name and check the file's paths are updated.");
        String newCategoryName = parentCategory.getName() + "_updated";
        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI()
                  .usingCategory(parentCategory)
                  .updateCategory(RestCategoryModel.builder().name(newCategoryName).create());

        waitForTwoSeconds();

        Step.STEP("Check there is a path with the updated category name.");
        SearchRequest query = req("PATH:\"" + categoryPath(newCategoryName, childCategory.getName(), testFile.getName()) + "\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFile.getName());
    }

    /**
     * Rename the specified node to have "_updated" on the end.
     *
     * @param node The node to update.
     * @return The updated node.
     */
    private RestNodeModel renameNode(ContentModel node)
    {
        String newName = node.getName() + "_updated";
        JsonObject renameJson = Json.createObjectBuilder().add("properties",
                Json.createObjectBuilder().add("cm:name", newName)).build();
        return restClient.authenticateUser(testUser).withCoreAPI().usingNode(node).updateNode(renameJson.toString());
    }

    /**
     * Move the specified node to a folder.
     *
     * @param node The node to move.
     * @param targetFolder The folder to move the node to.
     * @return The updated node.
     */
    private RestNodeModel moveNode(ContentModel node, FolderModel targetFolder)
    {
        RestNodeBodyMoveCopyModel moveBody = new RestNodeBodyMoveCopyModel();
        moveBody.setTargetParentId(targetFolder.getNodeRef());
        return restClient.authenticateUser(testUser).withCoreAPI().usingNode(node).move(moveBody);
    }

    /**
     * Create a category using admin.
     *
     * @param parentId The parent in which to create the category.
     * @return The new category.
     */
    private RestCategoryModel createCategory(String parentId)
    {
        return restClient.authenticateUser(dataUser.getAdminUser())
                         .withCoreAPI()
                         .usingCategory(RestCategoryModel.builder().id(parentId).create())
                         .createSingleCategory(RestCategoryModel.builder().name(RandomData.getRandomAlphanumeric()).create());
    }

    /**
     * Create a path to a file or folder in a site.
     *
     * @param site The site object.
     * @param documentLibraryNames The list of names of nodes from the document library to the target file or folder.
     * @return An absolute path suitable for use in a path query.
     */
    private String pathInSite(SiteModel site, String... documentLibraryNames)
    {
        return "/app:company_home/st:sites/cm:"+ site.getId() + "/cm:documentLibrary/cm:" + stream(documentLibraryNames).collect(joining("/cm:"));
    }

    /**
     * Create a path to a file or folder via the category hierarchy.
     *
     * @param nodeNames The ordered list of node names from the root category to the node that was categorised.
     * @return An absolute path through the categories to the specified node.
     */
    private String categoryPath(String... nodeNames)
    {
        return "/cm:categoryRoot/cm:generalclassifiable/cm:" + Arrays.stream(nodeNames).collect(joining("/cm:"));
    }

    /**
     * Wait for two seconds. This is needed due to the index refresh interval - see ACS-4637.
     */
    private void waitForTwoSeconds()
    {
        try
        {
            TimeUnit.SECONDS.sleep(2);
        }
        catch (InterruptedException e)
        {
            LOGGER.warn("Failed to wait for full second.");
        }
    }
}
