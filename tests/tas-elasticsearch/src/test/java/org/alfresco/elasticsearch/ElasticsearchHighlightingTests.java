package org.alfresco.elasticsearch;

import static java.util.stream.Collectors.toSet;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.utility.report.log.Step.STEP;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.alfresco.rest.search.ResponseHighlightModel;
import org.alfresco.rest.search.RestRequestHighlightModel;
import org.alfresco.rest.search.SearchNodeModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@ContextConfiguration (locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
public class ElasticsearchHighlightingTests extends AbstractTestNGSpringContextTests
{
    static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchHighlightingTests.class);
    static final String FILE_A = "fileA.txt";
    static final String FILE_B = "fileB.txt";
    static final String CONTENT_A = "The quick brown fox jumps over the lazy dog.";
    static final String CONTENT_B = """
            The lazy dog sleeps under the quick brown fox. The middle of the document is quite long:
            Lorem ipsum dolor sit amet. In veniam tempore hic provident sunt et distinctio velit et reprehenderit
            officiis id sapiente omnis et quisquam aliquam et porro perferendis. In sequi placeat ut quaerat voluptatem
            ea consequuntur impedit aut eaque enim aut atque rerum.
            The end of the document mentions the dog again!
            """;

    @Autowired
    ServerHealth serverHealth;
    @Autowired
    DataUser dataUser;
    @Autowired
    DataContent dataContent;
    @Autowired
    DataSite dataSite;
    @Autowired
    SearchQueryService searchQueryService;

    UserModel user;
    SiteModel site;
    FileModel fileA;
    FileModel fileB;

    @BeforeClass (alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();

        STEP("Create a test user and a private site.");
        user = dataUser.createRandomTestUser();
        site = dataSite.usingUser(user).createPrivateRandomSite();

        STEP("Create two test files with highlightable content");
        FileModel fileModelA = new FileModel(FILE_A, FileType.TEXT_PLAIN, CONTENT_A);
        fileA = dataContent.usingUser(user).usingSite(site).createContent(fileModelA);
        FileModel fileModelB = new FileModel(FILE_B, FileType.TEXT_PLAIN, CONTENT_B);
        fileB = dataContent.usingUser(user).usingSite(site).createContent(fileModelB);
    }

    @AfterClass
    public void dataCleanup()
    {
        STEP("Remove test site and user");
        dataSite.usingAdmin().deleteSite(site);
        dataUser.usingAdmin().deleteUser(user);
    }

    @Test (groups = { TestGroup.SEARCH })
    public void testHighlightsInContent()
    {
        STEP("Search for files mentioning 'dog'");
        String query = "cm:content:dog AND SITE:" + site.getId();
        SearchRequest searchRequest = req("afts", query);
        RestRequestHighlightModel highlightModel = RestRequestHighlightModel.builder().fields(List.of("cm:content")).build();
        searchRequest.setHighlight(highlightModel);
        // Configure the expected highlights for each document.
        Predicate<SearchNodeModel> assertionMethod = highlightAssert(
                Map.of(FILE_A, Map.of("cm:content", List.of("The quick brown fox jumps over the lazy <em>dog</em>.")),
                       FILE_B, Map.of("cm:content", List.of("The lazy <em>dog</em> sleeps under the quick brown fox.",
                                                                "The end of the document mentions the <em>dog</em> again!"))));
        searchQueryService.expectAllResultsFromQuery(searchRequest, user, assertionMethod);
    }

    @Test (groups = { TestGroup.SEARCH })
    public void testHighlightsInTwoFields()
    {
        STEP("Search for files with 'file' in the name that mention 'middle'");
        String query = "cm:content:middle AND cm:name:file AND SITE:" + site.getId();
        SearchRequest searchRequest = req("afts", query);
        RestRequestHighlightModel highlightModel = RestRequestHighlightModel.builder().fields(List.of("cm:name", "cm:content")).build();
        searchRequest.setHighlight(highlightModel);
        // Configure the expected highlights for each document.
        Predicate<SearchNodeModel> assertionMethod = highlightAssert(
                Map.of(FILE_B, Map.of("cm:name", List.of("<em>file</em>B.txt"),
                                   "cm:content", List.of("The <em>middle</em> of the document is quite long:\nLorem ipsum dolor sit amet."))));
        searchQueryService.expectAllResultsFromQuery(searchRequest, user, assertionMethod);
    }

    /**
     * Create a predicate that returns true if the highlights for a received document match the given expectation.
     * @param allExpectedHighlights The expected highlights for all documents keyed by document name and then field name.
     * @return The predicate.
     */
    private Predicate<SearchNodeModel> highlightAssert(Map<String, Map<String, List<String>>> allExpectedHighlights)
    {
        return document -> {
            if (!allExpectedHighlights.containsKey(document.getName()))
            {
                LOGGER.error("Unexpected entry in results: {}", document.getName());
                return false;
            }
            Map<String, List<String>> expectedHighlights = allExpectedHighlights.get(document.getName());
            List<ResponseHighlightModel> actualHighlights = document.getSearch().getHighlight();
            Set<String> actualFields = actualHighlights.stream()
                                                       .map(highlight -> highlight.getField())
                                                       .collect(toSet());
            if (!actualFields.equals(expectedHighlights.keySet()))
            {
                LOGGER.error("Unexpected field set for {}: {}", document.getName(), actualFields);
                return false;
            }
            Set<ResponseHighlightModel> expectedHighlightResponse = new HashSet<>();
            for (String expectedField : expectedHighlights.keySet())
            {
                ResponseHighlightModel expectedHighlight = new ResponseHighlightModel();
                List<String> expectedSnippets = expectedHighlights.get(expectedField);
                expectedHighlight.setField(expectedField);
                expectedHighlight.setSnippets(expectedSnippets);
                expectedHighlightResponse.add(expectedHighlight);
            }
            if (!new HashSet<>(actualHighlights).equals(expectedHighlightResponse))
            {
                LOGGER.error("Unexpected highlights for {}, {}", document.getName(), actualHighlights);
                return false;
            }
            return true;
        };
    }
}
