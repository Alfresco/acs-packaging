package org.alfresco.elasticsearch.reindexing;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.utility.report.log.Step.STEP;

import org.alfresco.rest.search.SearchRequest;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.TestGroup;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests verifying live indexing of secondary children and PATH index in Elasticsearch.
 */
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.JUnit4TestShouldUseTestAnnotation"}) // these are testng tests and use searchQueryService.expectResultsFromQuery for assertion
public class NodesSecondaryPathIndexingTests extends NodesSecondaryChildrenRelatedTests
{

    private FileModel fileInP;

    /**
     * Creates a user and a private site containing bellow hierarchy of folders.
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
     *  += fP += file -+ fA
     *  += fQ
     *  += fR
     *  += fS
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
        STEP("Create few sets of nested folders in site's Document Library.");
        folders().createNestedFolders(A, B, C);
        folders().createNestedFolders(K, L, M);
        folders().createNestedFolders(X, Y, Z);
        folders().createFolder(P);
        folders().createFolder(Q);
        folders().createFolder(R);
        folders().createFolder(S);
        fileInP = folders(P).createRandomDocument();

        STEP("Create few secondary parent-child relationships.");
        folders(K).addSecondaryChild(folders(B));
        folders(X).addSecondaryChild(folders(K));
        folders(L).addSecondaryChildren(folders(C), folders(Y));
        folders(M).addSecondaryChild(folders(C));
        folders(A).addSecondaryChild(fileInP);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryPath_withNodeHavingOneSecondaryChild()
    {
        // then
        STEP("Verify that folderC can be found by secondary PATH using secondary parent folderM.");
        SearchRequest query = req("PATH:\"//cm:" + folders(M).getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(query, testUser,
            // secondary path
            folders(C).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryPath_withNodeHavingOnePrimaryAndTwoSecondaryChildren()
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
            folders(Z).getName()
        );
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryPath_withNodeHavingDocumentAsSecondaryChild()
    {
        // then
        STEP("Verify that a file being a secondary child of folderA can be found using PATH index.");
        SearchRequest query = req("PATH:\"//cm:" + folders(A).getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(query, testUser,
            // primary path
            folders(B).getName(),
            folders(C).getName(),
            // secondary path
            fileInP.getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryPath_withNodeHavingComplexSecondaryRelationship()
    {
        // then
        STEP("Verify that all secondary children of folderX can be found.");
        SearchRequest query = req("PATH:\"//cm:" + folders(X).getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(query, testUser,
            // primary path
            folders(Y).getName(),
            folders(Z).getName(),
            // secondary path
            folders(B).getName(),
            folders(C).getName(),
            folders(K).getName(),
            folders(L).getName(),
            folders(M).getName()
        );
    }

    /**
     * Verify that removing secondary parent-child relationship will result in updating ES index: PATH.
     * Test changes bellow folders hierarchy:
     * <pre>
     * DL
     *  += fQ
     *     +
     *     |
     *  += fR
     * </pre>
     * into:
     * <pre>
     * DL
     *  += fQ
     *  += fR
     * </pre>
     */
    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryPath_withDeletedSecondaryRelationship()
    {
        // given
        STEP("Add to folderQ a secondary child folderR and verify if it can be found using PATH index and secondary parent name.");
        folders(Q).addSecondaryChild(folders(R));

        SearchRequest query = req("PATH:\"//cm:" + folders(Q).getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(query, testUser,
            // secondary path
            folders(R).getName());

        // when
        STEP("Delete the secondary parent relationship between folderQ and FolderR and verify that folderR cannot be found by PATH and folderQ anymore.");
        folders(Q).removeSecondaryChild(folders(R));

        // then
        searchQueryService.expectNoResultsFromQuery(query, testUser);
    }

    /**
     * Verify that removing a node D (fD) having a secondary children relationship will remove the relationships and update PATH index in ES.
     * Test changes bellow folders hierarchy from:
     * <pre>
     * DL
     *  += fQ
     *     +
     *     |
     *  += fD += fE
     *     +
     *     |
     *  += fR
     * </pre>
     * into:
     * <pre>
     * DL
     *  += fQ
     *  += fR
     * </pre>
     */
    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryPath_withDeletedSecondaryParentNode()
    {
        // given
        STEP("Create two nested folders (D and E) in Document Library.");
        Folder folderD = folders().createFolder( "D");
        Folder folderE = folderD.createNestedFolder( "E");
        STEP("Make folderD a secondary children of folderQ and folderR a secondary children of folderD.");
        folders(Q).addSecondaryChild(folderD);
        folderD.addSecondaryChild(folders(R));

        STEP("Verify that searching by PATH and folderQ will find nodes: folderD, folderE and folderR.");
        SearchRequest queryPathQ = req("PATH:\"//cm:" + folders(Q).getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(queryPathQ, testUser,
            // secondary path
            folderD.getName(),
            folderE.getName(),
            folders(R).getName());
        STEP("Verify that searching by PATH and folderD will find it's primary and secondary children: folderE and folderR.");
        SearchRequest queryPathD = req("PATH:\"//cm:" + folderD.getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(queryPathD, testUser,
            // primary path
            folderE.getName(),
            // secondary path
            folders(R).getName());

        // when
        STEP("Delete folderD and verify that PATH was updated for nodes folderQ and folderR.");
        folders().delete(folderD);

        // then
        searchQueryService.expectNoResultsFromQuery(queryPathQ, testUser);
        searchQueryService.expectNoResultsFromQuery(queryPathD, testUser);
    }

    /**
     * Verify that moving folder D (fD) containing secondary children from hierarchy:
     * <pre>
     * DL
     *  += fQ += fD +- fP += file
     *  += fR
     * </pre>
     * to:
     * <pre>
     * DL
     *  += fQ
     *  += fR += fD +- fP += file
     * </pre>
     * will update PATH index in ES.
     */
    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryPath_withMovedSecondaryParentNode()
    {
        // given
        STEP("Create folderD inside folderQ, and add folderP as a secondary child.");
        Folder folderD = folders(Q).createNestedFolder( "D");
        folderD.addSecondaryChild(folders(P));
        folders(M).addSecondaryChild(folderD);

        STEP("Verify that searching by PATH and folderD will find nodes: folderP and file.");
        SearchRequest queryPathD = req("PATH:\"//cm:" + folderD.getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(queryPathD, testUser,
            // secondary path
            folders(P).getName(),
            fileInP.getName());
        STEP("Verify that searching by PATH and folderQ will find nodes: folderD, folderP and file.");
        SearchRequest queryPathQ = req("PATH:\"//cm:" + folders(Q).getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(queryPathQ, testUser,
            // primary path
            folderD.getName(),
            // secondary path
            folders(P).getName(),
            fileInP.getName());

        // when
        STEP("Move folderD from folderQ to folderR.");
        folderD.moveTo(folders(R));

        // then
        STEP("Verify that search result for PATH and folderD didn't change.");
        searchQueryService.expectResultsFromQuery(queryPathD, testUser,
            // secondary path
            folders(P).getName(),
            fileInP.getName());
        STEP("Verify that searching by PATH and folderQ doesn't return any node anymore.");
        searchQueryService.expectNoResultsFromQuery(queryPathQ, testUser);
        STEP("Verify that searching by PATH and folderR will find nodes: folderD, folderP and file.");
        SearchRequest queryPathR = req("PATH:\"//cm:" + folders(R).getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(queryPathR, testUser,
            // primary path
            folderD.getName(),
            // secondary path
            folders(P).getName(),
            fileInP.getName());

        STEP("Clean-up - delete folderD.");
        folders().delete(folderD);
    }

    /**
     * Verify that copying folder will also result in copying folder's secondary children and update PATH index in ES.
     * Test change bellow folders hierarchy:
     * <pre>
     * DL
     *  += fS += fG += fH
     *         +
     *        /
     *  += fP += file
     *  += fT
     * </pre>
     * into:
     * <pre>
     * DL
     *  += fS += fG += fH
     *         +
     *        /
     *  += fP += file
     *        \
     *         +
     *  += fT += fG-c += fH-c
     *  </pre>
     */
    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryParent_withCopiedSecondaryParentNode()
    {
        // given
        STEP("Create nested folders (G and H) inside folderS and folderT in Document Library. Make folderP a secondary child of folderG.");
        Folder folderG = folders(S).createNestedFolder( "G");
        Folder folderH = folderG.createNestedFolder("H");
        Folder folderT = folders().createFolder("T");
        folderG.addSecondaryChild(folders(P));

        STEP("Verify that searching by PATH and folderG will find nodes: folderH, folderP and file.");
        SearchRequest queryPathG = req("PATH:\"//cm:" + folderG.getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(queryPathG, testUser,
            // primary path
            folderH.getName(),
            // secondary path
            folders(P).getName(),
            fileInP.getName());
        STEP("Verify that searching by PATH and folderS will find nodes: folderG, folderH, folderP and file.");
        SearchRequest queryPathS = req("PATH:\"//cm:" + folders(S).getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(queryPathS, testUser,
            // primary path
            folderG.getName(),
            folderH.getName(),
            // secondary path
            folders(P).getName(),
            fileInP.getName());

        // when
        STEP("Copy folderG with it's content to folderT.");
        Folder folderGCopy = folderG.copyTo(folderT);

        // then
        STEP("Verify that search result for PATH and folderS didn't change.");
        searchQueryService.expectResultsFromQuery(queryPathS, testUser,
            // primary path
            folderH.getName(),
            folderG.getName(),
            // secondary path
            folders(P).getName(),
            fileInP.getName());
        STEP("Verify that searching by PATH and folderS/folderG will find nodes: folderH, folderP and file in P.");
        SearchRequest queryPathSG = req("PATH:\"//cm:" + folders(S).getName() + "/cm:" + folderG.getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(queryPathSG, testUser,
            // primary path
            folderH.getName(),
            // secondary path
            folders(P).getName(),
            fileInP.getName());
        STEP("Verify that folderG was copied with secondary parent-child relationship and PATH reflects that - search by folderT/folderGCopy should find nodes: folderH, folderP and file in P.");
        SearchRequest queryPathTGCopy = req("PATH:\"//cm:" + folderT.getName() + "/cm:" + folderGCopy.getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(queryPathTGCopy, testUser,
            // primary path
            folderH.getName(), // the same name as folderH-copy
            // secondary path
            folders(P).getName(),
            fileInP.getName());

        STEP("Clean-up - delete folderG and folderT (with G's copy).");
        folders().delete(folderG);
        folders().delete(folderT);
    }
}
