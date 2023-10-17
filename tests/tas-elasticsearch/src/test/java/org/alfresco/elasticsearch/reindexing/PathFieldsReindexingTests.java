package org.alfresco.elasticsearch.reindexing;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.utility.report.log.Step.STEP;

import org.alfresco.rest.search.SearchRequest;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.model.TestGroup;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert"}) // these are TAS E2E tests and use searchQueryService.expectResultsFromQuery for assertion
public class PathFieldsReindexingTests extends NodesSecondaryChildrenRelatedTests
{
    /**
     * Creates a user and a private site containing below hierarchy of folders.
     * <pre>
     * Site
     * DL (Document Library)
     *  += fA += fB += fC (folderC)
     *         /     / |
     *        +     +  +
     *  += fK += fL += fM
     *     |     +
     *     +     |
     *  += fX += fY += fZ
     * </pre>
     * Parent += Child - primary parent-child relationship
     * Parent +- Child - secondary parent-child relationship
     */
    @BeforeClass(alwaysRun = true)
    @Override
    public void dataPreparation()
    {
        super.dataPreparation();

        // given
        STEP("Create some nested folders.");
        folders().createNestedFolders(A, B, C);
        folders().createNestedFolders(K, L, M);
        folders().createNestedFolders(X, Y, Z);

        STEP("Add some extra secondary parent relationships.");
        folders(K).addSecondaryChild(folders(B));
        folders(X).addSecondaryChild(folders(K));
        folders(L).addSecondaryChildren(folders(C), folders(Y));
        folders(M).addSecondaryChild(folders(C));

        STEP("Reindex everything before starting tests.");
        AlfrescoStackInitializer.reindexEverything();
    }

    @Test(groups = TestGroup.SEARCH)
    public void testPrimaryParentField()
    {
        STEP("Check that only M has L as a primary parent.");
        SearchRequest query = req("PRIMARYPARENT:" + folders(L).getNodeRef());
        searchQueryService.expectResultsFromQuery(query, testUser,
                folders(M).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testParentFieldIncludesSecondaryParents()
    {
        STEP("Check that three nodes have L as a primary or secondary parent.");
        SearchRequest query = req("PARENT:" + folders(L).getNodeRef());
        searchQueryService.expectResultsFromQuery(query, testUser,
                folders(C).getName(), folders(M).getName(), folders(Y).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testAncestorFieldIncludesSecondaryAssociations()
    {
        STEP("Check which nodes have K as an ancestor.");
        SearchRequest query = req("ANCESTOR:" + folders(K).getNodeRef());
        searchQueryService.expectResultsFromQuery(query, testUser,
                folders(B).getName(), folders(C).getName(), folders(L).getName(), folders(M).getName(),
                folders(Y).getName(), folders(Z).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryPathWithNodeHavingOneSecondaryChild()
    {
        // then
        STEP("Verify that folderC can be found by secondary PATH using secondary parent folderM.");
        SearchRequest query = req("PATH:\"//cm:" + folders(M).getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(query, testUser,
                // secondary path
                folders(C).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryPathWithNodeHavingOnePrimaryAndTwoSecondaryChildren()
    {
        // then
        STEP("Verify that primary and secondary children of folderL can be found using PATH index.");
        SearchRequest query = req("PATH:\"//cm:" + folders(L).getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(query, testUser,
                // primary path
                folders(M).getName(),
                // secondary path
                folders(C).getName(),
                folders(Y).getName(),
                folders(Z).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testQueryThroughSecondaryPath()
    {
        // then
        STEP("Verify we can get direct children of a secondary paths (X-K=L) referenced in a query.");
        SearchRequest query = req("PATH:\"//cm:" + folders(X).getName() + "//cm:" + folders(L).getName() + "/*\"");
        searchQueryService.expectResultsFromQuery(query, testUser,
                // primary path from L
                folders(M).getName(),
                // secondary path from L
                folders(C).getName(),
                folders(Y).getName()
                );
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryPathWithNodeHavingComplexSecondaryRelationship()
    {
        // then
        STEP("Verify we can find all secondary descendents (excluding direct children) of folderX.");
        SearchRequest query = req("PATH:\"//cm:" + folders(X).getName() + "//*//*\"");
        searchQueryService.expectResultsFromQuery(query, testUser,
                // primary path
                folders(Z).getName(),
                // secondary path
                folders(Y).getName(), // Y _is_ a direct primary child, but this query should only find it via the secondary path.
                folders(B).getName(),
                folders(C).getName(),
                folders(L).getName(),
                folders(M).getName());
    }
}
