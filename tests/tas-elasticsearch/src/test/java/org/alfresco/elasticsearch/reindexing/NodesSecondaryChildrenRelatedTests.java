package org.alfresco.elasticsearch.reindexing;

import static org.alfresco.utility.report.log.Step.STEP;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alfresco.elasticsearch.SearchQueryService;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestNodeBodyMoveCopyModel;
import org.alfresco.rest.model.RestNodeModel;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
    initializers = AlfrescoStackInitializer.class)
public abstract class NodesSecondaryChildrenRelatedTests extends AbstractTestNGSpringContextTests
{
    protected static final String A = "A", B = "B", C = "C", K = "K", L = "L", M = "M", P = "P", Q = "Q", R = "R", S = "S", X = "X", Y = "Y", Z = "Z";

    @Autowired
    private ServerHealth serverHealth;
    @Autowired
    private DataUser dataUser;
    @Autowired
    private DataSite dataSite;
    @Autowired
    private DataContent dataContent;
    @Autowired
    private RestWrapper restClient;
    @Autowired
    protected SearchQueryService searchQueryService;

    protected UserModel testUser;
    private SiteModel testSite;
    private final Folders folders = new TestFolders();

    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        STEP("Verify environment health.");
        serverHealth.isServerReachable();
        serverHealth.assertServerIsOnline();

        STEP("Use the reindexing component to index the system nodes.");
        AlfrescoStackInitializer.reindexEverything();

        STEP("Create a test user and private site.");
        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createPrivateRandomSite();
    }

    @AfterClass(alwaysRun = true)
    public void dataCleanUp()
    {
        STEP("Clean up data after tests.");
        dataSite.usingUser(testUser).deleteSite(testSite);
        dataUser.usingAdmin().deleteUser(testUser);
    }

    protected Folders folders()
    {
        return folders;
    }

    protected Folder folders(String alias)
    {
        return folders.get(alias);
    }

    /** Helper class allowing easy folder management. */
    public class Folder extends FolderModel
    {
        private final UserModel user;

        private Folder()
        {
            super();
            this.user = testUser;
        }

        /** Object construction in fact creates a new folder in system. */
        private Folder(SiteModel site, Folder parent, String name)
        {
            this();
            if (site != null)
            {
                dataContent.usingSite(site);
            }
            if (parent != null)
            {
                dataContent.usingResource(parent);
            }
            FolderModel folderModel = dataContent.usingUser(user).createFolder(new FolderModel(name));
            this.setName(folderModel.getName());
            this.setNodeRef(folderModel.getNodeRef());
            this.setCmisLocation(folderModel.getCmisLocation());
        }

        /** Adds one secondary child to this folder. */
        protected void addSecondaryChild(ContentModel secondaryChild)
        {
            restClient.authenticateUser(user).withCoreAPI().usingNode(this).addSecondaryChild(secondaryChild);
        }

        /** Adds multiple secondary children to this folder. */
        protected void addSecondaryChildren(ContentModel... secondaryChildren)
        {
            restClient.authenticateUser(user).withCoreAPI().usingNode(this).addSecondaryChildren(secondaryChildren);
        }

        /** Removes secondary parent-child association. */
        protected void removeSecondaryChild(ContentModel secondaryChild)
        {
            restClient.authenticateUser(user).withCoreAPI().usingNode(this).removeSecondaryChild(secondaryChild);
        }

        /** Creates nested folder in this folder. */
        protected Folder createNestedFolder(String folderSuffix)
        {
            Folder createdFolder = new Folder(testSite, this, generateRandomFolderNameWith(folderSuffix));
            folders.put(folderSuffix, createdFolder);
            return createdFolder;
        }

        /** Creates multiple nested folders in this folder. */
        protected Map<String, Folder> createNestedFolders(String... folderSuffixes)
        {
            Map<String, Folder> createdFolders = createNestedFolders(this, folderSuffixes);
            folders.putAll(createdFolders);
            return createdFolders;
        }

        private Map<String, Folder> createNestedFolders(Folder node, String... folderSuffixes)
        {
            Map<String, Folder> createdFolders = new TestFolders();
            Stream.of(folderSuffixes).findFirst().ifPresent(folderSuffix -> {
                Folder createdFolder = new Folder(testSite, node, generateRandomFolderNameWith(folderSuffix));
                createdFolders.put(folderSuffix, createdFolder);
                String[] remainingSuffixes = Stream.of(folderSuffixes).skip(1).toArray(String[]::new);
                if (remainingSuffixes.length > 0)
                {
                    createdFolders.putAll(createNestedFolders(createdFolder, remainingSuffixes));
                }
            });

            return createdFolders;
        }

        /** Moves this folder with its content to different folder. */
        protected void moveTo(Folder target)
        {
            RestNodeBodyMoveCopyModel moveModel = new RestNodeBodyMoveCopyModel();
            moveModel.setTargetParentId(target.getNodeRef());
            moveModel.setName(this.getName());

            RestNodeModel movedNode = restClient.authenticateUser(testUser).withCoreAPI().usingNode(this).include("path").move(moveModel);
            this.setCmisLocation(getCmisLocation(movedNode.getPath(), movedNode.getName()));
        }

        /** Copies this folder with its content to different folder. */
        protected Folder copyTo(Folder target)
        {
            RestNodeBodyMoveCopyModel copyModel = new RestNodeBodyMoveCopyModel();
            copyModel.setTargetParentId(target.getNodeRef());
            copyModel.setName(this.getName());

            RestNodeModel nodeCopy = restClient.authenticateUser(testUser).withCoreAPI().usingNode(this).include("path").copy(copyModel);
            Folder folderCopy = new Folder();
            folderCopy.setName(nodeCopy.getName());
            folderCopy.setNodeRef(nodeCopy.getId());
            folderCopy.setCmisLocation(getCmisLocation(nodeCopy.getPath(), nodeCopy.getName()));
            return folderCopy;
        }

        private void delete()
        {
            dataContent.usingUser(user).usingResource(this).deleteContent();
        }

        /** Creates random file in this folder. */
        protected FileModel createRandomDocument()
        {
            return createDocument(generateRandomFileName());
        }

        protected FileModel createDocument(String filename)
        {
            return dataContent.usingUser(user)
                .usingResource(this)
                .createContent(new FileModel(filename, FileType.TEXT_PLAIN, "content"));
        }

        private static String generateRandomFolderNameWith(String folderSuffix)
        {
            return "folder" + folderSuffix + "_" + UUID.randomUUID();
        }

        private static String generateRandomFileName()
        {
            return "TestFile" + UUID.randomUUID() + ".txt";
        }

        private static String getCmisLocation(Object pathMap, String name)
        {
            return Stream.concat(
                    Stream.of(pathMap)
                        .filter(Objects::nonNull)
                        .filter(path -> path instanceof Map)
                        .map(Map.class::cast)
                        .map(path -> path.get("elements"))
                        .filter(Objects::nonNull)
                        .filter(elements -> elements instanceof List)
                        .map(List.class::cast)
                        .flatMap(elements -> (Stream<?>) elements.stream())
                        .skip(1)
                        .filter(element -> element instanceof Map)
                        .map(Map.class::cast)
                        .map(element -> element.get("name"))
                        .filter(Objects::nonNull)
                        .filter(elementName -> elementName instanceof String)
                        .map(String.class::cast),
                    Stream.of(name))
                .collect(Collectors.joining("/", "/", "/"));
        }
    }

    /** Helper {@link Map} containing all created folders and allowing basic operations like creating and deleting a folder. */
    public class TestFolders extends HashMap<String, Folder> implements Folders
    {
        /** Creates a folder in site's Document Library. */
        public Folder createFolder(String folderSuffix)
        {
            Folder createdFolder = new Folder().createNestedFolders((Folder) null, folderSuffix).get(folderSuffix);
            this.put(folderSuffix, createdFolder);
            return createdFolder;
        }

        /** Creates multiple nested folders in site's Document Library. */
        public Map<String, Folder> createNestedFolders(String... folderSuffixes)
        {
            Map<String, Folder> createdFolders = new Folder().createNestedFolders(null, folderSuffixes);
            this.putAll(createdFolders);
            return createdFolders;
        }

        public void delete(Folder folder)
        {
            folder.delete();
            this.remove(folder);
        }
    }

    public interface Folders extends Map<String, Folder>
    {
        Folder createFolder(String folderSuffix);

        Map<String, Folder> createNestedFolders(String... folderSuffixes);

        void delete(Folder folder);
    }
}
