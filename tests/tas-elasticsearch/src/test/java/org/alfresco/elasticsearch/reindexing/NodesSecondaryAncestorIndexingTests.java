package org.alfresco.elasticsearch.reindexing;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.utility.report.log.Step.STEP;

import org.alfresco.rest.search.SearchRequest;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.TestGroup;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests verifying live indexing of secondary children and ANCESTOR index in Elasticsearch.
 */
public class NodesSecondaryAncestorIndexingTests extends NodesSecondaryChildrenRelatedTests
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
    public void testSecondaryAncestor_withNodeHavingOneSecondaryChild()
    {
        // then
        STEP("Verify that searching by ANCESTOR and folderM will find one descendant node: folderC.");
        SearchRequest query = req("ANCESTOR:" + folders(M).getNodeRef());
        searchQueryService.expectResultsFromQuery(query, testUser,
            folders(C).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryAncestor_withNodeHavingTwoSecondaryChildren()
    {
        // then
        STEP("Verify that searching by ANCESTOR and folderC will find nodes: .");
        SearchRequest queryAncestorC = req("ANCESTOR:" + folders(L).getNodeRef());
        searchQueryService.expectResultsFromQuery(queryAncestorC, testUser,
            // primary descendant
            folders(M).getName(),
            // secondary descendants
            folders(C).getName(),
            folders(Y).getName(),
            folders(Z).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryAncestor_withDocumentAsSecondaryChild()
    {
        // then
        STEP("Verify that searching by ANCESTOR and folderA will find nodes: .");
        SearchRequest query = req("ANCESTOR:" + folders(A).getNodeRef());
        searchQueryService.expectResultsFromQuery(query, testUser,
            // primary descendants
            folders(B).getName(),
            folders(C).getName(),
            // secondary descendant
            fileInP.getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryAncestor_withNodeHavingComplexSecondaryRelationship()
    {
        // then
        STEP("Verify that all descendant of folderX can be found.");
        SearchRequest query = req("ANCESTOR:" + folders(X).getNodeRef());
        searchQueryService.expectResultsFromQuery(query, testUser,
            // primary descendants
            folders(Y).getName(),
            folders(Z).getName(),
            // secondary descendants
            folders(B).getName(),
            folders(C).getName(),
            folders(K).getName(),
            folders(L).getName(),
            folders(M).getName()
        );
    }

    /**
     * Verify that removing secondary parent-child relationship will result in updating ES index: ANCESTOR.
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
    public void testSecondaryAncestor_withDeletedSecondaryRelationship()
    {
        // given
        STEP("Add to folderQ a secondary child folderR and verify if it can be found using ANCESTOR index and secondary child node reference.");
        folders(Q).addSecondaryChild(folders(R));

        STEP("Verify that searching by ANCESTOR and folderQ will find secondary descendant node: folderR.");
        SearchRequest query = req("ANCESTOR:" + folders(Q).getNodeRef());
        searchQueryService.expectResultsFromQuery(query, testUser,
            // secondary descendant
            folders(R).getName());

        // when
        STEP("Delete the secondary parent-child relationship between folderQ and FolderR.");
        folders(Q).removeSecondaryChild(folders(R));

        // then
        STEP("Verify that folderQ cannot be found by ANCESTOR and folderQ anymore.");
        searchQueryService.expectNoResultsFromQuery(query, testUser);
    }

    /**
     * Verify that removing a node D (fD) having a secondary children relationship will remove the relationships and update ANCESTOR index in ES.
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
    public void testSecondaryAncestor_withDeletedSecondaryParentNode()
    {
        // given
        STEP("Create two nested folders (D and E) in Document Library.");
        Folder folderD = folders().createFolder( "D");
        Folder folderE = folderD.createNestedFolder( "E");
        STEP("Make folderD a secondary children of folderQ and folderR a secondary children of folderD.");
        folders(Q).addSecondaryChild(folderD);
        folderD.addSecondaryChild(folders(R));

        STEP("Verify that searching by ANCESTOR and folderQ will find it's secondary descendant: folderD, folderE and folderR.");
        SearchRequest queryAncestorQ = req("ANCESTOR:" + folders(Q).getNodeRef());
        searchQueryService.expectResultsFromQuery(queryAncestorQ, testUser,
            // secondary descendants
            folderD.getName(),
            folderE.getName(),
            folders(R).getName());

        // when
        STEP("Delete folderD with it's content.");
        folders().delete(folderD);

        // then
        STEP("Verify that searching by ANCESTOR and folderQ will not find any nodes.");
        searchQueryService.expectNoResultsFromQuery(queryAncestorQ, testUser);
    }

    /**
     * Verify that moving folderD (fD) containing secondary children from hierarchy:
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
     * will update ANCESTOR index in ES.
     */
    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryAncestor_withMovedSecondaryParentNode()
    {
        // given
        STEP("Create folderD inside folderQ, and add folderP to D as a secondary child.");
        Folder folderD = folders(Q).createNestedFolder( "D");
        folderD.addSecondaryChild(folders(P));

        STEP("Verify that searching by ANCESTOR and folderQ will find it's primary and secondary descendant nodes: folderD, folderP and file.");
        SearchRequest queryAncestorQ = req("ANCESTOR:" + folders(Q).getNodeRef());
        searchQueryService.expectResultsFromQuery(queryAncestorQ, testUser,
            // primary descendant
            folderD.getName(),
            // secondary descendants
            folders(P).getName(),
            fileInP.getName());
        STEP("Verify that searching by ANCESTOR and folderR will not find any descendant nodes.");
        SearchRequest queryAncestorR = req("ANCESTOR:" + folders(R).getNodeRef());
        searchQueryService.expectNoResultsFromQuery(queryAncestorR, testUser);

        // when
        STEP("Move folderD from folderQ to folderR.");
        folderD.moveTo(folders(R));

        // then
        STEP("Verify that search result for ANCESTOR and folderQ will not find any descendant anymore.");
        searchQueryService.expectNoResultsFromQuery(queryAncestorQ, testUser);
        STEP("Verify that searching by ANCESTOR and folderR will find it's primary and secondary descendant nodes: folderD, folderP and file.");
        searchQueryService.expectResultsFromQuery(queryAncestorR, testUser,
            // primary descendant
            folderD.getName(),
            // secondary descendants
            folders(P).getName(),
            fileInP.getName());

        STEP("Clean-up - delete folderD.");
        folders().delete(folderD);
    }

    /**
     * Verify that copying folder will also result in copying folder's secondary children and update ANCESTOR index in ES.
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
    public void testSecondaryAncestor_withCopiedSecondaryParentNode()
    {
        // given
        STEP("Create nested folders (G and H) inside folderS and folderT in Document Library. Make folderP a secondary child of folderG.");
        Folder folderG = folders(S).createNestedFolder( "G");
        Folder folderH = folderG.createNestedFolder("H");
        Folder folderT = folders().createFolder("T");
        folderG.addSecondaryChild(folders(P));

        STEP("Verify that searching by ANCESTOR and folderS will find it's descendant nodes: folderG, folderH, folderP and file in P.");
        SearchRequest queryAncestorS = req("ANCESTOR:" + folders(S).getNodeRef());
        searchQueryService.expectResultsFromQuery(queryAncestorS, testUser,
            // primary descendants
            folderG.getName(),
            folderH.getName(),
            // secondary descendants
            folders(P).getName(),
            fileInP.getName());
        STEP("Verify that searching by ANCESTOR and folderT will not find any nodes.");
        SearchRequest queryAncestorT = req("ANCESTOR:" + folderT.getNodeRef());
        searchQueryService.expectNoResultsFromQuery(queryAncestorT, testUser);

        // when
        STEP("Copy folderG with it's content to folderT.");
        Folder folderGCopy = folderG.copyTo(folderT);

        // then
        STEP("Verify that searching by ANCESTOR and folderS will find it's descendant nodes: folderG, folderH, folderP and file in P.");
        searchQueryService.expectResultsFromQuery(queryAncestorS, testUser,
            // primary descendants
            folderG.getName(),
            folderH.getName(),
            // secondary descendants
            folders(P).getName(),
            fileInP.getName());
        STEP("Verify that searching by ANCESTOR and folderT will find it's descendant nodes: folderG-copy, folderH-copy, folderP, file.");
        searchQueryService.expectResultsFromQuery(queryAncestorT, testUser,
            // primary descendants
            folderGCopy.getName(),
            folderH.getName(), // the same name as folderH-copy
            // secondary descendants
            folders(P).getName(),
            fileInP.getName());

        STEP("Clean-up - delete folderG and folderT (with G's copy).");
        folders().delete(folderG);
        folders().delete(folderT);
    }
}
