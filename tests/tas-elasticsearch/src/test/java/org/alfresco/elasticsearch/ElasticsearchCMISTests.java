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
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * Data will be prepared using the schema below:
     * <p>
     * Site1:
     * - Users: user1, userMultiSite
     * - Documents: FILE_0_NAME (owner: user1), FILE_1_NAME (owner: user1), FILE_2_NAME (owner: user2)
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

        createContent(FILE_0_NAME, "This is the first test containing " + UNIQUE_WORD, siteModel1, user1);
        createContent(FILE_1_NAME, "This is another TEST file containing " + UNIQUE_WORD, siteModel1, user1);
        createContent(FILE_2_NAME, "This Test file is owned by user2 " + UNIQUE_WORD, siteModel1, user2);
        // Remove user 2 from site, but he keeps ownership on FILE_2_NAME.
        dataUser.removeUserFromSite(user2, siteModel1);
        // Also create another file that only user 2 has access to.
        createContent(USER_2_FILE_NAME, "This is a test file that user1 does not have access to, but it still contains " + UNIQUE_WORD, siteModel2, user2);
    }

    @TestRail (description = "Verify that we can perform a basic CMIS query against Elasticsearch.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void basicCMISQuery()
    {
        SearchRequest query = req("cmis", "SELECT cmis:name FROM cmis:document WHERE CONTAINS('*')");
        searchQueryService.expectSomeResultsFromQuery(query, user1);
    }

    @TestRail(description = "Verify that we can perform a basic CMIS query against the DB.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test(groups = TestGroup.SEARCH)
    public void basicCMISQueryAgainstDB()
    {
        // This query will be handled by the DB rather than ES.
        SearchRequest query = req("cmis", "SELECT cmis:name FROM cmis:document");
        searchQueryService.expectSomeResultsFromQuery(query, user1);
    }

    @TestRail (description = "Check we can use the CMIS LIKE syntax to match a prefix.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void matchNamesLikePrefix()
    {
        SearchRequest query = req("cmis", "SELECT * FROM cmis:document WHERE cmis:name LIKE '" + PREFIX + "%' AND CONTAINS('*')");
        searchQueryService.expectResultsFromQuery(query, user1, FILE_0_NAME, FILE_1_NAME, FILE_2_NAME);
    }

    @TestRail (description = "Check we can use the CMIS LIKE syntax to match a suffix.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void matchNamesLikeSuffix()
    {
        SearchRequest query = req("cmis", "SELECT * FROM cmis:document WHERE cmis:name LIKE '%" + SUFFIX + "' AND CONTAINS('*')");
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
        SearchRequest query = req("cmis", "SELECT * FROM cmis:document WHERE cmis:name LIKE '" + PREFIX + "%' AND CONTAINS('*')");
        searchQueryService.expectResultsFromQuery(query, user2, FILE_2_NAME, USER_2_FILE_NAME);
    }

    @TestRail (description = "Check that exact term search works.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void matchDocumentName()
    {
        SearchRequest query = req("cmis", "SELECT * FROM cmis:document WHERE cmis:name = '" + FILE_0_NAME + "' AND CONTAINS('*')");
        searchQueryService.expectResultsFromQuery(query, user1, FILE_0_NAME);
    }

    @TestRail (description = "Check IN('value1','value2') syntax works.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test (groups = TestGroup.SEARCH)
    public void checkInSyntax()
    {
        SearchRequest query = req("cmis", "SELECT * FROM cmis:document WHERE cmis:name IN ('" + FILE_0_NAME + "') AND CONTAINS('*')");
        searchQueryService.expectResultsFromQuery(query, user1, FILE_0_NAME);
    }

    private FileModel createContent(String filename, String content, SiteModel site, UserModel user)
    {
        FileModel fileModel = new FileModel(filename, FileType.TEXT_PLAIN, content);
        return dataContent.usingUser(user).usingSite(site)
                          .createContent(fileModel);
    }
}
