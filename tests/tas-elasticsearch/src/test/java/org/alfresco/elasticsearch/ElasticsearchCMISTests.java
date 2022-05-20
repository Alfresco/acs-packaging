package org.alfresco.elasticsearch;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.elasticsearch.TestDataUtility.getAlphabeticUUID;

import org.alfresco.rest.search.SearchRequest;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    private static final String PREFIX = getAlphabeticUUID();
    private static final String SUFFIX = getAlphabeticUUID();
    private static final String UNIQUE_WORD = getAlphabeticUUID();
    private static final String FILE_0_NAME = PREFIX + "_test.txt" + SUFFIX;
    private static final String FILE_1_NAME = "internal_" + PREFIX + "_and_" + SUFFIX + ".txt";
    private static final String FILE_2_NAME = PREFIX + "_user2doc_" + SUFFIX;
    /** This is a file that user 1 doesn't have access to and so shouldn't be returned in their search results. */
    private static final String USER_2_FILE_NAME = PREFIX + "_user2only_" + SUFFIX;
    private static final String FOLDER_PREFIX = getAlphabeticUUID();
    private static final String FOLDER_0_NAME = FOLDER_PREFIX + "_folder0";
    private static final String FOLDER_1_NAME = FOLDER_PREFIX + "_folder1";

    @Autowired
    private DataUser dataUser;

    @Autowired
    private DataContent dataContent;

    @Autowired
    private DataSite dataSite;

    @Autowired
    private ServerHealth serverHealth;

    @Autowired
    protected SearchQueryService searchQueryService;

    private UserModel user1;
    private UserModel user2;
    private UserModel userMultiSite;
    private SiteModel siteModel1;
    private SiteModel siteModel2;
    private FileModel file0;

    /**
     * Data will be prepared using the schema below:
     * <p>
     * Site1:
     * - Users: user1, userMultiSite
     * - Documents: FILE_0_NAME (owner: user1), FILE_1_NAME (owner: user1), FILE_2_NAME (owner: user2)
     * - Folders: FOLDER_0_NAME (owner: user1), FOLDER_1_NAME (owner: user1)
     * <p>
     * Site2:
     * - Users: user2, userMultiSite
     * - Documents: USER_2_FILE_NAME (owner: user2)
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

        file0 = createContent(FILE_0_NAME, "This is the first test containing " + UNIQUE_WORD, siteModel1, user1);
        createContent(FILE_1_NAME, "This is another TEST file containing " + UNIQUE_WORD, siteModel1, user1);
        createContent(FILE_2_NAME, "This Test file is owned by user2 " + UNIQUE_WORD, siteModel1, user2);
        // Remove user 2 from site, but he keeps ownership on FILE_2_NAME.
        dataUser.removeUserFromSite(user2, siteModel1);
        // Also create another file that only user 2 has access to.
        createContent(USER_2_FILE_NAME, "This is a test file that user1 does not have access to, but it still contains " + UNIQUE_WORD, siteModel2, user2);

        dataContent.usingUser(user1).usingSite(siteModel1).createFolder(new FolderModel(FOLDER_0_NAME));
        dataContent.usingUser(user1).usingSite(siteModel1).createFolder(new FolderModel(FOLDER_1_NAME));
    }

    @TestRail (description = "Check all documents can be selected when we omit the where clause.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void basicQuery()
    {
        SearchRequest query = req("cmis", "SELECT * FROM cmis:document");
        searchQueryService.expectResultsFromQuery(query, user1, FILE_0_NAME, FILE_1_NAME, FILE_2_NAME);
    }

    @TestRail (description = "Check documents can be selected using cmis:objectId.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void objectIdQuery()
    {
        SearchRequest query = req("cmis", "SELECT * FROM cmis:document WHERE cmis:objectId = '" + file0.getNodeRef() + "'");
        searchQueryService.expectResultsFromQuery(query, user1, FILE_0_NAME);
    }

    @TestRail (description = "Check folders can be selected using cmis:objectTypeId.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void objectTypeIdQuery()
    {
        SearchRequest query1 = req("cmis", "SELECT * FROM cmis:folder WHERE cmis:objectTypeId = 'cmis:folder'");
        searchQueryService.expectResultsFromQuery(query1, user1, "documentLibrary", FOLDER_0_NAME, FOLDER_1_NAME, user1.getUsername());

        SearchRequest query2 = req("cmis", "SELECT * FROM cmis:folder WHERE cmis:objectTypeId = 'F:st:site'");
        searchQueryService.expectResultsFromQuery(query2, user1, siteModel1.getId());
    }

    @TestRail (description = "Check we can use the CMIS LIKE syntax to match a prefix.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void matchNamesLikePrefix()
    {
        SearchRequest query = req("cmis", "SELECT * FROM cmis:document WHERE cmis:name LIKE '" + PREFIX + "%'");
        searchQueryService.expectResultsFromQuery(query, user1, FILE_0_NAME, FILE_1_NAME, FILE_2_NAME);
    }

    @TestRail (description = "Check we can use the CMIS LIKE syntax to match a suffix.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void matchNamesLikeSuffix()
    {
        SearchRequest query = req("cmis", "SELECT * FROM cmis:document WHERE cmis:name LIKE '%" + SUFFIX + "'");
        searchQueryService.expectResultsFromQuery(query, user1, FILE_0_NAME, FILE_1_NAME, FILE_2_NAME);
    }

    @TestRail (description = "Check we can use the CMIS CONTAINS syntax.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void matchContentOfFile()
    {
        // Check the query is case insensitive.
        SearchRequest query = req("cmis", "SELECT * FROM cmis:document WHERE CONTAINS('" + UNIQUE_WORD + "')");
        searchQueryService.expectResultsFromQuery(query, user1, FILE_0_NAME, FILE_1_NAME, FILE_2_NAME);
    }

    @TestRail (description = "Check users can access documents they created even if they are in a site they don't have access to.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void checkPermissionForUser2()
    {
        // Reuse the prefix query to check which documents user2 can access.
        SearchRequest query = req("cmis", "SELECT * FROM cmis:document WHERE cmis:name LIKE '" + PREFIX + "%'");
        searchQueryService.expectResultsFromQuery(query, user2, FILE_2_NAME, USER_2_FILE_NAME);
    }

    @TestRail (description = "Check that we can match a document's name. Needs exact term search to be enabled to pass.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void matchDocumentName()
    {
        SearchRequest query = req("cmis", "SELECT * FROM cmis:document WHERE cmis:name = '" + FILE_0_NAME + "'");
        searchQueryService.expectResultsFromQuery(query, user1, FILE_0_NAME);
    }

    @TestRail (description = "Check IN('value1','value2') syntax works. Needs exact term search to be enabled to pass.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void checkInSyntax()
    {
        SearchRequest query = req("cmis", "SELECT * FROM cmis:document WHERE cmis:name IN ('" + FILE_0_NAME + "', '" + FILE_1_NAME + "')");
        searchQueryService.expectResultsFromQuery(query, user1, FILE_0_NAME, FILE_1_NAME);
    }

    @Test (groups = TestGroup.SEARCH)
    public void negative_basicCMISQuery_missingFrom()
    {
        // note: ideally 400 but currently 500 (also for Solr) :-(

        SearchRequest query1 = req("cmis", "SELECT *");
        searchQueryService.expectErrorFromQuery(query1, user1, HttpStatus.INTERNAL_SERVER_ERROR, "expecting FROM");

        SearchRequest query2 = req("cmis", "SELECT * FROM");
        searchQueryService.expectErrorFromQuery(query2, user1, HttpStatus.INTERNAL_SERVER_ERROR, "no viable alternative at input");
    }

    @Test (groups = TestGroup.SEARCH)
    public void negative_basicCMISQuery_invalidType()
    {
        // note: ideally 400 but currently 500 (also for Solr) :-(
        SearchRequest query = req("SELECT * FROM cmis:unknown");
        searchQueryService.expectErrorFromQuery(query, user1, HttpStatus.INTERNAL_SERVER_ERROR, "Unknown property: {http://www.alfresco.org/model/content/1.0}cmis");
    }

    @Test (groups = TestGroup.SEARCH)
    public void negative_objectTypeIdQuery_invalidObjectTypeId()
    {
        // note: ideally 400 but currently 500 (also for Solr) :-(
        SearchRequest query = req("SELECT * FROM cmis:folder WHERE cmis:objectTypeId = 'unknown:site'");
        searchQueryService.expectErrorFromQuery(query, user1, HttpStatus.INTERNAL_SERVER_ERROR, "Unknown property: {http://www.alfresco.org/model/content/1.0}cmis");
    }

    @Test (groups = TestGroup.SEARCH)
    public void negative_basicCMISQuery_invalidFieldName()
    {
        // note: ideally 400 but currently 500 (also for Solr) :-(

        SearchRequest query1 = req("SELECT cmis:unknown FROM cmis:document");
        searchQueryService.expectErrorFromQuery(query1, user1, HttpStatus.INTERNAL_SERVER_ERROR, "Unknown property: {http://www.alfresco.org/model/content/1.0}cmis");
    
        SearchRequest query2 = req("SELECT cm:unknown FROM cmis:document");
        searchQueryService.expectErrorFromQuery(query2, user1, HttpStatus.INTERNAL_SERVER_ERROR, "Unknown property: {http://www.alfresco.org/model/content/1.0}cm");

        SearchRequest query3 = req("SELECT my:custom FROM cmis:document");
        searchQueryService.expectErrorFromQuery(query3, user1, HttpStatus.INTERNAL_SERVER_ERROR, "Unknown property: {http://www.alfresco.org/model/content/1.0}my");
    }

    @TestRail (description = "Check all folders can be selected (including sites/doc libs).", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void selectFolders()
    {
        SearchRequest query = req("cmis", "SELECT * FROM cmis:folder");
        searchQueryService.expectResultsFromQuery(query, user1,
                // We expect the site and the document library, ...
                siteModel1.getId(), "documentLibrary",
                // the two folders, ...
                FOLDER_0_NAME, FOLDER_1_NAME,
                // and the user home for the user performing the query.
                user1.getUsername());
    }

    @TestRail (description = "Check that cmis:item is equivalent to all files and folders.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void selectItems()
    {
        SearchRequest query = req("cmis", "SELECT * FROM cmis:item");
        // This error is consistent with the way the DB and Solr handle the request.
        searchQueryService.expectErrorFromQuery(query, user1, HttpStatus.INTERNAL_SERVER_ERROR, "Type is not queryable cmis:item");
    }

    @TestRail (description = "Check that cm:cmobject includes both files and folders.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void selectObjects()
    {
        SearchRequest query = req("cmis", "SELECT * FROM cm:cmobject WHERE cmis:name IN ('" + FILE_0_NAME + "', '" + FOLDER_0_NAME + "')");
        searchQueryService.expectResultsFromQuery(query, user1, FILE_0_NAME, FOLDER_0_NAME);
    }

    @TestRail (description = "Check that CMIS queries for cm:person returns people.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void selectPeople()
    {
        SearchRequest query = req("cmis", "SELECT * FROM cm:person");
        searchQueryService.expectNodeTypesFromQuery(query, user1, "cm:person");
    }

    @TestRail (description = "Check that we can use aliases in CMIS queries.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void tableAlias()
    {
        SearchRequest query = req("cmis", "SELECT d.cmis:name FROM cmis:document d WHERE d.cmis:name IN ('" + FILE_0_NAME + "')");
        searchQueryService.expectResultsFromQuery(query, user1, FILE_0_NAME);
    }

    @TestRail (description = "Check that we can use aliases with the AS keyword in CMIS queries.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void tableAliasWithAs()
    {
        SearchRequest query = req("cmis", "SELECT d.cmis:name FROM cmis:document AS d WHERE d.cmis:name IN ('" + FILE_0_NAME + "')");
        searchQueryService.expectResultsFromQuery(query, user1, FILE_0_NAME);
    }

    @TestRail (description = "Negative test for mismatched aliases in CMIS select clause.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void tableAlias_mismatchedAliasInSelect()
    {
        SearchRequest query = req("cmis", "SELECT z.cmis:name FROM cmis:document d WHERE d.cmis:name IN ('" + FILE_0_NAME + "')");
        searchQueryService.expectErrorFromQuery(query, user1, HttpStatus.INTERNAL_SERVER_ERROR, "No selector for z");
    }

    @TestRail (description = "Negative test for mismatched aliases in CMIS where clause.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void tableAlias_mismatchedAliasInWhere()
    {
        SearchRequest query = req("cmis", "SELECT d.cmis:name FROM cmis:document d WHERE z.cmis:name IN ('" + FILE_0_NAME + "')");
        searchQueryService.expectErrorFromQuery(query, user1, HttpStatus.INTERNAL_SERVER_ERROR, "No selector for z");
    }

    @TestRail (description = "Check that we can join two tables in CMIS queries.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void tableJoin()
    {
        SearchRequest query = req("cmis", "SELECT t.*, d.* FROM cm:titled t JOIN cmis:document d ON t.cmis:objectId = d.cmis:objectId WHERE d.cmis:name = '" + FILE_0_NAME + "'");
        searchQueryService.expectResultsFromQuery(query, user1, FILE_0_NAME);
    }

    private FileModel createContent(String filename, String content, SiteModel site, UserModel user)
    {
        FileModel fileModel = new FileModel(filename, FileType.TEXT_PLAIN, content);
        return dataContent.usingUser(user).usingSite(site)
                          .createContent(fileModel);
    }
}
