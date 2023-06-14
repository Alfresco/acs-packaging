package org.alfresco.elasticsearch;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.utility.data.RandomData.getRandomFile;
import static org.alfresco.utility.data.RandomData.getRandomName;

import java.util.Map;

import org.alfresco.rest.model.RestTagModel;
import org.alfresco.rest.search.RestRequestDefaultsModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
    initializers = AlfrescoStackInitializer.class)
public class ElasticsearchTemplateSearchTests extends AbstractTestNGSpringContextTests
{
    private static final String SEARCH_PHRASE = "sample";

    @Autowired
    ServerHealth serverHealth;

    @Autowired
    DataUser dataUser;

    @Autowired
    DataContent dataContent;

    @Autowired
    SearchQueryService searchQueryService;

    private UserModel testUser;
    private String fileWithPhraseInName;
    private String fileWithPhraseInContent;
    private String fileWithPhraseInTitle;
    private String fileWithPhraseInDescription;
    private String fileWithPhraseInTag;
    private String fileWithoutPhrase;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();

        fileWithPhraseInName = createContent(SEARCH_PHRASE + ".txt", "some text");
        fileWithPhraseInContent = createContent(getRandomFile(FileType.TEXT_PLAIN), SEARCH_PHRASE + " text");
        fileWithPhraseInTitle = createDummyContentWithTitle(SEARCH_PHRASE);
        fileWithPhraseInDescription = createDummyContentWithDescription(SEARCH_PHRASE);
        fileWithPhraseInTag = createDummyContentWithTag(SEARCH_PHRASE);
        fileWithoutPhrase = createDummyContent();

        testUser = dataUser.createRandomTestUser();
    }

    @Test(groups = { TestGroup.SEARCH })
    public void testAftsQuery_simpleTemplate()
    {
        Map<String, String> templates = Map.of("_NODE", "%(cm:name cm:description)");
        String query = "TYPE:cm:content AND _NODE:" + SEARCH_PHRASE;
        SearchRequest request = req("afts", query, templates);

        searchQueryService.expectNodeRefsFromQuery(request, testUser, fileWithPhraseInName, fileWithPhraseInDescription);
    }

    @Test(groups = { TestGroup.SEARCH })
    public void testAfteQuery_nestedTemplate()
    {
        Map<String, String> templates = Map.of("_NODE", "%(cm:name cm:description)", "_NODEX", "%(_NODE cm:title TAG)");
        String query = "TYPE:cm:content AND _NODEX:" + SEARCH_PHRASE;
        SearchRequest request = req("afts", query, templates);

        searchQueryService.expectNodeRefsFromQuery(request, testUser, fileWithPhraseInName, fileWithPhraseInDescription, fileWithPhraseInTitle, fileWithPhraseInTag);
    }

    @Test(groups = { TestGroup.SEARCH })
    public void testAftsQuery_simpleTemplateAndDefaultFieldName()
    {
        Map<String, String> templates = Map.of("_NODE", "%(cm:name cm:description TEXT)");
        String query = "TYPE:cm:content AND " + SEARCH_PHRASE;
        SearchRequest request = req("afts", query, templates);
        request.setDefaults(RestRequestDefaultsModel.builder().defaultFieldName("_NODE").create());

        searchQueryService.expectNodeRefsFromQuery(request, testUser, fileWithPhraseInName, fileWithPhraseInDescription, fileWithPhraseInContent);
    }

    /* ********************* Helper methods ********************* */

    private String createContent(String filename, String content)
    {
        return createContent(filename, content, null, null, null);
    }

    private String createDummyContentWithTitle(String title)
    {
        return createDummyContent(title, null, null);
    }

    private String createDummyContentWithDescription(String description)
    {
        return createDummyContent(null, description, null);
    }

    private String createDummyContentWithTag(String tag)
    {
        return createDummyContent(null, null, tag);
    }

    private String createDummyContent()
    {
        return createDummyContent(null, null, null);
    }

    private String createDummyContent(String title, String description, String tag)
    {
        return createContent(getRandomFile(FileType.TEXT_PLAIN), getRandomName("dummy text"), title, description, tag);
    }

    private String createContent(String filename, String content, String title, String description, String tag)
    {
        final ContentModel contentRoot = new ContentModel("-root-");
        contentRoot.setNodeRef(contentRoot.getName());
        FileModel fileModel = new FileModel(filename, FileType.TEXT_PLAIN, content);
        fileModel.setTitle(title);
        fileModel.setDescription(description);

        FileModel file = dataContent
            .usingAdmin()
            .usingResource(contentRoot)
            .createContent(fileModel);

        if (StringUtils.isNotBlank(tag))
        {
            dataContent.usingAdmin().usingResource(file).addTagToContent(RestTagModel.builder().tag(tag).create());
        }

        return file.getNodeRef();
    }
}
