package org.alfresco.elasticsearch.reindexing;

import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.utility.model.FileType.TEXT_PLAIN;
import static org.junit.Assert.assertEquals;

import javax.json.Json;
import javax.json.JsonObject;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;

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
import org.testcontainers.shaded.org.apache.commons.lang3.tuple.Triple;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests to check that paths are updated correctly.
 * <p>
 * These tests use an unusual structure where the bulk of the code is stored in lambdas within the {@#testSteps} method.
 * All the "given" and "when" steps are run in the `@BeforeClass` method (with a one second pause between them) and then
 * the "then" steps are run in the individual `@Test` methods.
 * This structure saves about two seconds per test over having the "given" and "when" steps in the `@Test` methods.
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

    /** Map from test method name to the set of named properties used by the test. */
    private Map<String, Map<String, Object>> propertyMaps;

    /** All the "given", "when" and "then" steps for each test, grouped according to the test name. */
    private Map<String, Triple<Supplier<Map<String, Object>>, Function<Map<String, Object>, Map<String, Object>>, Consumer<Map<String, Object>>>> testSteps = Map.of(
            "testChangeFileNameUpdatesPath", Triple.of(() -> {
                Step.STEP("Create a file in the site.");
                FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
                FileModel testFile = dataContent.usingUser(testUser).usingSite(testSite).createContent(fileModel);

                return Map.of("testFile", testFile);
            }, (properties) -> {
                FileModel testFile = (FileModel) properties.get("testFile");

                Step.STEP("Update the filename.");
                RestNodeModel updatedFile = renameNode(restClient, testUser, testFile);

                return Map.of("updatedName", updatedFile.getName());
            }, (properties) -> {
                String updatedName = (String) properties.get("updatedName");

                Step.STEP("Check the path is updated");
                SearchRequest query = req("PATH:\"" + pathInSite(testSite, updatedName) + "\"");
                searchQueryService.expectResultsFromQuery(query, testUser, updatedName);
            }),

            "testChangeFileParentUpdatesPath", Triple.of(() -> {
                Step.STEP("Create a file next to a folder.");
                FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
                FileModel testFile = dataContent.usingUser(testUser).usingSite(testSite).createContent(fileModel);
                FolderModel testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();

                return Map.of("testFile", testFile, "testFolder", testFolder);
            }, (properties) -> {
                FileModel testFile = (FileModel) properties.get("testFile");
                FolderModel testFolder = (FolderModel) properties.get("testFolder");

                Step.STEP("Move the file into the folder and check the path is updated.");
                moveNode(restClient, testUser, testFile, testFolder);

                return Map.of("folderName", testFolder.getName(), "fileName", testFile.getName());
            }, (properties) -> {
                String folderName = (String) properties.get("folderName");
                String fileName = (String) properties.get("fileName");

                Step.STEP("Check the path is updated");
                SearchRequest query = req("PATH:\"" + pathInSite(testSite, folderName, fileName) + "\"");
                searchQueryService.expectResultsFromQuery(query, testUser, fileName);
            }),

            "testChangeFolderNameUpdatesPath", Triple.of(() -> {
                Step.STEP("Create a file in a folder.");
                FolderModel testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
                FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
                FileModel testFile = dataContent.usingUser(testUser).usingResource(testFolder).createContent(fileModel);

                return Map.of("testFile", testFile, "testFolder", testFolder);
            }, (properties) -> {
                FileModel testFile = (FileModel) properties.get("testFile");
                FolderModel testFolder = (FolderModel) properties.get("testFolder");

                Step.STEP("Update the folder's name.");
                RestNodeModel updatedFolder = renameNode(restClient, testUser, testFolder);

                return Map.of("updatedFolderName", updatedFolder.getName(), "fileName", testFile.getName());
            }, (properties) -> {
                String updatedFolderName = (String) properties.get("updatedFolderName");
                String fileName = (String) properties.get("fileName");

                Step.STEP("Check the path is updated");
                SearchRequest query = req("PATH:\"" + pathInSite(testSite, updatedFolderName, fileName) + "\"");
                searchQueryService.expectResultsFromQuery(query, testUser, fileName);
            }),

            "testChangeFolderParentUpdatesPath", Triple.of(() -> {
                Step.STEP("Create two folders with a file in the first.");
                FolderModel firstFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
                FolderModel secondFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
                FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
                FileModel testFile = dataContent.usingUser(testUser).usingResource(firstFolder).createContent(fileModel);

                return Map.of("testFile", testFile, "firstFolder", firstFolder, "secondFolder", secondFolder);
            }, (properties) -> {
                FileModel testFile = (FileModel) properties.get("testFile");
                FolderModel firstFolder = (FolderModel) properties.get("firstFolder");
                FolderModel secondFolder = (FolderModel) properties.get("secondFolder");

                Step.STEP("Move the first folder into the second.");
                moveNode(restClient, testUser, firstFolder, secondFolder);

                return Map.of("fileName", testFile.getName(), "firstFolderName", firstFolder.getName(), "secondFolderName", secondFolder.getName());
            }, (properties) -> {
                String fileName = (String) properties.get("fileName");
                String firstFolderName = (String) properties.get("firstFolderName");
                String secondFolderName = (String) properties.get("secondFolderName");

                Step.STEP("Check the path is updated");
                SearchRequest query = req("PATH:\"" + pathInSite(testSite, secondFolderName, firstFolderName, fileName) + "\"");
                searchQueryService.expectResultsFromQuery(query, testUser, fileName);
            }),

            "testChangeCategoriesUpdatesPath", Triple.of(() -> {
                Step.STEP("Create two categories and a file in the first.");
                RestCategoryModel firstCategory = createCategory(restClient, dataUser.getAdminUser(), "-root-");
                RestCategoryModel secondCategory = createCategory(restClient, dataUser.getAdminUser(), "-root-");
                FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
                FileModel testFile = dataContent.usingUser(testUser).usingSite(testSite).createContent(fileModel);
                restClient.authenticateUser(testUser).withCoreAPI().usingNode(testFile)
                          .linkToCategory(RestCategoryLinkBodyModel.builder().categoryId(firstCategory.getId()).create());

                return Map.of("testFile", testFile, "firstCategory", firstCategory, "secondCategory", secondCategory);
            }, (properties) -> {
                FileModel testFile = (FileModel) properties.get("testFile");
                RestCategoryModel firstCategory = (RestCategoryModel) properties.get("firstCategory");
                RestCategoryModel secondCategory = (RestCategoryModel) properties.get("secondCategory");

                Step.STEP("Remove the file from the first category and add it to the second.");
                restClient.authenticateUser(testUser).withCoreAPI().usingNode(testFile)
                          .unlinkFromCategory(firstCategory.getId());
                restClient.authenticateUser(testUser).withCoreAPI().usingNode(testFile)
                          .linkToCategory(RestCategoryLinkBodyModel.builder().categoryId(secondCategory.getId()).create());

                return Map.of("fileName", testFile.getName(), "firstCategoryName", firstCategory.getName(), "secondCategoryName", secondCategory.getName());
            }, (properties) -> {
                String fileName = (String) properties.get("fileName");
                String firstCategoryName = (String) properties.get("firstCategoryName");
                String secondCategoryName = (String) properties.get("secondCategoryName");

                Step.STEP("Check there is no path for the first category.");
                SearchRequest query = req("PATH:\"" + categoryPath(firstCategoryName, fileName) + "\"");
                searchQueryService.expectNoResultsFromQuery(query, testUser);

                Step.STEP("Check there is a path for the second category.");
                query = req("PATH:\"" + categoryPath(secondCategoryName, fileName) + "\"");
                searchQueryService.expectResultsFromQuery(query, testUser, fileName);
            }),

            "testChangeCategoryNameUpdatesPath", Triple.of(() -> {
                Step.STEP("Create a category and a file in it.");
                RestCategoryModel category = createCategory(restClient, dataUser.getAdminUser(), "-root-");
                FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
                FileModel testFile = dataContent.usingUser(testUser).usingSite(testSite).createContent(fileModel);
                restClient.authenticateUser(testUser).withCoreAPI().usingNode(testFile)
                          .linkToCategory(RestCategoryLinkBodyModel.builder().categoryId(category.getId()).create());

                return Map.of("testFile", testFile, "category", category);
            }, (properties) -> {
                FileModel testFile = (FileModel) properties.get("testFile");
                RestCategoryModel category = (RestCategoryModel) properties.get("category");

                Step.STEP("Update the name of the category.");
                String newCategoryName = category.getName() + "_updated";
                restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI()
                          .usingCategory(category)
                          .updateCategory(RestCategoryModel.builder().name(newCategoryName).create());

                return Map.of("fileName", testFile.getName(), "newCategoryName", newCategoryName);
            }, (properties) -> {
                String fileName = (String) properties.get("fileName");
                String newCategoryName = (String) properties.get("newCategoryName");

                Step.STEP("Check there is a path with the updated category name.");
                SearchRequest query = req("PATH:\"" + categoryPath(newCategoryName, fileName) + "\"");
                searchQueryService.expectResultsFromQuery(query, testUser, fileName);
            }),

            "testChangeCategoryPathUpdatesPath", Triple.of(() -> {
                Step.STEP("Create two nested categories and assign a file to the child.");
                RestCategoryModel parentCategory = createCategory(restClient, dataUser.getAdminUser(), "-root-");
                RestCategoryModel childCategory = createCategory(restClient, dataUser.getAdminUser(), parentCategory.getId());
                FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
                FileModel testFile = dataContent.usingUser(testUser).usingSite(testSite).createContent(fileModel);
                restClient.authenticateUser(testUser).withCoreAPI().usingNode(testFile)
                          .linkToCategory(RestCategoryLinkBodyModel.builder().categoryId(childCategory.getId()).create());

                return Map.of("testFile", testFile, "childCategory", childCategory, "parentCategory", parentCategory);
            }, (properties) -> {
                FileModel testFile = (FileModel) properties.get("testFile");
                RestCategoryModel childCategory = (RestCategoryModel) properties.get("childCategory");
                RestCategoryModel parentCategory = (RestCategoryModel) properties.get("parentCategory");

                Step.STEP("Update the parent category name and check the file's paths are updated.");
                String newCategoryName = parentCategory.getName() + "_updated";
                restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI()
                          .usingCategory(parentCategory)
                          .updateCategory(RestCategoryModel.builder().name(newCategoryName).create());

                return Map.of("fileName", testFile.getName(), "childCategoryName", childCategory.getName(),"parentCategoryName", newCategoryName);
            }, (properties) -> {
                String fileName = (String) properties.get("fileName");
                String childCategoryName = (String) properties.get("childCategoryName");
                String parentCategoryName = (String) properties.get("parentCategoryName");

                Step.STEP("Check there is a path with the updated category name.");
                SearchRequest query = req("PATH:\"" + categoryPath(parentCategoryName, childCategoryName, fileName) + "\"");
                searchQueryService.expectResultsFromQuery(query, testUser, fileName);
            })
    );

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

        // Check that the methods in testWhenAndThenSteps match the @Test methods in this class.
        Set<String> testMethods = stream(this.getClass().getMethods())
                .filter(method -> stream(method.getAnnotations()).anyMatch(Test.class::isInstance))
                .map(Method::getName)
                .collect(Collectors.toSet());
        assertEquals("Some entries in testWhenAndThenSteps do not have corresponding @Test methods.", emptySet(), Sets.difference(testSteps.keySet(), testMethods));

        // Execute the "given" step for all tests and store the state to be passed into the "when" steps.
        propertyMaps = testSteps.entrySet()
                                .stream()
                                .collect(toMap(Entry::getKey,
                                                    entry -> testSteps.get(entry.getKey())
                                                                      .getLeft()
                                                                      .get()));

        waitForOneSecond();

        // Execute the "when" step for all tests and store the state to be passed into the "then" steps.
        propertyMaps = testSteps.entrySet()
                                .stream()
                                .collect(toMap(Entry::getKey,
                                        entry -> testSteps.get(entry.getKey())
                                                          .getMiddle()
                                                          .apply(propertyMaps.get(entry.getKey()))));
    }

    @Test
    public void testChangeFileNameUpdatesPath()
    {
        String methodName = StackWalker.getInstance().walk(Stream::findFirst).get().getMethodName();
        testSteps.get(methodName).getRight().accept(propertyMaps.get(methodName));
    }

    @Test
    public void testChangeFileParentUpdatesPath()
    {
        String methodName = StackWalker.getInstance().walk(Stream::findFirst).get().getMethodName();
        testSteps.get(methodName).getRight().accept(propertyMaps.get(methodName));
    }

    @Test
    public void testChangeFolderNameUpdatesPath()
    {
        String methodName = StackWalker.getInstance().walk(Stream::findFirst).get().getMethodName();
        testSteps.get(methodName).getRight().accept(propertyMaps.get(methodName));
    }

    @Test
    public void testChangeFolderParentUpdatesPath()
    {
        String methodName = StackWalker.getInstance().walk(Stream::findFirst).get().getMethodName();
        testSteps.get(methodName).getRight().accept(propertyMaps.get(methodName));
    }

    @Test
    public void testChangeCategoriesUpdatesPath()
    {
        String methodName = StackWalker.getInstance().walk(Stream::findFirst).get().getMethodName();
        testSteps.get(methodName).getRight().accept(propertyMaps.get(methodName));
    }

    @Test
    public void testChangeCategoryNameUpdatesPath()
    {
        String methodName = StackWalker.getInstance().walk(Stream::findFirst).get().getMethodName();
        testSteps.get(methodName).getRight().accept(propertyMaps.get(methodName));
    }

    @Test
    public void testChangeCategoryPathUpdatesPath()
    {
        String methodName = StackWalker.getInstance().walk(Stream::findFirst).get().getMethodName();
        testSteps.get(methodName).getRight().accept(propertyMaps.get(methodName));
    }

    /**
     * Rename the specified node to have "_updated" on the end.
     *
     * @param restClient The REST client to use to send the request.
     * @param user The user to use for the update.
     * @param node The node to update.
     * @return The updated node.
     */
    private static RestNodeModel renameNode(RestWrapper restClient, UserModel user, ContentModel node)
    {
        String newName = node.getName() + "_updated";
        JsonObject renameJson = Json.createObjectBuilder().add("properties",
                Json.createObjectBuilder().add("cm:name", newName)).build();
        return restClient.authenticateUser(user).withCoreAPI().usingNode(node).updateNode(renameJson.toString());
    }

    /**
     * Move the specified node to a folder.
     *
     * @param restClient The REST client to use to send the request.
     * @param user The user to use for the move.
     * @param node The node to move.
     * @param targetFolder The folder to move the node to.
     * @return The updated node.
     */
    private static RestNodeModel moveNode(RestWrapper restClient, UserModel user, ContentModel node, FolderModel targetFolder)
    {
        RestNodeBodyMoveCopyModel moveBody = new RestNodeBodyMoveCopyModel();
        moveBody.setTargetParentId(targetFolder.getNodeRef());
        return restClient.authenticateUser(user).withCoreAPI().usingNode(node).move(moveBody);
    }

    /**
     * Create a category.
     *
     * @param restClient The REST client to use.
     * @param user The user to use to create the category.
     * @param parentId The parent in which to create the category.
     * @return The new category.
     */
    private static RestCategoryModel createCategory(RestWrapper restClient, UserModel user, String parentId)
    {
        return restClient.authenticateUser(user)
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
    private static String pathInSite(SiteModel site, String... documentLibraryNames)
    {
        return "/app:company_home/st:sites/cm:"+ site.getId() + "/cm:documentLibrary/cm:" + stream(documentLibraryNames).collect(joining("/cm:"));
    }

    /**
     * Create a path to a file or folder via the category hierarchy.
     *
     * @param nodeNames The ordered list of node names from the root category to the node that was categorised.
     * @return An absolute path through the categories to the specified node.
     */
    private static String categoryPath(String... nodeNames)
    {
        return "/cm:categoryRoot/cm:generalclassifiable/cm:" + Arrays.stream(nodeNames).collect(joining("/cm:"));
    }

    /**
     * Wait for a second. This is needed due to the index refresh interval - see ACS-4637.
     */
    private static void waitForOneSecond()
    {
        try
        {
            TimeUnit.SECONDS.sleep(1);
        }
        catch (InterruptedException e)
        {
            LOGGER.warn("Failed to wait for full second.");
        }
    }
}
