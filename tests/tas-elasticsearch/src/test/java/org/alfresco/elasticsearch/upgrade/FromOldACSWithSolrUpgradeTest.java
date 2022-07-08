package org.alfresco.elasticsearch.upgrade;

import static java.time.Duration.ofMinutes;

import static org.alfresco.elasticsearch.AlfrescoStackInitializer.getImagesConfig;
import static org.alfresco.elasticsearch.upgrade.Utils.waitFor;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alfresco.elasticsearch.EnvHelper;
import org.junit.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testng.Assert;

public class FromOldACSWithSolrUpgradeTest
{
    private static final URL TEST_FILE_URL = FromSolrUpgradeTest.class.getResource("babekyrtso.pdf");
    private static final String SEARCH_TERM = "babekyrtso";

    @Test
    public void testIt() throws InterruptedException, IOException
    {
        final Path sharedContentStorePath = createSharedContentStoreDirectory();

        Network initialEnvNetwork = Network
                .builder()
                .createNetworkCmdModifier(cmd -> cmd.withAttachable(true).withName("B" + UUID.randomUUID()))
                .build();
        final Network mirroredEnvNetwork = Network
                .builder()
                .createNetworkCmdModifier(cmd -> cmd.withAttachable(true).withName("A" + UUID.randomUUID()))
                .build();

        GenericContainer postgres = new GenericContainer<>("postgres:9.4")
                .withEnv("POSTGRES_PASSWORD", "alfresco")
                .withEnv("POSTGRES_USER", "alfresco")
                .withEnv("POSTGRES_DB", "alfresco")
                .withCommand("postgres -c max_connections=300 -c log_min_messages=LOG")
                .withNetwork(initialEnvNetwork)
                .withNetworkAliases("postgres");
        postgres.start();

        GenericContainer libreoffice = new GenericContainer("xcgd/libreoffice")
                .withNetwork(initialEnvNetwork)
                .withNetworkAliases("libreoffice");
        libreoffice.start();

        GenericContainer alfresco = new GenericContainer<>("quay.io/alfresco/alfresco-content-repository-52:5.2.6")
                .withExposedPorts(8080)
                .withEnv("JAVA_TOOL_OPTIONS", "-Dsolr.host=solr6 -Dsolr.port=8983 -Dsolr.secureComms=none -Dsolr.base.url=/solr -Dindex.subsystem.name=solr6")
                .withNetwork(initialEnvNetwork)
                .withNetworkAliases("alfresco")
                .withLogConsumer(of -> System.err.print("[alfresco] " + of.getUtf8String()))
                .withFileSystemBind(sharedContentStorePath.toAbsolutePath().toString(), "/usr/local/alf_data", BindMode.READ_WRITE);
        alfresco.start();

        GenericContainer solr6 = new GenericContainer<>("alfresco/alfresco-search-services:1.3.0.6")
                .withEnv("SOLR_ALFRESCO_HOST", "alfresco")
                .withEnv("SOLR_ALFRESCO_PORT", "8080")
                .withEnv("ALFRESCO_SECURE_COMMS", "none")
                .withEnv("SOLR_SOLR_HOST", "solr6")
                .withEnv("SOLR_SOLR_PORT", "8983")
                .withEnv("SOLR_CREATE_ALFRESCO_DEFAULTS", "alfresco,archive")
                .withEnv("SOLR_JAVA_MEM", "-Xms1g -Xmx1g")
                .withNetwork(initialEnvNetwork)
                .withNetworkAliases("solr6")
                .withLogConsumer(of -> System.err.print("[solr6] " + of.getUtf8String()));
        solr6.start();

        GenericContainer share = new GenericContainer("quay.io/alfresco/alfresco-share-52:5.2.6")
                .withEnv("MEM_LIMIT", "1200m")
                .withNetwork(initialEnvNetwork)
                .withNetworkAliases("share")
                .withExposedPorts(8080);
        share.start();

        RepoHttpClient repoHttpClient = new RepoHttpClient(URI.create("http://" + alfresco.getHost() + ":" + alfresco.getMappedPort(8080)));
        expectNoSearchResult(repoHttpClient, ofMinutes(5), UUID.randomUUID().toString());
        System.out.println("It works");

        uploadFile(repoHttpClient, TEST_FILE_URL, "after-startup.pdf");
        expectSearchResult(repoHttpClient, ofMinutes(1), SEARCH_TERM, "after-startup.pdf");

        String dump = getMetadataDump(postgres);
        System.out.println(dump);

        Elasticsearch elasticsearch = new Elasticsearch(getUpgradeScenarioConfig(), mirroredEnvNetwork, initialEnvNetwork);

        ACSEnv mirroredEnv = new ACSEnv(getUpgradeScenarioConfig(), mirroredEnvNetwork, "elasticsearch");
        mirroredEnv.setContentStoreHostPath(sharedContentStorePath);
        mirroredEnv.setMetadataDumpToRestore(dump);


        elasticsearch.start();
        Assert.assertFalse(elasticsearch.isIndexCreated());

        mirroredEnv.start();

        Thread.sleep(1000 * 60 * 3);

        mirroredEnv.uploadLicense("src/test/resources/alf73-allenabled.lic");

        mirroredEnv.expectNoSearchResult(ofMinutes(5), UUID.randomUUID().toString());

        mirroredEnv.expectNoSearchResult(ofMinutes(1), SEARCH_TERM);

        Assert.assertTrue(elasticsearch.isIndexCreated());
        Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), 0);
        mirroredEnv.expectNoSearchResult(ofMinutes(1), SEARCH_TERM);

        mirroredEnv.startLiveIndexing();

        final long initialReIndexingUpperBound = mirroredEnv.getMaxNodeDbId();
        mirroredEnv.reindexByIds(0, initialReIndexingUpperBound);

        Assert.assertTrue(elasticsearch.getIndexedDocumentCount() > 0);
        mirroredEnv.expectSearchResult(ofMinutes(1), SEARCH_TERM, "after-startup.pdf");



        Thread.sleep(10000);
    }

    private static Path createSharedContentStoreDirectory()
    {
        try
        {
            final Path tempDir = Files.createTempDirectory("alf_data");
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))
            {
                Files.setPosixFilePermissions(tempDir, PosixFilePermissions.fromString("rwxrwxrwx"));
            }
            return tempDir;
        } catch (IOException e)
        {
            throw new RuntimeException("Couldn't create aa temp directory.", e);
        }
    }

    public String getMetadataDump(GenericContainer postgres)
    {
        return execInPostgres(postgres, "pg_dump -c -U alfresco alfresco").getStdout();
    }

    private ExecResult execInPostgres(GenericContainer postgres, String command)
    {
        final ExecResult result;
        try
        {
            result = postgres.execInContainer("sh", "-c", command);
        } catch (IOException e)
        {
            throw new RuntimeException("Failed to execute command `" + command + "`.", e);
        } catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Command execution has been interrupted..", e);
        }
        if (result.getExitCode() != 0)
        {
            throw new RuntimeException("Failed to execute command `" + command + "`. " + result.getStderr());
        }

        return result;
    }

    public UUID uploadFile(RepoHttpClient repoHttpClient, URL contentUrl, String fileName) throws IOException
    {
        return repoHttpClient.uploadFile(contentUrl, fileName);
    }

    public void expectNoSearchResult(RepoHttpClient repoHttpClient, Duration timeout, String term)
    {
        expectSearchResult(repoHttpClient, timeout, term);
    }

    public void expectSearchResult(RepoHttpClient repoHttpClient, Duration timeout, String term, String... expectedFiles)
    {
        final Set<String> expected = Stream
                .of(expectedFiles)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());

        waitFor("Reaching the point where `" + expected + "` is returned.", timeout, () -> {
            try
            {
                Optional<Set<String>> actual = repoHttpClient.searchForFiles(term);
                return actual.map(expected::equals).orElse(false);
            } catch (IOException e)
            {
                return false;
            }
        });
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
