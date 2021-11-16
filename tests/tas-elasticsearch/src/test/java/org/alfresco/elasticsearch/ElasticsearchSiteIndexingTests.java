package org.alfresco.elasticsearch;

import static org.alfresco.elasticsearch.SearchQueryService.req;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.alfresco.dataprep.AlfrescoHttpClientFactory;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.report.log.Step;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@ContextConfiguration (locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
public class ElasticsearchSiteIndexingTests extends AbstractTestNGSpringContextTests
{
    private static final Iterable<String> LANGUAGES_TO_CHECK = List.of("afts", "lucene");
    private static final String ALL_SITES = "_ALL_SITES_";
    private static final String EVERYTHING = "_EVERYTHING_";

    @Autowired
    ServerHealth serverHealth;

    @Autowired
    DataUser dataUser;

    @Autowired
    DataContent dataContent;

    @Autowired
    public DataSite dataSite;

    @Autowired
    private AlfrescoHttpClientFactory alfrescoHttpClientFactory;

    @Autowired
    SearchQueryService searchQueryService;

    @Autowired
    protected RestWrapper restClient;

    private UserModel testUser;
    private SiteModel publicSite1;

    @BeforeClass (alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        serverHealth.assertServerIsOnline();

        Step.STEP("Create a test user and public site.");
        testUser = dataUser.createRandomTestUser();
        publicSite1 = dataSite.usingUser(testUser).createPublicRandomSite();
    }

    @TestRail (section = { TestGroup.SEARCH, TestGroup.SITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify the SITE queries work correctly")
    @Test (groups = { TestGroup.SEARCH, TestGroup.SITES, TestGroup.REGRESSION })
    public void testSiteUseCases()
    {
        Step.STEP("No such site exists so no results.");
        // No such site exists so no results
        assertSiteQueryResult(unique("NoSuchSite"), List.of());

        // Public site has no docs so no result
        Step.STEP("Public site has no files, so no results.");
        assertSiteQueryResult(publicSite1.getId(), List.of());

        // Create document in the public site - expect a single result
        Step.STEP("Public site has one file, so one result.");
        FileModel file1 = createContentInSite(publicSite1, "test1");
        assertSiteQueryResult(publicSite1.getGuid(), List.of(file1));
    }

    private void assertSiteQueryResult(String siteName, Collection<ContentModel> contentModels)
    {
        final List<String> contentNames = contentModels
                .stream()
                .map(ContentModel::getName)
                .collect(Collectors.toList());

            for (final String language : LANGUAGES_TO_CHECK)
            {
                Step.STEP("Searching for SITE `" + siteName + "` using `" + language + "` language.");
                final SearchRequest query = req(language, "SITE:" + siteName);
                if (contentNames.isEmpty())
                {
                    searchQueryService.expectNoResultsFromQuery(query, testUser);
                }
                else
                {
                    Collections.shuffle(contentNames);
                    searchQueryService.expectResultsFromQuery(query, testUser, contentNames.toArray(String[]::new));
                }
            }
    }

    private FileModel createContentInSite(SiteModel site, String fileName)
    {
        final FileModel file = new FileModel(unique(fileName) + ".txt", FileType.TEXT_PLAIN, "Content for " + fileName);
        return dataContent.usingUser(testUser)
                .usingSite(site)
                .createContent(file);
    }

    // will need this for additional tests
//    private FileModel givenFile(final String fileName)
//    {
//        final FileModel file = new FileModel(unique(fileName), FileType.TEXT_PLAIN, "Content for " + fileName);
//        return dataContent
//                .usingAdmin()
//                .usingResource(testFolder)
//                .createContent(file);
//    }

    // will need this for additional tests
//    private void deleteFile(ContentModel contentModel)
//    {
//        dataContent
//                .usingAdmin()
//                .usingResource(contentModel)
//                .deleteContent();
//    }

    private static String unique(String prefix)
    {
        return prefix + "-" + UUID.randomUUID();
    }
}