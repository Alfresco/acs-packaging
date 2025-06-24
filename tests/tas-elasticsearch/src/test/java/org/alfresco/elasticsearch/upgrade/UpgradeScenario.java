package org.alfresco.elasticsearch.upgrade;

import static org.alfresco.elasticsearch.upgrade.Utils.createNetwork;
import static org.alfresco.elasticsearch.upgrade.Utils.createTempContentStoreDirectory;

import java.nio.file.Path;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

class UpgradeScenario implements AutoCloseable
{
    private final GenericContainer<?> solr6;
    private final ACSEnv initialEnv;
    private final Elasticsearch elasticsearch;
    private final ACSEnv mirroredEnv;

    public UpgradeScenario(Config cfg)
    {
        // We need to keep these networks in stable lexicographical order. By default, UUIDs are used and in wrong order
        // attaching a running container to second network clears exposed ports.
        final Network initialEnvNetwork = createNetwork("B");
        final Network mirroredEnvNetwork = createNetwork("A");

        final Path sharedContentStorePath = createTempContentStoreDirectory();

        solr6 = new GenericContainer<>(cfg.getSearchServiceImageName())
                .withEnv("SOLR_ALFRESCO_HOST", "alfresco")
                .withEnv("SOLR_ALFRESCO_PORT", "8080")
                .withEnv("SOLR_SOLR_HOST", "solr6")
                .withEnv("SOLR_SOLR_PORT", "8983")
                .withEnv("SOLR_CREATE_ALFRESCO_DEFAULTS", "alfresco,archive")
                .withEnv("ALFRESCO_SECURE_COMMS", "secret")
                .withEnv("JAVA_TOOL_OPTIONS", "-Dalfresco.secureComms.secret=secret")
                .withNetwork(initialEnvNetwork)
                .withNetworkAliases("solr6");
        initialEnv = new ACSEnv(cfg, initialEnvNetwork, "solr6");
        initialEnv.setContentStoreHostPath(sharedContentStorePath);

        elasticsearch = new Elasticsearch(cfg, mirroredEnvNetwork, initialEnvNetwork);

        mirroredEnv = new ACSEnv(cfg, mirroredEnvNetwork, "elasticsearch");
        mirroredEnv.setReadOnlyContentStoreHostPath(sharedContentStorePath);
    }

    public ACSEnv startInitialEnvWithSolrBasedSearchService()
    {
        initialEnv.start();
        try
        {
            Thread.sleep(5000);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        solr6.start();

        return initialEnv;
    }

    public void shutdownSolr()
    {
        solr6.stop();
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
        return mirroredEnv;
    }

    @Override
    public void close()
    {
        initialEnv.close();
        solr6.close();
        elasticsearch.close();
        mirroredEnv.close();
    }
}
