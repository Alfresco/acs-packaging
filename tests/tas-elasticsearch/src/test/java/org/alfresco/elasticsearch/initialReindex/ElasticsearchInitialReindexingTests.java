package org.alfresco.elasticsearch.initialReindex;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.tas.AlfrescoStackInitializer.reindex;
import static org.alfresco.utility.model.FileType.TEXT_PLAIN;

import java.util.Map;
import java.util.UUID;

import org.alfresco.elasticsearch.SearchQueryService;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
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
 * In this test we are verifying end-to-end the reindexer component on Elasticsearch.
 */
@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
                      initializers = AlfrescoStackInitializer.class)

public class ElasticsearchInitialReindexingTests extends AbstractTestNGSpringContextTests
{
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

    private UserModel testUser;
    private SiteModel testSite;

    /**
     * Create a user and a private site and wait for these to be indexed.
     */
    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.isServerReachable();
        serverHealth.assertServerIsOnline();

        Step.STEP("Create a test user and private site containing a document.");

        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createPrivateRandomSite();
        createDocument();
    }

    /**
     * This is run as the first test in the class so that we know that no other test has indexed the system documents.
     */
    @Test(groups = TestGroup.SEARCH, priority = -1)
    public void testReindexerIndexesSystemDocuments()
    {
        // GIVEN
        // Check a particular system document is NOT indexed.
        // Nb. The cm:name:* term ensures that the query hits the index rather than the db.
        SearchRequest query = req("cm:name:budget AND cm:title:\"web site design - budget\" AND cm:description:\"Budget file for the web site redesign\" AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser());

        // WHEN
        // Run reindexer against the initial documents.
        reindex(Map.of("ALFRESCO_REINDEX_JOB_NAME", "reindexByIds",
                       "ALFRESCO_REINDEX_FROM_ID", "0",
                       "ALFRESCO_REINDEX_TO_ID", "1000"));

        // THEN
        // Check system document is indexed.
        searchQueryService.expectResultsFromQuery(query, dataUser.getAdminUser(), "budget.xls");
    }

    /**
     * Create a document using in the test site using the test user.
     *
     * @return The randomly generated name of the new document.
     */
    private String createDocument()
    {
        String documentName = "TestFile" + UUID.randomUUID() + ".txt";
        dataContent.usingUser(testUser)
                   .usingSite(testSite)
                   .createContent(new FileModel(documentName, TEXT_PLAIN, "content"));
        return documentName;
    }
}
