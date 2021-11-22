package org.alfresco.elasticsearch;

import static org.alfresco.elasticsearch.SearchQueryService.req;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.alfresco.dataprep.AlfrescoHttpClientFactory;
import org.alfresco.dataprep.ContentActions;
import org.alfresco.dataprep.SiteService.Visibility;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.report.log.Step;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.logging.log4j.util.Strings;
import org.apache.chemistry.opencmis.client.api.Session;
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
    private static final FileModel DOCUMENT_LIBRARY = new FileModel("documentLibrary");
    private static final String FILENAME_PREFIX = "EsSiteTest";
    private static final String FILE_CONTENT_CONDITION = " TEXT:" + FILENAME_PREFIX + "*";
    private static final String SAMPLE_SITE_ID = "swsdp";
    private static final String ALL_SITES = "_ALL_SITES_ ";
    private static final String EVERYTHING = "_EVERYTHING_ ";

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
    private UserModel siteCreator;
    private FolderModel testFolder;

    @BeforeClass (alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();

        Step.STEP("Index system nodes.");
        AlfrescoStackInitializer.reindexEverything();

        Step.STEP("Create test users and a folder.");
        testUser = dataUser.createRandomTestUser();
        siteCreator = dataUser.createRandomTestUser();
        testFolder = dataContent
                .usingAdmin()
                .usingResource(contentRoot())
                .createFolder(new FolderModel(unique("FOLDER")));
    }

    @TestRail (section = { TestGroup.SEARCH, TestGroup.SITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify the SITE queries work correctly")
    @Test (groups = { TestGroup.SEARCH, TestGroup.SITES, TestGroup.REGRESSION })
    public void testSiteUseCasesForCreateModifyDeleteSite()
    {
        // Remove the automatically created Sample Site
        deleteSite(SAMPLE_SITE_ID);

        Step.STEP("Site creation use cases");

        // Check there are no files within or without a site, that have the given filename prefix
        assertSiteQueryResult(ALL_SITES, List.of());
        assertSiteQueryResult(EVERYTHING, "AND", FILE_CONTENT_CONDITION, List.of());

        // Create a file with the given filename prefix, outside any site
        FileModel fileNotInSite = new FileModel(unique(FILENAME_PREFIX) + ".txt", FileType.TEXT_PLAIN, "Content for fileNotInSite");
        fileNotInSite = dataContent
                .usingAdmin()
                .usingResource(testFolder)
                .createContent(fileNotInSite);

        // Search with condition of filename prefix - we should see the file using EVERYTHING but not from ALL_SITES
        assertSiteQueryResult(ALL_SITES, "AND", FILE_CONTENT_CONDITION, List.of());
        assertSiteQueryResult(EVERYTHING, "AND", FILE_CONTENT_CONDITION, List.of(fileNotInSite));

        // No sites should exist - expect no results
        assertSiteQueryResult(ALL_SITES, List.of());
        assertSiteQueryResult(unique("NoSuchSite"), List.of());

        // Create one empty public site - expect no results other than document library
        SiteModel testSite1 = createPublicSite();
        assertSiteQueryResult(testSite1.getId(), List.of(DOCUMENT_LIBRARY));
        assertSiteQueryResult(ALL_SITES, List.of(DOCUMENT_LIBRARY));
        assertSiteQueryResult(EVERYTHING, "AND", FILE_CONTENT_CONDITION, List.of(fileNotInSite));

        // Create a file in public site - expect one file plus the document library.
        FileModel file1 = createContentInSite(testSite1, FILENAME_PREFIX + "test1");
        assertSiteQueryResult(testSite1.getId(), List.of(DOCUMENT_LIBRARY, file1));
        assertSiteQueryResult(ALL_SITES, List.of(DOCUMENT_LIBRARY, file1));
        assertSiteQueryResult(EVERYTHING, "AND", FILE_CONTENT_CONDITION, List.of(fileNotInSite, file1));

        // Create another file in public site - expect two files plus the document library.
        FileModel file2 = createContentInSite(testSite1, FILENAME_PREFIX + "test2");
        assertSiteQueryResult(testSite1.getId(), List.of(DOCUMENT_LIBRARY, file1, file2));
        assertSiteQueryResult(ALL_SITES, List.of(DOCUMENT_LIBRARY, file1, file2));
        assertSiteQueryResult(EVERYTHING, "AND", FILE_CONTENT_CONDITION, List.of(fileNotInSite, file1, file2));

        // Create a second public site, empty - expect no results other than document library
        SiteModel testSite2 = createPublicSite();
        assertSiteQueryResult(testSite2.getId(), List.of(DOCUMENT_LIBRARY));
        assertSiteQueryResult(testSite1.getId(), List.of(DOCUMENT_LIBRARY, file1, file2));
        assertSiteQueryResult(ALL_SITES, List.of(DOCUMENT_LIBRARY, file1, file2));
        assertSiteQueryResult(EVERYTHING, "AND", FILE_CONTENT_CONDITION, List.of(fileNotInSite, file1, file2));

        // Create a file in second public site - expect one file plus the document library.
        FileModel file3 = createContentInSite(testSite2, FILENAME_PREFIX + "test3");
        assertSiteQueryResult(testSite2.getId(), List.of(DOCUMENT_LIBRARY, file3));
        assertSiteQueryResult(testSite1.getId(), List.of(DOCUMENT_LIBRARY, file1, file2));
        assertSiteQueryResult(ALL_SITES, List.of(DOCUMENT_LIBRARY, file1, file2, file3));
        assertSiteQueryResult(EVERYTHING, "AND", FILE_CONTENT_CONDITION, List.of(fileNotInSite, file1, file2, file3));

        // Create another file in second public site - expect two files plus the document library.
        FileModel file4 = createContentInSite(testSite2, FILENAME_PREFIX + "test4");
        assertSiteQueryResult(testSite2.getId(), List.of(DOCUMENT_LIBRARY, file3, file4));
        assertSiteQueryResult(testSite1.getId(), List.of(DOCUMENT_LIBRARY, file1, file2));
        assertSiteQueryResult(ALL_SITES, List.of(DOCUMENT_LIBRARY, file1, file2, file3, file4));
        assertSiteQueryResult(EVERYTHING, "AND", FILE_CONTENT_CONDITION, List.of(fileNotInSite, file1, file2, file3, file4));

        // Test disjunction and conjunction using SITE: and SITE:
        assertSiteQueryResult(testSite1.getId(), "OR", " SITE:" + testSite2.getId(), List.of(DOCUMENT_LIBRARY, file1, file2, file3, file4));
        assertSiteQueryResult(testSite1.getId(), "OR", " SITE:" + unique("NoSuchSite"), List.of(DOCUMENT_LIBRARY, file1, file2));
        assertSiteQueryResult(testSite1.getId(), "AND", " SITE:" + unique("NoSuchSite"), List.of());

        // Test conjunction using SITE: and TEXT:
        assertSiteQueryResult(testSite1.getId(), "AND", " TEXT:" + FILENAME_PREFIX + "test2*", List.of(file2));
        assertSiteQueryResult(testSite1.getId(), "AND", " TEXT:" + FILENAME_PREFIX + "testX*", List.of());
        assertSiteQueryResult(EVERYTHING, "AND", " TEXT:" + FILENAME_PREFIX + "test2*", List.of(file2));
        assertSiteQueryResult(EVERYTHING, "AND", " TEXT:" + FILENAME_PREFIX + "testX*", List.of());
        assertSiteQueryResult(ALL_SITES, "AND", " TEXT:" + FILENAME_PREFIX + "test2*", List.of(file2));
        assertSiteQueryResult(ALL_SITES, "AND", " TEXT:" + FILENAME_PREFIX + "testX*", List.of());

        // Test modify site
        Step.STEP("Site modification use cases");

        // Verify site creating user can see all files in both public sites
        assertSiteQueryResult(siteCreator, testSite1.getId(), List.of(DOCUMENT_LIBRARY, file1, file2));
        assertSiteQueryResult(siteCreator, testSite2.getId(), List.of(DOCUMENT_LIBRARY, file3, file4));
        assertSiteQueryResult(siteCreator, ALL_SITES, List.of(DOCUMENT_LIBRARY, file1, file2, file3, file4));
        assertSiteQueryResult(siteCreator, EVERYTHING, "AND", FILE_CONTENT_CONDITION, List.of(fileNotInSite, file1, file2, file3, file4));

        // Verify user who is not a member of any sites can see all files in both public sites
        UserModel publicUser = dataUser.createRandomTestUser();
        assertSiteQueryResult(publicUser, testSite1.getId(), List.of(DOCUMENT_LIBRARY, file1, file2));
        assertSiteQueryResult(publicUser, testSite2.getId(), List.of(DOCUMENT_LIBRARY, file3, file4));
        assertSiteQueryResult(publicUser, ALL_SITES, List.of(DOCUMENT_LIBRARY, file1, file2, file3, file4));
        assertSiteQueryResult(publicUser, EVERYTHING, "AND", FILE_CONTENT_CONDITION, List.of(fileNotInSite, file1, file2, file3, file4));

        // Change first site's visibility to private - verify site creator can still see all files in both sites
        dataSite.usingUser(siteCreator).updateSiteVisibility(testSite1, Visibility.PRIVATE);
        assertSiteQueryResult(siteCreator, testSite1.getId(), List.of(DOCUMENT_LIBRARY, file1, file2));
        assertSiteQueryResult(siteCreator, testSite2.getId(), List.of(DOCUMENT_LIBRARY, file3, file4));
        assertSiteQueryResult(siteCreator, ALL_SITES, List.of(DOCUMENT_LIBRARY, file1, file2, file3, file4));
        assertSiteQueryResult(siteCreator, EVERYTHING, "AND", FILE_CONTENT_CONDITION, List.of(fileNotInSite, file1, file2, file3, file4));

        // Verify user who is not a member of any sites can see files in public site but not see files in private site
        assertSiteQueryResult(publicUser, testSite1.getId(), List.of());
        assertSiteQueryResult(publicUser, testSite2.getId(), List.of(DOCUMENT_LIBRARY, file3, file4));
        assertSiteQueryResult(publicUser, ALL_SITES, List.of(DOCUMENT_LIBRARY, file3, file4));
        assertSiteQueryResult(publicUser, EVERYTHING, "AND", FILE_CONTENT_CONDITION, List.of(fileNotInSite, file3, file4));

        // Change site visibility back to public - user who is not a member of any sites can see files in both sites again
        dataSite.usingUser(siteCreator).updateSiteVisibility(testSite1, Visibility.PUBLIC);
        assertSiteQueryResult(publicUser, testSite1.getId(), List.of(DOCUMENT_LIBRARY, file1, file2));
        assertSiteQueryResult(publicUser, testSite2.getId(), List.of(DOCUMENT_LIBRARY, file3, file4));
        assertSiteQueryResult(publicUser, ALL_SITES, List.of(DOCUMENT_LIBRARY, file1, file2, file3, file4));
        assertSiteQueryResult(publicUser, EVERYTHING, "AND", FILE_CONTENT_CONDITION, List.of(fileNotInSite, file1, file2, file3, file4));

        // Test delete site
        Step.STEP("Site deletion use cases");

        // Delete one site - expect no results for that site
        deleteSite(testSite1.getId());
        assertSiteQueryResult(testSite1.getId(), List.of());
        assertSiteQueryResult(testSite2.getId(), List.of(DOCUMENT_LIBRARY, file3, file4));
        assertSiteQueryResult(ALL_SITES, List.of(DOCUMENT_LIBRARY, file3, file4));
        assertSiteQueryResult(EVERYTHING, "AND", FILE_CONTENT_CONDITION, List.of(fileNotInSite, file3, file4));

        // Delete remaining site - expect no results
        deleteSite(testSite2.getId());
        assertSiteQueryResult(testSite2.getId(), List.of());
        assertSiteQueryResult(testSite1.getId(), List.of());
        assertSiteQueryResult(ALL_SITES, List.of());
        assertSiteQueryResult(EVERYTHING, "AND", FILE_CONTENT_CONDITION, List.of(fileNotInSite));

        Step.STEP("Moving files between sites and modifying content use cases");

        SiteModel publicSite1 = dataSite.usingUser(testUser).createSite(new SiteModel("public-site-1"));
        SiteModel publicSite2 = dataSite.usingUser(testUser).createSite(new SiteModel("public-site-2"));
        FileModel file5 = createContentInSite(publicSite1, "file5");

        //Moving a file between sites.
        moveFile(file5, publicSite1, publicSite2);
        assertSiteQueryResult(publicSite1.getId(), List.of(DOCUMENT_LIBRARY));
        assertSiteQueryResult(publicSite2.getId(), List.of(DOCUMENT_LIBRARY, file5));

        //Moving a file out of a site.
        moveFileOutsideOfSite(file5, publicSite2, testFolder);
        assertSiteQueryResult(publicSite2.getId(), List.of(DOCUMENT_LIBRARY));

        //Removing file.
        FileModel file6 = createContentInSite(publicSite2, "file6");
        assertSiteQueryResult(publicSite2.getId(), List.of(DOCUMENT_LIBRARY, file6));
        deleteFile(file6);
        assertSiteQueryResult(publicSite2.getId(), List.of(DOCUMENT_LIBRARY));

        //Document modification.
        FileModel file7 = createContentInSite(publicSite2, "file7", "Initial Content.");
        assertContentInGivenFileUnderSiteQueryResult(publicSite2.getId(), "initial", List.of(file7));
        modifyDocument(file7, "Modified Content.");
        assertContentInGivenFileUnderSiteQueryResult(publicSite2.getId(), "modified", List.of(file7));
    }

    private void assertSiteQueryResult(String siteName, Collection<ContentModel> contentModels)
    {
        assertSiteQueryResult(testUser, siteName, contentModels);
    }

    private void assertSiteQueryResult(UserModel user, String siteName, Collection<ContentModel> contentModels)
    {
        final List<String> contentNames = contentModels
                .stream()
                .map(ContentModel::getName)
                .collect(Collectors.toList());

        for (final String language : LANGUAGES_TO_CHECK)
        {
            Step.STEP("Searching for SITE `" + siteName + "` using `" + language + "` language.");
            final SearchRequest query = req(language, "SITE:" + siteName + " ");
            if (contentNames.isEmpty())
            {
                searchQueryService.expectNoResultsFromQuery(query, user);
            }
            else
            {
                Collections.shuffle(contentNames);
                searchQueryService.expectResultsFromQuery(query, user, contentNames.toArray(String[]::new));
            }
        }
    }

    private void assertSiteQueryResult(String site1Name, String operator, String condition, Collection<ContentModel> contentModels)
    {
        assertSiteQueryResult(testUser, site1Name, operator, condition, contentModels);
    }

    private void assertSiteQueryResult(UserModel user, String site1Name, String operator, String condition, Collection<ContentModel> contentModels)
    {
        final List<String> contentNames = contentModels
                .stream()
                .map(ContentModel::getName)
                .collect(Collectors.toList());

        for (final String language : LANGUAGES_TO_CHECK)
        {
            Step.STEP("Searching for SITE `" + site1Name + "` " + operator + " `" + condition + "` using `" + language + "` language.");
            final SearchRequest query = req(language, "SITE:" + site1Name + " "  + operator + condition);
            if (contentNames.isEmpty())
            {
                searchQueryService.expectNoResultsFromQuery(query, user);
            }
            else
            {
                Collections.shuffle(contentNames);
                searchQueryService.expectResultsFromQuery(query, user, contentNames.toArray(String[]::new));
            }
        }
    }

    private void assertContentInGivenFileUnderSiteQueryResult(String siteName, String content, Collection<ContentModel> contentModels)
    {
        final List<String> contentNames = contentModels
                .stream()
                .map(ContentModel::getName)
                .collect(Collectors.toList());
        for (final String language : LANGUAGES_TO_CHECK)
        {
            Step.STEP("Searching for SITE `" + siteName + "` and content:'" + content + "' using `" + language + "` language.");
            final SearchRequest query = req(language, "SITE:" + siteName + " AND TEXT:" + content + " ");
            searchQueryService.expectResultsFromQuery(query, testUser, contentNames.toArray(String[]::new));
        }
    }

    private void modifyDocument(FileModel file, String newContent)
    {
        dataContent.usingUser(testUser).usingResource(file)
                .updateContent(newContent);
    }

    private void deleteFile(ContentModel contentModel)
    {
        dataContent
                .usingAdmin()
                .usingResource(contentModel)
                .deleteContent();
    }

    private void moveFile(FileModel file, SiteModel sourceSite, SiteModel targetSite)
    {
        moveFile(file, sourceSite, targetSite, null);
    }

    private void moveFile(FileModel file, SiteModel sourceSite, SiteModel targetSite, ContentModel targetFolder)
    {
        Session session = dataContent.getContentActions().getCMISSession(testUser.getUsername(), testUser.getPassword());
        dataContent.usingUser(testUser).getContentActions()
                .moveTo(
                        session,
                        sourceSite.getId(),
                        file.getName(),
                        targetSite.getId(),
                        targetFolder != null ? targetFolder.getName() : Strings.EMPTY //if empty then target folder is documentLibrary (which is default target location)
                );
    }

    private void moveFileOutsideOfSite(FileModel file, SiteModel sourceSite, ContentModel targetContentModel)
    {
        Session session = dataContent.getContentActions().getCMISSession(alfrescoHttpClientFactory.getAdminUser(), alfrescoHttpClientFactory.getAdminPassword());
        ContentActions actions = dataContent.usingAdmin().getContentActions();
        CmisObject objFrom = actions.getCmisObject(session, sourceSite.getId(), file.getName());
        CmisObject objTarget = actions.getCmisObject(session, targetContentModel.getCmisLocation());

        List parents;
        CmisObject parent;
        if (objFrom instanceof Document) {
            Document d = (Document)objFrom;
            parents = d.getParents();
            parent = session.getObject(((Folder)parents.get(0)).getId());
            d.move(parent, objTarget);
        } else if (objFrom instanceof Folder) {
            Folder f = (Folder)objFrom;
            parents = f.getParents();
            parent = session.getObject(((Folder)parents.get(0)).getId());
            f.move(parent, objTarget);
        }
    }

    private FileModel createContentInSite(SiteModel site, String fileName)
    {
        return createContentInSite(site, fileName, "Content for " + fileName);
    }

    private FileModel createContentInSite(SiteModel site, String fileName, String content)
    {
        final FileModel file = new FileModel(unique(fileName) + ".txt", FileType.TEXT_PLAIN, content);
        return dataContent.usingUser(siteCreator)
                .usingSite(site)
                .createContent(file);
    }

    private SiteModel createPublicSite()
    {
        SiteModel createdSite = dataSite.usingUser(siteCreator).createPublicRandomSite();
        Step.STEP("Created public site '" + createdSite.getId() + "'.");
        return createdSite;
    }

    private void deleteSite(String siteId)
    {
        Step.STEP("Deleting site '" + siteId + "', if it exists");
        SiteModel siteToDelete = new SiteModel(siteId);
        if (dataSite.usingAdmin().isSiteCreated(siteToDelete))
        {
            dataSite.usingAdmin().deleteSite(siteToDelete);
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