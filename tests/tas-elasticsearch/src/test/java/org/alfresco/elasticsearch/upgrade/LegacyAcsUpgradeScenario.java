package org.alfresco.elasticsearch.upgrade;

import static org.alfresco.elasticsearch.upgrade.Utils.createNetwork;
import static org.alfresco.elasticsearch.upgrade.Utils.createTempContentStoreDirectory;

import java.nio.file.Path;

import org.testcontainers.containers.Network;

class LegacyAcsUpgradeScenario implements AutoCloseable
{
    private final LegacyACSEnv initialEnv;
    private final Elasticsearch elasticsearch;
    private final ACSEnv mirroredEnv;
    private final Path sharedContentStorePath;

    LegacyAcsUpgradeScenario(Config cfg)
    {
        final Network initialEnvNetwork = createNetwork("B");
        final Network mirroredEnvNetwork = createNetwork("A");

        sharedContentStorePath = createTempContentStoreDirectory();

        initialEnv = new ACSEnv52(cfg, initialEnvNetwork);
        initialEnv.setContentStoreHostPath(sharedContentStorePath);

        elasticsearch = new Elasticsearch(cfg, mirroredEnvNetwork, initialEnvNetwork);

        mirroredEnv = new ACSEnv(cfg, mirroredEnvNetwork, "elasticsearch");
        mirroredEnv.setContentStoreHostPath(sharedContentStorePath);
    }

    public LegacyACSEnv startLegacyEnv()
    {
        initialEnv.start();
        return initialEnv;
    }

    public Elasticsearch startElasticsearch()
    {
        elasticsearch.start();
        return elasticsearch;
    }

    public ACSEnv startMirroredEnvWitElasticsearchBasedSearchService()
    {
        mirroredEnv.setMetadataDumpToRestore(initialEnv.getMetadataDump());
        mirroredEnv.start();

        uploadLicence(mirroredEnv);

        return mirroredEnv;
    }

    @Override
    public void close()
    {
        initialEnv.close();
        elasticsearch.close();
        mirroredEnv.close();
    }

    public ACSEnv upgradeLegacyEnvironmentToCurrent()
    {
        final ACSEnv upgradedEnv = initialEnv.upgradeToCurrent();
        upgradedEnv.start();

        uploadLicence(upgradedEnv);

        return upgradedEnv;
    }

    private void uploadLicence(ACSEnv env)
    {
        final String licencePath = getTargetAcsLicencePath();
        if (!env.uploadLicence(licencePath))
        {
            throw new RuntimeException("Failed to upload licence from `" + licencePath + "`.");
        }
    }

    private String getTargetAcsLicencePath()
    {
        return System.getenv("ALF_LICENCE_LOCAL_PATH");
    }
}
