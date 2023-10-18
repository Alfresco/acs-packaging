package org.alfresco.elasticsearch.reindexing;

import static java.lang.String.format;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.utility.report.log.Step.STEP;

import org.alfresco.elasticsearch.reindexing.utils.Categories;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.utility.model.TestGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests verifying live indexing of secondary children and ANCESTOR index in Elasticsearch.
 */
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.JUnit4TestShouldUseTestAnnotation"}) // these are testng tests and use searchQueryService.expectResultsFromQuery for assertion
public class NodeWithCategoryIndexingTests extends NodesSecondaryChildrenRelatedTests
{

    @Autowired
    private Categories categories;

    /*
    A --- B  (folders)
    \____
         \
    K --- L  (categories)
     */
    @BeforeClass(alwaysRun = true)
    @Override
    public void dataPreparation()
    {
        super.dataPreparation();

        // given
        STEP("Create nested folders in site's Document Library.");
        folders().createNestedFolders(A, B);

        STEP("Create nested categories.");
        categories.createNestedCategories(K, L);

        STEP("Link folders to category.");
        folders(A).linkToCategory(categories.get(L));
    }

    @Test(groups = TestGroup.SEARCH)
    public void testParentQueryAgainstCategory()
    {
        // then
        STEP("Verify that searching by PARENT and category will find one descendant node: categoryL.");
        SearchRequest query = req("PARENT:" + categories.get(K).getId());
        searchQueryService.expectResultsFromQuery(query, testUser, categories.get(L).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testParentQueryAgainstFolder()
    {
        // then
        STEP("Verify that searching by PARENT and category will find one descendant node: folderA.");
        SearchRequest query = req("PARENT:" + categories.get(L).getId());
        searchQueryService.expectResultsFromQuery(query, testUser, folders(A).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testParentQueryAgainstFolderAfterCategoryDeletion()
    {
        // given
        STEP("Create nested folders in site's Document Library.");
        folders().createNestedFolders(C);

        STEP("Create nested categories.");
        categories.createNestedCategories(M);

        STEP("Link folders to category.");
        folders(C).linkToCategory(categories.get(M));

        // when
        STEP("Verify that searching by PARENT and category will find one descendant node: folderC.");
        SearchRequest query = req("PARENT:" + categories.get(M).getId());
        searchQueryService.expectResultsFromQuery(query, testUser, folders(C).getName());

        // then
        STEP("Delete categoryM.");
        categories.delete(M);

        STEP("Verify that searching by PARENT and deleted category will find no descendant nodes.");
        searchQueryService.expectResultsFromQuery(query, testUser);
    }


    @Test(groups = TestGroup.SEARCH)
    public void testAncestorQuery()
    {
        // then
        STEP("Verify that searching by ANCESTOR and category will find one descendant node: folderA.");
        SearchRequest query = req("ANCESTOR:" + categories.get(L).getId());
        searchQueryService.expectResultsFromQuery(query, testUser, folders(A).getName());
    }


    @Test(groups = TestGroup.SEARCH)
    public void testAncestorQuery_byParentCategory()
    {
        // then
        STEP("Verify that searching by ANCESTOR and category will find one descendant node: folderA.");
        SearchRequest query = req("ANCESTOR:" + categories.get(K).getId());
        searchQueryService.expectResultsFromQuery(query, testUser, categories.get(L).getName(), folders(A).getName());
    }


    @Test(groups = TestGroup.SEARCH)
    public void testAncestorQueryAgainstFolderAfterCategoryDeletion()
    {
        // given
        STEP("Create nested folders in site's Document Library.");
        folders().createNestedFolders(C);

        STEP("Create nested categories.");
        categories.createNestedCategories(M);

        STEP("Link folders to category.");
        folders(C).linkToCategory(categories.get(M));

        // when
        STEP("Verify that searching by PARENT and category will find one descendant node: folderC.");
        SearchRequest query = req("ANCESTOR:" + categories.get(M).getId());
        searchQueryService.expectResultsFromQuery(query, testUser, folders(C).getName());

        // then
        STEP("Delete categoryM.");
        categories.delete(M);

        STEP("Verify that searching by PARENT and deleted category will find no descendant nodes.");
        searchQueryService.expectResultsFromQuery(query, testUser);
    }


    @Test(groups = TestGroup.SEARCH)
    public void testAncestorQueryAgainstFolderAfterChildCategoryDeletion()
    {
        // given
        STEP("Create nested folders in site's Document Library.");
        folders().createNestedFolders(C);

        STEP("Create nested categories.");
        categories.createNestedCategories(P, Q);

        STEP("Link folders to category.");
        folders(C).linkToCategory(categories.get(P));
        folders(C).linkToCategory(categories.get(Q));

        // when
        STEP("Verify that searching by ANCESTOR and category will find one descendant node: folderC.");
        SearchRequest queryBy_P = req("ANCESTOR:" + categories.get(P).getId());
        SearchRequest queryBy_Q = req("ANCESTOR:" + categories.get(Q).getId());

        searchQueryService.expectResultsFromQuery(queryBy_P, testUser, categories.get(Q).getName(), folders(C).getName());
        searchQueryService.expectResultsFromQuery(queryBy_Q, testUser, folders(C).getName());

        // then
        STEP("Delete categoryQ.");
        categories.delete(Q);

        STEP("Verify that searching by ANCESTOR and parent category will find folderC.");
        searchQueryService.expectResultsFromQuery(queryBy_Q, testUser, folders(C).getName());

        STEP("Verify that searching by ANCESTOR and deleted category will find no descendant nodes.");
        searchQueryService.expectResultsFromQuery(queryBy_P, testUser);
    }


    @Test(groups = TestGroup.SEARCH)
    public void testAncestorQueryAgainstFolderAfterParentCategoryDeletion()
    {
        // given
        STEP("Create nested folders in site's Document Library.");
        folders().createNestedFolders(C);

        STEP("Create nested categories.");
        categories.createNestedCategories(P, Q);

        STEP("Link folders to category.");
        folders(C).linkToCategory(categories.get(Q));

        // when
        STEP("Verify that searching by ANCESTOR and category will find one descendant node: folderC.");
        SearchRequest query = req("ANCESTOR:" + categories.get(Q).getId());
        searchQueryService.expectResultsFromQuery(query, testUser, folders(C).getName());

        // then
        STEP("Delete categoryM.");
        categories.delete(P);

        STEP("Verify that searching by ANCESTOR and deleted category will find no descendant nodes.");
        searchQueryService.expectResultsFromQuery(query, testUser);
    }


    @Test(groups = TestGroup.SEARCH)
    public void testParentQueryAgainstFolderAfterParentCategoryDeletion()
    {
        // given
        STEP("Create nested folders in site's Document Library.");
        folders().createNestedFolders(C);

        STEP("Create nested categories.");
        categories.createNestedCategories(P, Q);

        STEP("Link folders to category.");
        folders(C).linkToCategory(categories.get(Q));

        // when
        STEP("Verify that searching by PARENT and category will find one descendant node: folderC.");
        SearchRequest query = req("PARENT:" + categories.get(Q).getId());
        searchQueryService.expectResultsFromQuery(query, testUser, folders(C).getName());

        // then
        STEP("Delete categoryM.");
        categories.delete(P);

        STEP("Verify that searching by PARENT and deleted category will find no descendant nodes.");
        searchQueryService.expectResultsFromQuery(query, testUser);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSearchByPath()
    {
        // given
        String Kname = categories.get(K).getName();
        String Lname = categories.get(L).getName();
        String Aname = folders(A).getName();

        // then
        STEP("Verify that searching by PATH and category will find: folderA");
        SearchRequest query = req(format("PATH:'/cm:categoryRoot/cm:generalclassifiable/cm:%s/cm:%s/cm:%s'", Kname, Lname, Aname));
        searchQueryService.expectResultsFromQuery(query, testUser, folders(A).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void ensureCategoriesAreNotTransitive_lookingForExactChildren()
    {
        // given
        String Kname = categories.get(K).getName();
        String Lname = categories.get(L).getName();
        String Aname = folders(A).getName();
        String Bname = folders(B).getName();

        // then
        STEP("Verify that searching by PATH for nested folder will return no results (Dependency to category is not transitive)");
        SearchRequest query = req(format("PATH:'/cm:categoryRoot/cm:generalclassifiable/cm:%s/cm:%s/cm:%s/cm:%s'", Kname, Lname, Aname, Bname));
        searchQueryService.expectResultsFromQuery(query, testUser);
    }

    @Test(groups = TestGroup.SEARCH)
    public void ensureCategoriesAreNotTransitive_lookingForAllChildren()
    {
        // given
        String Kname = categories.get(K).getName();
        String Lname = categories.get(L).getName();

        // then
        STEP("Verify that searching by PATH and category will find: folderA");
        SearchRequest query = req(format("PATH:'/cm:categoryRoot/cm:generalclassifiable/cm:%s/cm:%s//*'", Kname, Lname));
        searchQueryService.expectResultsFromQuery(query, testUser, folders(A).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void ensureCategoriesAreNotTransitive_lookingForAllChildren_byParentCategory()
    {
        // given
        String Kname = categories.get(K).getName();

        // then
        STEP("Verify that searching by PATH and category will find: folderA");
        SearchRequest query = req(format("PATH:'/cm:categoryRoot/cm:generalclassifiable/cm:%s//*'", Kname));
        searchQueryService.expectResultsFromQuery(query, testUser, categories.get(L).getName(), folders(A).getName());
    }
}
