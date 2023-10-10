package org.alfresco.elasticsearch.reindexing;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.utility.report.log.Step.STEP;

import org.alfresco.rest.search.SearchRequest;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.TestGroup;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests verifying live indexing of secondary children and PARENT index in Elasticsearch.
 */
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert"}) // these are TAS tests and use searchQueryService.expectResultsFromQuery for assertion
public class NodesSecondaryParentIndexingTests extends NodesSecondaryChildrenRelatedTests
{

    private FileModel fileInP;

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
    public void testSecondaryParentWithNodeHavingOneSecondaryChild()
    {
        // then
        STEP("Verify that searching rby PARENT and folderM will find node folderC.");
        SearchRequest query = req("PARENT:" + folders(M).getNodeRef());
        searchQueryService.expectResultsFromQuery(query, testUser,
            folders(C).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryParentWithNodeHavingTwoSecondaryChildren()
    {
        // then
        STEP("Verify that searching by PARENT and folderL will find nodes: folderM, FolderC and folderY.");
        SearchRequest query = req("PARENT:" + folders(L).getNodeRef());
        searchQueryService.expectResultsFromQuery(query, testUser,
            // primary child
            folders(M).getName(),
            // secondary children
            folders(C).getName(),
            folders(Y).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryParentWithDocumentAsSecondaryChild()
    {
        // then
        STEP("Verify that searching by PARENT and folderA will find nodes: folderB and file.");
        SearchRequest query = req("PARENT:" + folders(A).getNodeRef());
        searchQueryService.expectResultsFromQuery(query, testUser,
            // primary child
            folders(B).getName(),
            // secondary child
            fileInP.getName());
    }

    /**
     * Verify that removing secondary parent-child relationship will result in updating ES index: PARENT.
     * Test changes below folders hierarchy:
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
    public void testSecondaryParentWithDeletedSecondaryRelationship()
    {
        // given
        STEP("Add to folderQ a secondary child folderR and verify if it can be found using PARENT index and secondary parent node reference.");
        folders(Q).addSecondaryChild(folders(R));

        SearchRequest query = req("PARENT:" + folders(Q).getNodeRef());
        searchQueryService.expectResultsFromQuery(query, testUser,
            // secondary child
            folders(R).getName());

        // when
        STEP("Delete the secondary parent relationship between folderQ and FolderR and verify that folderR cannot be found by PARENT and folderQ anymore.");
        folders(Q).removeSecondaryChild(folders(R));

        // then
        searchQueryService.expectNoResultsFromQuery(query, testUser);
    }

    /**
     * Verify that removing a node D (fD) having a secondary children relationship will remove the relationships and update PARENT index in ES.
     * Test changes below folders hierarchy from:
     * <pre>
     * DL
     *  += fQ
     *     +
     *     |
     *  += fE += fF
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
    public void testSecondaryParentWithDeletedSecondaryParentNode()
    {
        // given
        STEP("Create two nested folders (E and F) in Document Library.");
        Folder folderE = folders().createFolder( "E");
        Folder folderF = folderE.createNestedFolder( "F");
        STEP("Make folderE a secondary children of folderQ and folderR a secondary children of folderE.");
        folders(Q).addSecondaryChild(folderE);
        folderE.addSecondaryChild(folders(R));

        STEP("Verify that searching by PARENT and folderQ will find its secondary child: folderE.");
        SearchRequest queryParentQ = req("PARENT:" + folders(Q).getNodeRef());
        searchQueryService.expectResultsFromQuery(queryParentQ, testUser,
            // secondary child
            folderE.getName());
        STEP("Verify that searching by PARENT and folderE will find its primary and secondary children: folderF and folderR.");
        SearchRequest queryParentD = req("PARENT:" + folderE.getNodeRef());
        searchQueryService.expectResultsFromQuery(queryParentD, testUser,
            // primary child
            folderF.getName(),
            // secondary child
            folders(R).getName());

        // when
        STEP("Delete folderE and verify that PARENT was updated for nodes folderQ and folderR.");
        folders().delete(folderE);

        // then
        searchQueryService.expectNoResultsFromQuery(queryParentQ, testUser);
        searchQueryService.expectNoResultsFromQuery(queryParentD, testUser);
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
     * will update PARENT index in ES.
     */
    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryParentWithMovedSecondaryParentNode()
    {
        // given
        STEP("Create folderD inside folderQ, and add folderP to D as a secondary child.");
        Folder folderD = folders(Q).createNestedFolder( "D");
        folderD.addSecondaryChild(folders(P));

        STEP("Verify that searching by PARENT and folderD will find node folderP.");
        SearchRequest queryParentD = req("PARENT:" + folderD.getNodeRef());
        searchQueryService.expectResultsFromQuery(queryParentD, testUser,
            // secondary child
            folders(P).getName());
        STEP("Verify that searching by PARENT and folderQ will find node folderD.");
        SearchRequest queryParentQ = req("PARENT:" + folders(Q).getNodeRef());
        searchQueryService.expectResultsFromQuery(queryParentQ, testUser,
            // primary child
            folderD.getName());

        // when
        STEP("Move folderD from folderQ to folderR.");
        folderD.moveTo(folders(R));

        // then
        STEP("Verify that search result for PARENT and folderD didn't change.");
        searchQueryService.expectResultsFromQuery(queryParentD, testUser,
            // secondary child
            folders(P).getName());
        STEP("Verify that searching by PARENT and folderQ doesn't return any node anymore.");
        searchQueryService.expectNoResultsFromQuery(queryParentQ, testUser);
        STEP("Verify that searching by PARENT and folderR will find node folderD.");
        SearchRequest queryParentR = req("PARENT:" + folders(R).getNodeRef());
        searchQueryService.expectResultsFromQuery(queryParentR, testUser,
            // primary child
            folderD.getName());

        STEP("Clean-up - delete folderD.");
        folders().delete(folderD);
    }

    /**
     * Verify that copying folder will also result in copying folder's secondary children and update PARENT index in ES.
     * Test changes below folders hierarchy:
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
    public void testSecondaryParentWithCopiedSecondaryParentNode()
    {
        // given
        STEP("Create nested folders (G and H) inside folderS and folderT in Document Library. Make folderP a secondary child of folderG.");
        Folder folderG = folders(S).createNestedFolder( "G");
        Folder folderH = folderG.createNestedFolder("H");
        Folder folderT = folders().createFolder("T");
        folderG.addSecondaryChild(folders(P));

        STEP("Verify that searching by PARENT and folderG will find nodes: folderH, folderP and file.");
        SearchRequest queryParentG = req("PARENT:" + folderG.getNodeRef());
        searchQueryService.expectResultsFromQuery(queryParentG, testUser,
            // primary child
            folderH.getName(),
            // secondary child
            folders(P).getName());
        STEP("Verify that searching by PARENT and folderS will find nodes: folderG, folderH, folderP and file.");
        SearchRequest queryParentS = req("PARENT:" + folders(S).getNodeRef());
        searchQueryService.expectResultsFromQuery(queryParentS, testUser,
            // primary child
            folderG.getName());
        STEP("Verify that searching by PARENT and folderP will find node: file.");
        SearchRequest queryParentP = req("PARENT:" + folders(P).getNodeRef());
        searchQueryService.expectResultsFromQuery(queryParentP, testUser,
            // primary child
            fileInP.getName());

        // when
        STEP("Copy folderG with its content to folderT.");
        Folder folderGCopy = folderG.copyTo(folderT);

        // then
        STEP("Verify that search result for PARENT and folderS didn't change.");
        searchQueryService.expectResultsFromQuery(queryParentS, testUser,
            // primary child
            folderG.getName());
        STEP("Verify that searching by PARENT and folderS/folderG will find nodes: folderH, folderP and file in P.");
        searchQueryService.expectResultsFromQuery(queryParentG, testUser,
            // primary child
            folderH.getName(),
            // secondary child
            folders(P).getName());
        STEP("Verify that folderG was copied with secondary parent-child relationship and PARENT reflects that - search by folderT/folderGCopy should find nodes: folderH, folderP and file in P.");
        SearchRequest queryParentGCopy = req("PARENT:" + folderGCopy.getNodeRef());
        searchQueryService.expectResultsFromQuery(queryParentGCopy, testUser,
            // primary child
            folderH.getName(), // name is the same as folderH-copy
            // secondary child
            folders(P).getName());
        STEP("Verify that this time searching by PARENT and folderP will find node: file.");
        searchQueryService.expectResultsFromQuery(queryParentP, testUser,
            // primary child
            fileInP.getName());

        STEP("Clean-up - delete folderG and folderT (with G's copy).");
        folders().delete(folderG);
        folders().delete(folderT);
    }
}
