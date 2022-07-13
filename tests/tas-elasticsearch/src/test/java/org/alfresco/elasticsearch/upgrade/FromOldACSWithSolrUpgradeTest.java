package org.alfresco.elasticsearch.upgrade;

import static java.time.Duration.ofMinutes;

import static org.alfresco.elasticsearch.AlfrescoStackInitializer.getImagesConfig;
import static org.alfresco.elasticsearch.upgrade.Utils.createNetwork;
import static org.alfresco.elasticsearch.upgrade.Utils.createTempContentStoreDirectory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.UUID;

import org.alfresco.elasticsearch.EnvHelper;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;
import org.testcontainers.containers.Network;
import org.testng.Assert;

public class FromOldACSWithSolrUpgradeTest
{
    private static final URL TEST_FILE_URL = FromSolrUpgradeTest.class.getResource("babekyrtso.pdf");
    private static final String SEARCH_TERM = "babekyrtso";
    public static final String FILE_UPLOADED_AFTER_INITIAL_ENVIRONMENT_STARTUP = "after-startup.pdf";
    public static final String FILE_UPLOADED_AFTER_TAKING_DUMP = "after-dump.pdf";
    public static final String FILE_UPLOADED_BEFORE_UPGRADING_INITIAL_ENVIRONMENT = "before-upgrade.pdf";
    public static final String FILE_UPLOADED_AFTER_UPGRADE = "after-upgrade.pdf";

    @Test
    public void testIt() throws IOException
    {
        final Path oldEnvContentStorePath = createTempContentStoreDirectory();
        final Path mirroredEnvContentStorePath = createTempContentStoreDirectory();

        final Network initialEnvNetwork = createNetwork("B");
        final Network mirroredEnvNetwork = createNetwork("A");

        try (final ACSEnv52 acs52 = new ACSEnv52(initialEnvNetwork))
        {
            acs52.setContentStoreHostPath(oldEnvContentStorePath);
            acs52.start();

            acs52.expectNoSearchResult(ofMinutes(5), UUID.randomUUID().toString());
            acs52.uploadFile(TEST_FILE_URL, FILE_UPLOADED_AFTER_INITIAL_ENVIRONMENT_STARTUP);
            acs52.expectSearchResult(ofMinutes(1), SEARCH_TERM, FILE_UPLOADED_AFTER_INITIAL_ENVIRONMENT_STARTUP);

            final long initialReIndexingUpperBound = acs52.getMaxNodeDbId();
            String dump = acs52.getMetadataDump();

            acs52.uploadFile(TEST_FILE_URL, FILE_UPLOADED_AFTER_TAKING_DUMP);
            acs52.expectSearchResult(ofMinutes(1), SEARCH_TERM, FILE_UPLOADED_AFTER_INITIAL_ENVIRONMENT_STARTUP, FILE_UPLOADED_AFTER_TAKING_DUMP);

            final Elasticsearch elasticsearch = new Elasticsearch(getUpgradeScenarioConfig(), mirroredEnvNetwork, initialEnvNetwork);

            try (final ACSEnv mirroredEnv = new ACSEnv(getUpgradeScenarioConfig(), mirroredEnvNetwork, "elasticsearch"))
            {
                mirroredEnv.setContentStoreHostPath(mirroredEnvContentStorePath);
                mirroredEnv.setMetadataDumpToRestore(dump);
                FileSystemUtils.copyRecursively(oldEnvContentStorePath, mirroredEnvContentStorePath);

                elasticsearch.start();
                Assert.assertFalse(elasticsearch.isIndexCreated());

                mirroredEnv.start();

                Assert.assertTrue(mirroredEnv.uploadLicence("/Users/pzurek/Downloads/alf73-allenabled.lic"));

                mirroredEnv.expectNoSearchResult(ofMinutes(1), SEARCH_TERM);

                Assert.assertTrue(elasticsearch.isIndexCreated());
                Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), 0);

                mirroredEnv.startLiveIndexing();
                mirroredEnv.reindexByIds(0, initialReIndexingUpperBound);

                Assert.assertTrue(elasticsearch.getIndexedDocumentCount() > 0);
                mirroredEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM, FILE_UPLOADED_AFTER_INITIAL_ENVIRONMENT_STARTUP);
            }

            acs52.uploadFile(TEST_FILE_URL, FILE_UPLOADED_BEFORE_UPGRADING_INITIAL_ENVIRONMENT);
            acs52.expectSearchResult(ofMinutes(1), SEARCH_TERM,
                    FILE_UPLOADED_AFTER_INITIAL_ENVIRONMENT_STARTUP,
                    FILE_UPLOADED_AFTER_TAKING_DUMP,
                    FILE_UPLOADED_BEFORE_UPGRADING_INITIAL_ENVIRONMENT);

            try (final ACSEnv upgradedEnv = acs52.upgrade(getUpgradeScenarioConfig()))
            {
                upgradedEnv.start();
                Assert.assertTrue(upgradedEnv.uploadLicence("/Users/pzurek/Downloads/alf73-allenabled.lic"));
                upgradedEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM, FILE_UPLOADED_AFTER_INITIAL_ENVIRONMENT_STARTUP);

                upgradedEnv.startLiveIndexing();

                upgradedEnv.reindexByIds(initialReIndexingUpperBound, 1_000_000_000);
                upgradedEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM,
                        FILE_UPLOADED_AFTER_INITIAL_ENVIRONMENT_STARTUP,
                        FILE_UPLOADED_AFTER_TAKING_DUMP,
                        FILE_UPLOADED_BEFORE_UPGRADING_INITIAL_ENVIRONMENT);

                upgradedEnv.uploadFile(TEST_FILE_URL, FILE_UPLOADED_AFTER_UPGRADE);
                upgradedEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM,
                        FILE_UPLOADED_AFTER_INITIAL_ENVIRONMENT_STARTUP,
                        FILE_UPLOADED_AFTER_TAKING_DUMP,
                        FILE_UPLOADED_BEFORE_UPGRADING_INITIAL_ENVIRONMENT,
                        FILE_UPLOADED_AFTER_UPGRADE);
            }
        }
    }

    private Config getUpgradeScenarioConfig()
    {
        return new Config()
        {
            @Override
            public String getRepositoryImage()
            {
                return getImagesConfig().getRepositoryImage();
            }

            @Override
            public String getSearchServiceImageName()
            {
                return "quay.io/alfresco/insight-engine:" + EnvHelper.getEnvProperty("SOLR6_TAG");
            }

            @Override
            public String getPostgreSQLImage()
            {
                return getImagesConfig().getPostgreSQLImage();
            }

            @Override
            public String getActiveMqImage()
            {
                return getImagesConfig().getActiveMqImage();
            }

            @Override
            public String getSharedFileStoreImage()
            {
                return getImagesConfig().getSharedFileStoreImage();
            }

            @Override
            public String getTransformCoreAIOImage()
            {
                return getImagesConfig().getTransformCoreAIOImage();
            }

            @Override
            public String getTransformRouterImage()
            {
                return getImagesConfig().getTransformRouterImage();
            }

            @Override
            public String getReIndexingImage()
            {
                return getImagesConfig().getReIndexingImage();
            }

            @Override
            public String getLiveIndexingImage()
            {
                return getImagesConfig().getLiveIndexingImage();
            }

            @Override
            public String getElasticsearchImage()
            {
                return getImagesConfig().getElasticsearchImage();
            }
        };
    }
}
