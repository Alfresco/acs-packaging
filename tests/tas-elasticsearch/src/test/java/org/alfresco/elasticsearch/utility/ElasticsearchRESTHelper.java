package org.alfresco.elasticsearch.utility;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.utility.model.FileType.TEXT_PLAIN;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.alfresco.elasticsearch.SearchQueryService;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestCategoryLinkBodyModel;
import org.alfresco.rest.model.RestCategoryModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Elasticsearch Connector cannot index certain changes immediately and so it is often necessary to wait for an
 * index refresh to run before moving on to the next step. In particular path indexing can get very confused if the
 * tests send requests too quickly. The methods in this helper class aim will check that the index refresh has
 * completed before returning. See ACS-4637 for more details.
 */
public class ElasticsearchRESTHelper
{
    /** The alias for the root of the category hierarchy. */
    public static final String ROOT_CATEGORY_ALIAS = "-root-";
    /** The root of the category hierarchy. */
    private static final RestCategoryModel ROOT_CATEGORY = RestCategoryModel.builder().id(ROOT_CATEGORY_ALIAS).name(ROOT_CATEGORY_ALIAS).create();

    @Autowired
    private RestWrapper client;
    @Autowired
    private DataSite dataSite;
    @Autowired
    private DataContent dataContent;
    @Autowired
    private DataUser dataUser;
    @Autowired
    private SearchQueryService searchQueryService;

    /**
     * Create a private site.
     *
     * @param user The user to use.
     * @return The new site.
     */
    public SiteModel createPrivateSite(UserModel user)
    {
        SiteModel site = dataSite.usingUser(user).createPrivateRandomSite();
        SearchRequest request = req("PATH:\"/app:company_home/st:sites/cm:" + site.getId() + "\"");
        searchQueryService.expectResultsInclude(request, user, site.getId());
        return site;
    }

    /**
     * Create a folder in a site.
     *
     * @param user The user to use.
     * @param site The site to create the folder in.
     * @return The new folder.
     */
    public FolderModel createFolderInSite(UserModel user, SiteModel site)
    {
        FolderModel folder = dataContent.usingUser(user).usingSite(site).createFolder();
        waitForIndexing(user, pathQueryInSite(site, folder.getName()), folder.getName());
        return folder;
    }

    /**
     * Create a text file in a site.
     *
     * @param user The user to use.
     * @param site The site to create the file in.
     * @return The new file.
     */
    public FileModel createFileInSite(UserModel user, SiteModel site)
    {
        FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
        FileModel file = dataContent.usingUser(user).usingSite(site).createContent(fileModel);
        waitForIndexing(user, pathQueryInSite(site, file.getName()), file.getName());
        return file;
    }

    /**
     * Create a file in a folder.
     *
     * @param user The user to use.
     * @param folder The folder to create the file in.
     * @return The new file.
     */
    public FileModel createFileInFolder(UserModel user, FolderModel folder)
    {
        FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
        FileModel file = dataContent.usingUser(user).usingResource(folder).createContent(fileModel);
        waitForIndexing(user, pathQueryInFolder(folder, file.getName()), file.getName());
        return file;
    }

    /**
     * Create a category.
     *
     * @param ancestorCategories The path of categories between the root and the new category, or leave blank to
     * create the category at the "-root-".
     * @return The newly created category.
     */
    public RestCategoryModel createCategory(RestCategoryModel... ancestorCategories)
    {
        RestCategoryModel parent = (ancestorCategories.length > 0 ? ancestorCategories[ancestorCategories.length - 1] : ROOT_CATEGORY);
        RestCategoryModel category = client.authenticateUser(dataUser.getAdminUser()).withCoreAPI()
                                           .usingCategory(parent)
                                           .createSingleCategory(RestCategoryModel.builder().name(RandomData.getRandomAlphanumeric()).create());
        List<String> pathElements = stream(ancestorCategories).map(RestCategoryModel::getName).collect(toList());
        waitForIndexing(dataUser.getAdminUser(), categoryPath(pathElements, category.getName()), category.getName());
        return category;
    }

    /**
     * Link a file or folder to a category.
     *
     * @param user The user who should create the link.
     * @param node The file or folder to be linked.
     * @param categoryHierarchy The full list of categories from the root (excluding "-root-") to the category to use.
     * @return The category that was linked to.
     */
    public RestCategoryModel linkToCategory(UserModel user, ContentModel node, RestCategoryModel... categoryHierarchy)
    {
        RestCategoryModel linkedToCategory = (categoryHierarchy.length > 0 ? categoryHierarchy[categoryHierarchy.length - 1] : ROOT_CATEGORY);
        RestCategoryModel createdCategory = client.authenticateUser(user).withCoreAPI().usingNode(node)
                                                  .linkToCategory(RestCategoryLinkBodyModel.builder().categoryId(linkedToCategory.getId()).create());
        List<String> pathElements = stream(categoryHierarchy).map(RestCategoryModel::getName).collect(Collectors.toCollection(ArrayList::new));
        waitForIndexing(user, categoryPath(pathElements, node.getName()), node.getName());
        return createdCategory;
    }

    /**
     * Unlink a node from a category.
     *
     * @param user The user who should remove the link.
     * @param node The node to unlink.
     * @param categoryHierarchy The full list of categories from the root (excluding "-root-") to the category to use.
     */
    public void unlinkFromCategory(UserModel user, ContentModel node, RestCategoryModel... categoryHierarchy)
    {
        RestCategoryModel linkedToCategory = (categoryHierarchy.length > 0 ? categoryHierarchy[categoryHierarchy.length - 1] : ROOT_CATEGORY);
        client.authenticateUser(user).withCoreAPI().usingNode(node).unlinkFromCategory(linkedToCategory.getId());
        List<String> pathElements = stream(categoryHierarchy).map(RestCategoryModel::getName).collect(Collectors.toCollection(ArrayList::new));
        waitForIndexing(user, categoryPath(pathElements, node.getName()));
    }

    /**
     * Create an AFTS path query for a file or folder in a site.
     *
     * @param site The site object.
     * @param documentLibraryNames The list of names of nodes from the document library to the target file or folder.
     * @return An absolute path suitable for use in a path query.
     */
    private String pathQueryInSite(SiteModel site, String... documentLibraryNames)
    {
        String path = "/app:company_home/st:sites/cm:" + site.getId() + "/cm:documentLibrary/cm:" + stream(documentLibraryNames).collect(joining("/cm:"));
        return "PATH:\"" + path + "\"";
    }

    /**
     * Create an AFTS path query from the CMIS location stored in a folder and the name of a newly created child of
     * the folder.
     * <p>
     * For example the CMIS location "/Sites/sitePrivate-aybHSuANdKtyWiW/documentLibrary/Folder-JTZwAKTyqmGTjUq/"
     * and file name File-WufmPkLqwvVxnoA has to be converted to an AFTS path query like
     * PATH:"/app:company_home/st:sites/cm:sitePrivate-aybHSuANdKtyWiW/cm:documentLibrary/Folder-JTZwAKTyqmGTjUq/cm:File-WufmPkLqwvVxnoA"
     *
     * @param folder The folder that should contain the item.
     * @param name The name of the item in the folder.
     * @return An AFTS query to find the item in the new location.
     */
    private String pathQueryInFolder(FolderModel folder, String name)
    {
        String tail = folder.getCmisLocation().split("/Sites", 2)[1];
        String aftsPath = "/app:company_home/st:sites" + tail.replace("/", "/cm:") + name;
        return "PATH:\"" + aftsPath + "\"";
    }

    /**
     * Create a path to a file or folder via the category hierarchy.
     *
     * @param categoryHierarchy The ordered list of categories from the root category (excluding -root-) to the node
     * that was categorised.
     * @return An absolute path through the categories to the specified node.
     */
    private String categoryPath(List<String> categoryHierarchy, String child)
    {
        return "PATH:\"/cm:categoryRoot/cm:generalclassifiable"
                + categoryHierarchy.stream().map(name -> "/cm:" + name).collect(joining(""))
                + "/cm:" + child + "\"";
    }

    /**
     * Wait for indexing to complete. This is needed to overcome the index refresh interval - see ACS-4637.
     */
    private void waitForIndexing(UserModel user, String expectedQuery, String... expectedFileNames)
    {
        SearchRequest query = req(expectedQuery);
        searchQueryService.expectResultsFromQuery(query, user, expectedFileNames);
    }
}
