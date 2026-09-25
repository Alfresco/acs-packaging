package org.alfresco.elasticsearch.upgrade;

import static java.time.Duration.ofMinutes;

import static org.alfresco.elasticsearch.upgrade.Config.getUpgradeScenarioConfig;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.alfresco.elasticsearch.upgrade.AvailabilityProbe.Stats;

public class FromSolrUpgradeTest
{
    private static final Duration MAX_TIMEOUT = ofMinutes(5);
    private static final URL TEST_FILE_URL = FromSolrUpgradeTest.class.getResource("babekyrtso.pdf");
    private static final String SEARCH_TERM = "babekyrtso";
    private static final String FILE_UPLOADED_BEFORE_INITIAL_REINDEXING = "before-initial-re-indexing.pdf";
    private static final String FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING = "before-live-indexing.pdf";
    private static final String FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING = "after-live-indexing.pdf";
    private static final String FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH = "before-switch.pdf";
    private static final String FILE_UPLOADED_AFTER_SWITCHING_TO_ELASTICSEARCH = "after-switch.pdf";

    // ACS-12862: fixtures for the advanced migration scenarios.
    // Each scenario uses a distinct search term, because expectQueryResult
    // compares the returned file-name set exactly.
    // ------------------------------------------------------------------
    private static final SearchUser ADVANCED_USER = new SearchUser("migrationuser", "password"); // pragma: allowlist secret
    private static final SearchUser OUTSIDER_USER = new SearchUser("migrationoutsider", "password"); // pragma: allowlist secret
    private static final SearchUser GROUP_USER = new SearchUser("migrationgroupuser", "password"); // pragma: allowlist secret
    private static final String MIGRATION_GROUP = "migrationgroup";

    private static final String DEEP_DOC = "deep-doc.txt";
    private static final String L1_CHILD_DOC = "level-one-doc.txt";
    private static final String SHARED_DOC = "shared-doc.txt";
    private static final String MULTILANG_DOC = "multilang-doc.txt";
    private static final String TAGGED_DOC = "tagged-doc.txt";
    private static final String RESTRICTED_DOC = "restricted-doc.txt";
    private static final String CATEGORISED_DOC = "categorised-doc.txt";
    private static final String GROUP_PROTECTED_DOC = "group-protected-doc.txt";

    private static final String DEEP_FOLDER_L1 = "advL1";
    private static final String DEEP_FOLDER_L2 = "advL2";
    private static final String DEEP_FOLDER_L5 = "advL5";
    private static final String PRIMARY_FOLDER = "advPrimary";
    private static final String SECONDARY_FOLDER = "advSecondary";
    private static final String MIGRATION_TAG = "migrationtag";
    private static final String MIGRATION_CATEGORY = "migrationcategory";
    private static final String ACL_FOLDER = "advAclFolder";

    // ACS-12862: scenarios 8-11 - content mutated before the migration, so the re-index has to
    // reflect each node's current state rather than the state it was first created in.
    private static final String MOVE_SOURCE_FOLDER = "advMoveSource";
    private static final String MOVE_TARGET_FOLDER = "advMoveTarget";
    private static final String MOVED_DOC = "moved-doc.txt";
    private static final String UPDATED_DOC = "updated-doc.txt";
    private static final String STALE_TERM = "stalecontentterm";
    private static final String CURRENT_TERM = "currentcontentterm";
    private static final String DELETED_DOC = "deleted-doc.txt";
    private static final String SURVIVING_DOC = "surviving-doc.txt";
    private static final String DELETE_PAIR_TERM = "deletepairterm";

    // ACS-12862: LocalDate.toString() is ISO yyyy-MM-dd, the format AbstractSearchExactTermTest uses.
    private static final String QUERY_FROM_DATE = LocalDate.now().minusDays(1).toString();
    private static final String QUERY_TO_DATE = LocalDate.now().plusDays(1).toString();

    // ACS-12862: set during seeding, read back when the category query is asserted.
    private String migrationCategoryRef;

    @Test
    public void testZeroDowntimeUpgradeFromSolrToElasticsearch() throws IOException, InterruptedException
    {
        try (final UpgradeScenario scenario = new UpgradeScenario(getUpgradeScenarioConfig()))
        {
            final ACSEnv initialEnv = scenario.startInitialEnvWithSolrBasedSearchService();
            initialEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_BEFORE_INITIAL_REINDEXING);
            initialEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM, FILE_UPLOADED_BEFORE_INITIAL_REINDEXING);

            // ACS-12862: seed the advanced content while the repository is still on Solr,
            // so all of it has to survive the migration to Elasticsearch.
            seedAdvancedTestData(initialEnv);

            final AvailabilityProbe probe = initialEnv.getRunningSearchAPIAvailabilityProbe();

            final Elasticsearch elasticsearch = scenario.startElasticsearch();
            Assert.assertFalse(elasticsearch.isIndexCreated());

            final long initialReIndexingUpperBound = initialEnv.getMaxNodeDbId();

            try (ACSEnv mirroredEnv = scenario.startMirroredEnvWitElasticsearchBasedSearchService())
            {
                mirroredEnv.expectNoSearchResult(MAX_TIMEOUT, SEARCH_TERM);
                Assert.assertTrue(mirroredEnv.getMaxNodeDbId() >= initialEnv.getMaxNodeDbId());
                elasticsearch.waitForIndexCreation(MAX_TIMEOUT);
                Assert.assertTrue(elasticsearch.isIndexCreated());
                Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), 0);
                mirroredEnv.expectNoSearchResult(MAX_TIMEOUT, SEARCH_TERM);

                mirroredEnv.startLiveIndexing();
                mirroredEnv.reindexByIds(0, initialReIndexingUpperBound * 2);

                Assert.assertTrue(elasticsearch.getIndexedDocumentCount() > 0);
                mirroredEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM, FILE_UPLOADED_BEFORE_INITIAL_REINDEXING);
            }

            final long documentsCount = elasticsearch.getIndexedDocumentCount();
            initialEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING);

            initialEnv.startLiveIndexing();

            initialEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM,
                    FILE_UPLOADED_BEFORE_INITIAL_REINDEXING,
                    FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING);
            // Live indexing was not running so FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING hasn't been indexed
            Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), documentsCount);

            initialEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING);
            initialEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM,
                    FILE_UPLOADED_BEFORE_INITIAL_REINDEXING,
                    FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING);
            // FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING has been indexed, but we still have a gap.
            // FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING is still not indexed
            Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), documentsCount + 1);

            initialEnv.reindexByIds((long) (initialReIndexingUpperBound * 0.9), 1_000_000_000);
            // Gap has been closed by running reindexing. Both FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING and
            // FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING have been indexed.
            Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), documentsCount + 2);

            initialEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH);
            initialEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM,
                    FILE_UPLOADED_BEFORE_INITIAL_REINDEXING,
                    FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH);
            // Live indexing is still running so FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH should be indexed as well.
            Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), documentsCount + 3);

            initialEnv.setElasticsearchSearchService();

            // Now we use ES. Check if we still have valid result.
            initialEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM,
                    FILE_UPLOADED_BEFORE_INITIAL_REINDEXING,
                    FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH);

            // ACS-12862: the repository is now served by Elasticsearch - run one query per
            // advanced scenario to confirm the seeded content survived the migration intact.
            verifyAdvancedScenarios(initialEnv);

            scenario.shutdownSolr();

            // Solr has been stopped. Check if we still have valid result.
            initialEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM,
                    FILE_UPLOADED_BEFORE_INITIAL_REINDEXING,
                    FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH);

            initialEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_AFTER_SWITCHING_TO_ELASTICSEARCH);

            // Check if FILE_UPLOADED_AFTER_SWITCHING_TO_ELASTICSEARCH is part of the search result.
            initialEnv.expectSearchResult(MAX_TIMEOUT, SEARCH_TERM,
                    FILE_UPLOADED_BEFORE_INITIAL_REINDEXING,
                    FILE_UPLOADED_BEFORE_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_AFTER_STARTING_LIVE_INDEXING,
                    FILE_UPLOADED_BEFORE_SWITCHING_TO_ELASTICSEARCH,
                    FILE_UPLOADED_AFTER_SWITCHING_TO_ELASTICSEARCH);

            final Stats availabilityStats = probe.stop();
            Assert.assertTrue(availabilityStats.getSuccessRatioInPercents() >= 99, "Search was unavailable. Stats: " + availabilityStats);
        }
    }

    /**
     * ACS-12862: creates the content used by the advanced migration scenarios. Called against the Solr-based environment before any re-indexing happens.
     */
    private void seedAdvancedTestData(ACSEnv env) throws IOException
    {
        // Scenarios 1, 6, 7 and 11 - deep folder hierarchy: advL1/advL2/advL3/advL4/advL5/deep-doc.txt
        // DEEP_DOC is reused by the CMIS query and the cm:created range check, so it is seeded once here.
        final UUID l1 = env.createFolder("-my-", DEEP_FOLDER_L1);
        final UUID l2 = env.createFolder(l1.toString(), DEEP_FOLDER_L2);
        final UUID l3 = env.createFolder(l2.toString(), "advL3");
        final UUID l4 = env.createFolder(l3.toString(), "advL4");
        final UUID l5 = env.createFolder(l4.toString(), DEEP_FOLDER_L5);
        env.uploadTextFile(l5.toString(), DEEP_DOC, "deepindexing content at level five");
        env.uploadTextFile(l1.toString(), L1_CHILD_DOC, "deepindexing content at level one");

        // Scenario 2 - secondary parent-child association
        final UUID folderA = env.createFolder("-my-", PRIMARY_FOLDER);
        final UUID folderB = env.createFolder("-my-", SECONDARY_FOLDER);
        final UUID sharedDoc = env.uploadTextFile(folderA.toString(), SHARED_DOC, "secondaryassoc content");
        env.addSecondaryChildAssociation(folderB.toString(), sharedDoc);

        // Scenario 3 - multi-language content
        env.uploadTextFile("-my-", MULTILANG_DOC, "Guten Morgen. Bonjour les croissants. Hola, jugando.");

        // Scenario 4a - tags
        final UUID taggedDoc = env.uploadTextFile("-my-", TAGGED_DOC, "tagged content");
        env.addTag(taggedDoc, MIGRATION_TAG);

        // Scenario 4b - categories. A dedicated category is created under the root category so
        // the fixture is deterministic.
        migrationCategoryRef = "workspace://SpacesStore/" + env.createCategory(MIGRATION_CATEGORY);
        final UUID categorisedDoc = env.uploadTextFile("-my-", CATEGORISED_DOC, "categorised content");
        env.setCategory(categorisedDoc, migrationCategoryRef);

        // Scenario 5a - direct ACL: inheritance broken, readable only by ADVANCED_USER
        env.createUser(ADVANCED_USER.username(), ADVANCED_USER.password());
        env.createUser(OUTSIDER_USER.username(), OUTSIDER_USER.password());
        final UUID restrictedDoc = env.uploadTextFile("-my-", RESTRICTED_DOC, "restricted content");
        env.setExclusivePermission(restrictedDoc, ADVANCED_USER.username(), "Consumer");

        // Scenario 5b - complex ACL: group-based permission on a folder, inherited by its child.
        env.createUser(GROUP_USER.username(), GROUP_USER.password());
        final String group = env.createGroup(MIGRATION_GROUP, "Migration Test Group");
        env.addUserToGroup(group, GROUP_USER.username());
        final UUID aclFolder = env.createFolder("-my-", ACL_FOLDER);
        env.setExclusivePermission(aclFolder, group, "Consumer");
        env.uploadTextFile(aclFolder.toString(), GROUP_PROTECTED_DOC, "group protected content");

        // Scenario 8 - a node moved before the migration; its indexed path must follow the move.
        final UUID moveSource = env.createFolder("-my-", MOVE_SOURCE_FOLDER);
        final UUID moveTarget = env.createFolder("-my-", MOVE_TARGET_FOLDER);
        final UUID movedDoc = env.uploadTextFile(moveSource.toString(), MOVED_DOC, "moved content");
        env.moveNode(movedDoc, moveTarget.toString());

        // Scenario 9 - content replaced before the migration; only the current version is searchable.
        final UUID updatedDoc = env.uploadTextFile("-my-", UPDATED_DOC, STALE_TERM + " content");
        env.updateTextFileContent(updatedDoc, CURRENT_TERM + " content");

        // Scenario 10 - two siblings share a term and one is deleted, so a single exact-set
        // assertion proves the survivor is indexed and the deleted node was not resurrected.
        env.uploadTextFile("-my-", SURVIVING_DOC, DELETE_PAIR_TERM + " content");
        final UUID deletedDoc = env.uploadTextFile("-my-", DELETED_DOC, DELETE_PAIR_TERM + " content");
        env.deleteNode(deletedDoc);
    }

    /**
     * ACS-12862: one query per advanced scenario, executed after the repository has switched to Elasticsearch.
     */
    private void verifyAdvancedScenarios(ACSEnv env)
    {
        // 1) Deep folder hierarchy - a node buried five levels down is indexed with its path
        env.expectQueryResult(MAX_TIMEOUT, "afts",
                "PATH:'//cm:" + DEEP_FOLDER_L5 + "//*' AND TYPE:'cm:content'", DEEP_DOC);

        // 2) Secondary parent-child association - the doc is reachable under its secondary parent
        env.expectQueryResult(MAX_TIMEOUT, "afts",
                "PATH:'//cm:" + SECONDARY_FOLDER + "//*' AND TYPE:'cm:content'", SHARED_DOC);

        // 3) Multi-language content - a non-English term inside the content is searchable.
        env.expectQueryResult(MAX_TIMEOUT, "afts", "cm:content:'croissants'", MULTILANG_DOC);

        // 4) Categories and tags - both associations survived the re-index
        env.expectQueryResult(MAX_TIMEOUT, "afts", "TAG:'" + MIGRATION_TAG + "'", TAGGED_DOC);
        env.expectQueryResult(MAX_TIMEOUT, "afts",
                "cm:categories:\"" + migrationCategoryRef + "\" AND cm:name:'" + CATEGORISED_DOC + "'",
                CATEGORISED_DOC);

        // 5) Complex ACLs and permission-based filtering.
        // 5a - direct grant on the node; positive check first so the ACL is indexed.
        env.expectQueryResultAs(MAX_TIMEOUT, ADVANCED_USER, "afts",
                "cm:name:'" + RESTRICTED_DOC + "'", RESTRICTED_DOC);
        env.expectQueryResultAs(MAX_TIMEOUT, OUTSIDER_USER, "afts",
                "cm:name:'" + RESTRICTED_DOC + "'");
        // 5b - group-based grant on the parent folder, inherited by the child document.
        env.expectQueryResultAs(MAX_TIMEOUT, GROUP_USER, "afts",
                "cm:name:'" + GROUP_PROTECTED_DOC + "'", GROUP_PROTECTED_DOC);
        env.expectQueryResultAs(MAX_TIMEOUT, OUTSIDER_USER, "afts",
                "cm:name:'" + GROUP_PROTECTED_DOC + "'");

        // 6) CMIS query language
        env.expectQueryResult(MAX_TIMEOUT, "cmis",
                "SELECT * FROM cmis:document WHERE cmis:name = '" + DEEP_DOC + "'", DEEP_DOC);

        // 7) Path query variants - '//' matches the whole subtree, '/' only direct children.
        // TYPE:'cm:content' keeps the intermediate folders out of the result set.
        env.expectQueryResult(MAX_TIMEOUT, "afts",
                "PATH:'//cm:" + DEEP_FOLDER_L1 + "//*' AND TYPE:'cm:content'", DEEP_DOC, L1_CHILD_DOC);
        env.expectQueryResult(MAX_TIMEOUT, "afts",
                "PATH:'//cm:" + DEEP_FOLDER_L1 + "/*' AND TYPE:'cm:content'", L1_CHILD_DOC);

        // 8) Move - found under the new parent, and no longer under the old one.
        // The positive check runs first, so the negative cannot pass on a stale index.
        env.expectQueryResult(MAX_TIMEOUT, "afts",
                "PATH:'//cm:" + MOVE_TARGET_FOLDER + "//*' AND TYPE:'cm:content'", MOVED_DOC);
        env.expectQueryResult(MAX_TIMEOUT, "afts",
                "PATH:'//cm:" + MOVE_SOURCE_FOLDER + "//*' AND TYPE:'cm:content'");

        // 9) Content update - the current content matches, the replaced content does not.
        env.expectQueryResult(MAX_TIMEOUT, "afts", "cm:content:'" + CURRENT_TERM + "'", UPDATED_DOC);
        env.expectQueryResult(MAX_TIMEOUT, "afts", "cm:content:'" + STALE_TERM + "'");

        // 10) Delete - exactly the surviving sibling comes back.
        env.expectQueryResult(MAX_TIMEOUT, "afts",
                "cm:content:'" + DELETE_PAIR_TERM + "'", SURVIVING_DOC);

        // 11) Typed field mapping - a date range on cm:created still resolves after the migration.
        env.expectQueryResult(MAX_TIMEOUT, "afts",
                "cm:created:['" + QUERY_FROM_DATE + "' TO '" + QUERY_TO_DATE + "']"
                        + " AND cm:name:'" + DEEP_DOC + "'",
                DEEP_DOC);
    }
}