package org.alfresco.elasticsearch;

import static org.alfresco.elasticsearch.SearchQueryService.req;

import java.util.UUID;

import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.TagModel;
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

@ContextConfiguration (locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
public class ElasticsearchTagIndexingTests extends AbstractTestNGSpringContextTests
{
    @Autowired
    ServerHealth serverHealth;

    @Autowired
    DataUser dataUser;

    @Autowired
    private DataContent dataContent;

    @Autowired
    SearchQueryService searchQueryService;

    @Autowired
    protected RestWrapper restClient;

    private UserModel testUser;
    private FolderModel testFolder;

    @BeforeClass (alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();
        testUser = dataUser.createRandomTestUser();
        testFolder = dataContent
                .usingAdmin()
                .usingResource(contentRoot())
                .createFolder(new FolderModel(unique("FOLDER")));
    }

    @TestRail (section = { TestGroup.SEARCH, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION,
            description = "Verify the TAG query doesn't fail on unknown TAG")
    @Test (groups = { TestGroup.SEARCH, TestGroup.TAGS, TestGroup.REGRESSION })
    public void shouldReturnEmptyResultForUnknownTag()
    {
        SearchRequest query = req("TAG:unknown");
        searchQueryService.expectNoResultsFromQuery(query, testUser);
    }

    @TestRail (section = { TestGroup.SEARCH, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION,
            description = "Verify the TAG query doesn't fail on not used TAG")
    @Test (groups = { TestGroup.SEARCH, TestGroup.TAGS, TestGroup.REGRESSION })
    public void shouldReturnEmptyResultForExistingButNotUsedTag() throws Exception
    {
        final FileModel testContent = givenContent("test");
        final String tagName = unique("TAG");

        tagContent(testContent, tagName);

        SearchRequest query = req("TAG:" + tagName);
        searchQueryService.expectNoResultsFromQuery(query, testUser);
    }

    private FileModel givenContent(final String content)
    {
        final FileModel file = new FileModel(unique("FILE"), FileType.TEXT_PLAIN, content);
        return dataContent
                .usingAdmin()
                .usingResource(testFolder)
                .createContent(file);
    }

    private void tagContent(ContentModel content, String ... tagNames)
    {
        final var contentData = dataContent
                .usingAdmin()
                .usingResource(content);
        for (String tag : tagNames)
        {
            var tagModel = new TagModel(tag);
            contentData.addTagToContent(tagModel);
            contentData.assertContentHasTag(content.getCmisLocation(), tagModel);
        }
    }

    private static ContentModel contentRoot()
    {
        final ContentModel root = new ContentModel("-root-");
        root.setNodeRef(root.getName());
        return root;
    }

    private static String unique(String prefix)
    {
        return prefix + "-" + UUID.randomUUID();
    }
}
