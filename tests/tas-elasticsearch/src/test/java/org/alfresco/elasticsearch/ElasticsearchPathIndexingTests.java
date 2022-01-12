package org.alfresco.elasticsearch;

import org.alfresco.rest.search.SearchRequest;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.report.log.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.alfresco.elasticsearch.MavenPropertyHelper.getMavenProperty;
import static org.alfresco.elasticsearch.SearchQueryService.req;

/**
 * Tests to verify live indexing of paths using Elasticsearch.
 */
@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
                      initializers = AlfrescoStackInitializer.class)

public class ElasticsearchPathIndexingTests extends AbstractTestNGSpringContextTests
{
    public static final String CUSTOM_ALFRESCO_INDEX = "custom-alfresco-index";

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

    private org.alfresco.utility.model.UserModel testUser;

    private org.alfresco.utility.model.SiteModel testSite;

    private List<FolderModel> testFolders;

    private String testFileName;
    private String filenameWhichIncludesWhitespace = "TestFile " + UUID.randomUUID() + ".txt";
    private String escapedFilenameWhichIncludesWhitespace = filenameWhichIncludesWhitespace.replace(" ", "_x0020_");
    private String testFileNameWithWhitespace;

    /**
     * Create a user and a private site containing some nested folders with a document in.
     */
    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.isServerReachable();
        serverHealth.assertServerIsOnline();

        // Before we start testing the live indexing we need to use the reindexing component to index the system nodes.
        Step.STEP("Index system nodes.");
        reindexEverything();

        Step.STEP("Create a test user and private site containing three nested folders and a document.");
        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createPrivateRandomSite();

        testFolders = createNestedFolders(3);

        testFileName = createDocument(testFolders.get(testFolders.size() - 1));

        testFileNameWithWhitespace = createDocument(testFolders.get(testFolders.size() - 1), filenameWhichIncludesWhitespace);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSimple()
    {
        SearchRequest query = req("PATH:\"//cm:" + testFileName + "\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRelativePathQuery()
    {
        SearchRequest query = req("PATH:\"//cm:" + testFileName + "\" ");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRelativePathQueryWithoutPrefixes()
    {
        SearchRequest query = req("PATH:\"//" + testFileName + "\" AND name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testWildcardQuery()
    {
        // The test file should be the only descendent of the last folder.
        SearchRequest query = req("PATH:\"//" + testSite.getId() + "//" + testFolders.get(testFolders.size() - 1).getName() + "/*\" AND name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName, testFileNameWithWhitespace);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testAbsolutePathQuery()
    {
        String folderPath = testFolders.stream().map(folder -> "cm:" + folder.getName()).collect(Collectors.joining("/"));
        SearchRequest query = req("PATH:\"/app:company_home/st:sites/cm:" + testSite.getId() + "/cm:documentLibrary/" + folderPath + "/cm:" + testFileName + "\" AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testAbsolutePathQueryWithoutPrefixes()
    {
        String folderPath = testFolders.stream().map(folder -> "cm:" + folder.getName()).collect(Collectors.joining("/"));
        SearchRequest query = req("PATH:\"/company_home/sites/" + testSite.getId() + "/documentLibrary/" + folderPath + "/" + testFileName + "\" AND name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRootNodes()
    {
        SearchRequest query = req("PATH:\"/*\" AND name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, "categories", "Company Home");
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRootNodesWithoutWildcard()
    {
        SearchRequest query = req("PATH:\"/\"");
        searchQueryService.expectNodeTypesFromQuery(query, testUser, "sys:store_root");
    }

    @Test(groups = TestGroup.SEARCH)
    public void testPathNameMismatch()
    {
        SearchRequest query = req("PATH:\"/*\" AND name:" + testFileName + " AND name:*");
        searchQueryService.expectNoResultsFromQuery(query, testUser);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testPathNameIntersect()
    {
        SearchRequest query = req("PATH:\"//*\" AND name:" + testFileName + " AND name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testAllDescendentsOfFolder()
    {
        SearchRequest query = req("PATH:\"//" + testFolders.get(0).getName() + "//*\" AND name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName, testFileNameWithWhitespace, testFolders.get(1).getName(), testFolders.get(2).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testAllFoldersInSite()
    {
        SearchRequest query = req("PATH:\"/*/sites/" + testSite.getId() + "/*//*\" AND TYPE:\"cm:folder\" AND name:*");
        String[] folderNames = testFolders.stream().map(ContentModel::getName).toArray(String[]::new);
        searchQueryService.expectResultsFromQuery(query, testUser, folderNames);
    }

    @Test(groups = TestGroup.SEARCH, enabled = false)
    public void testUpdatePath()
    {
        //disabled test: find a way to rename or move a folder on repository
        String folderPath = testFolders.stream().map(folder -> "cm:" + folder.getName()).collect(Collectors.joining("/"));
        SearchRequest query = req("PATH:\"/app:company_home/cm:" + testFileName + "\" AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName);
    }

    /**
     * Create a set of nested folders in the test site using the test user.
     *
     * @return The folder objects (containing the randomly generated names) in order of depth.
     */
    private List<FolderModel> createNestedFolders(int maxDepth)
    {
        List<FolderModel> folders = new ArrayList<>();
        dataContent.usingSite(testSite);
        for (int depth = 0; depth < maxDepth; depth++)
        {
            String folderName = "TestFolder" + depth + "_" + UUID.randomUUID();
            FolderModel folderModel = new FolderModel(folderName);
            folders.add(folderModel);
            if (depth != 0)
            {
                dataContent.usingResource(folders.get(depth - 1));
            }
            dataContent.usingUser(testUser)
                       .createFolder(folderModel);
        }
        return folders;
    }

    /**
     * Create a document in the given folder using the test user and a random filename.
     *
     * @param folderModel The location to create the document.
     * @return The randomly generated name of the new document.
     */
    private String createDocument(FolderModel folderModel)
    {
        return createDocument(folderModel, "TestFile" + UUID.randomUUID() + ".txt");
    }

    /**
     * Create a document in the given folder using the test user and the given filename.
     *
     * @param folderModel The location to create the document.
     * @param filename    the filename.
     * @return the passed filename.
     */
    private String createDocument(FolderModel folderModel, String filename)
    {
        dataContent.usingUser(testUser)
                   .usingResource(folderModel)
                   .createContent(new org.alfresco.utility.model.FileModel(filename, org.alfresco.utility.model.FileType.TEXT_PLAIN, "content"));
        return filename;
    }

    /**
     * Run the alfresco-elasticsearch-reindexing container with path reindexing enabled.
     */
    private void reindexEverything()
    {
        // Run the reindexing container.
        Map<String, String> env = new HashMap<>(
                Map.of("ALFRESCO_REINDEX_PATHINDEXINGENABLED", "true", // Ensure path reindexing is enabled.
                       "SPRING_ELASTICSEARCH_REST_URIS", "http://elasticsearch:9200",
                       "SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/alfresco",
                       "ELASTICSEARCH_INDEX_NAME", CUSTOM_ALFRESCO_INDEX,
                       "SPRING_ACTIVEMQ_BROKER-URL", "nio://activemq:61616",
                       "ALFRESCO_ACCEPTEDCONTENTMEDIATYPESCACHE_BASEURL", "http://transform-core-aio:8090/transform/config",
                       "ALFRESCO_REINDEX_JOB_NAME", "reindexByDate"));

        try (GenericContainer reindexingComponent = new GenericContainer("quay.io/alfresco/alfresco-elasticsearch-reindexing:" + getMavenProperty("dependency.elasticsearch-shared.version"))
                .withEnv(env)
                .withNetwork(AlfrescoStackInitializer.network)
                .withStartupCheckStrategy(
                        new IndefiniteWaitOneShotStartupCheckStrategy()))
        {
            reindexingComponent.start();
        }
    }
}