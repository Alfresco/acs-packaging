package org.alfresco.elasticsearch.upgrade;

import static java.time.Duration.ofMinutes;

import static org.alfresco.elasticsearch.upgrade.Utils.waitFor;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alfresco.elasticsearch.upgrade.AvailabilityProbe.ProbeResult;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.Transferable;

class ACSEnv implements AutoCloseable
{
    private final Config cfg;
    private final GenericContainer<?> alfresco;
    private final GenericContainer<?> postgres;
    private final List<GenericContainer<?>> createdContainers = new ArrayList<>();

    private RepoHttpClient repoHttpClient;

    private String metadataDumpToRestore;
    private Path alfDataHostPath;
    private boolean readOnlyContentStore;

    public ACSEnv(Config cfg, final Network network, final String indexSubsystemName)
    {
        this.cfg = cfg;

        postgres = createPostgresContainer(network);

        createActiveMqContainer(network);
        createSharedFileStoreContainer(network);
        createTransformCoreAIOContainer(network);
        createTransformRouterContainer(network);

        alfresco = createRepositoryContainer(network, indexSubsystemName);
    }

    public void setMetadataDumpToRestore(String metadataDumpToRestore)
    {
        this.metadataDumpToRestore = Objects.requireNonNull(metadataDumpToRestore);
    }

    public void setContentStoreHostPath(Path hostPath)
    {
        readOnlyContentStore = false;
        alfDataHostPath = hostPath;
    }

    public void setReadOnlyContentStoreHostPath(Path hostPath)
    {
        readOnlyContentStore = true;
        alfDataHostPath = hostPath;
    }

    public AvailabilityProbe startSearchAPIAvailabilityProbe()
    {
        return AvailabilityProbe.createRunning(10, this::checkSearchAPIAvailability);
    }

    private ProbeResult checkSearchAPIAvailability()
    {
        try
        {
            return repoHttpClient.searchForFiles("babekyrtso").map(v -> ProbeResult.ok()).orElseGet(ProbeResult::fail);
        } catch (Exception e)
        {
            return ProbeResult.fail(e);
        }
    }

    public void start()
    {
        if (alfresco.isRunning())
        {
            throw new IllegalStateException("Already started");
        }

        if (metadataDumpToRestore != null)
        {
            postgres.start();
            postgres.copyFileToContainer(Transferable.of(metadataDumpToRestore), "/opt/alfresco/pg-dump-alfresco.sql");
            execInPostgres("cat /opt/alfresco/pg-dump-alfresco.sql | psql -U alfresco");
        }

        if (alfDataHostPath != null)
        {
            final String hostPath = alfDataHostPath.toAbsolutePath().toString();
            final BindMode bindMode = readOnlyContentStore ? BindMode.READ_ONLY : BindMode.READ_WRITE;
            alfresco.addFileSystemBind(hostPath, "/usr/local/tomcat/alf_data", bindMode);
        }

        createdContainers.forEach(GenericContainer::start);
        repoHttpClient = new RepoHttpClient(URI.create("http://" + alfresco.getHost() + ":" + alfresco.getMappedPort(8080)));

        expectNoSearchResult(ofMinutes(5), UUID.randomUUID().toString());
    }

    public String getMetadataDump()
    {
        return execInPostgres("pg_dump -c -U alfresco alfresco").getStdout();
    }

    public long getMaxNodeDbId()
    {
        return Long.parseLong(execInPostgres("psql -U alfresco -t -c 'SELECT max(id) FROM alf_node'").getStdout().strip());
    }

    private ExecResult execInPostgres(String command)
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

    @Override
    public void close()
    {
        createdContainers.forEach(GenericContainer::stop);
    }

    public void expectNoSearchResult(Duration timeout, String term)
    {
        expectSearchResult(timeout, term);
    }

    public void expectSearchResult(Duration timeout, String term, String... expectedFiles)
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

    public UUID uploadFile(URL contentUrl, String fileName) throws IOException
    {
        return repoHttpClient.uploadFile(contentUrl, fileName);
    }

    public void setElasticsearchSearchService() throws IOException
    {
        repoHttpClient.setSearchService("elasticsearch");
    }

    public void reindexByIds(long fromId, long toId)
    {
        final GenericContainer<?> reIndexing = createReIndexingContainer(fromId, toId);

        reIndexing.start();
        waitFor("Re-indexing Startup", ofMinutes(1), reIndexing::isRunning);
        waitFor("Re-indexing Exit", ofMinutes(5), () -> !reIndexing.isRunning());
    }

    public void startLiveIndexing()
    {
        final GenericContainer<?> liveIndexing = createLiveIndexingContainer();
        liveIndexing.start();
    }

    private GenericContainer<?> createRepositoryContainer(Network network, String indexSubsystemName)
    {
        return newContainer(GenericContainer.class, cfg.getRepositoryImage())
                .withEnv("JAVA_TOOL_OPTIONS",
                        "-Dencryption.keystore.type=JCEKS " +
                                "-Dencryption.cipherAlgorithm=DESede/CBC/PKCS5Padding " +
                                "-Dencryption.keyAlgorithm=DESede " +
                                "-Dencryption.keystore.location=/usr/local/tomcat/shared/classes/alfresco/extension/keystore/keystore " +
                                "-Dmetadata-keystore.password=mp6yc0UD9e -Dmetadata-keystore.aliases=metadata " +
                                "-Dmetadata-keystore.metadata.password=oKIWzVdEdA -Dmetadata-keystore.metadata.algorithm=DESede")
                .withEnv("JAVA_OPTS",
                        "-Ddb.driver=org.postgresql.Driver " +
                                "-Ddb.username=alfresco " +
                                "-Ddb.password=alfresco " +
                                "-Ddb.url=jdbc:postgresql://postgres:5432/alfresco " +
                                "-Dsolr.host=solr6 " +
                                "-Dsolr.port=8983 " +
                                "-Dsolr.secureComms=secret " +
                                "-Dsolr.sharedSecret=secret " +
                                "-Dsolr.base.url=/solr " +
                                "-Delasticsearch.createIndexIfNotExists=true " +
                                "-Delasticsearch.host=" + cfg.getElasticsearchHostname() + " " +
                                "-Delasticsearch.indexName=" + cfg.getIndexName() + " " +
                                "-Dindex.subsystem.name=" + indexSubsystemName + " " +
                                "-Dalfresco.host=localhost " +
                                "-Dalfresco.port=8080 " +
                                "-Dmessaging.broker.url=\"failover:(nio://activemq:61616)?timeout=3000&jms.useCompression=true\" " +
                                "-Ddeployment.method=DOCKER_COMPOSE " +
                                "-Dtransform.service.enabled=true " +
                                "-Dtransform.service.url=http://transform-router:8095 " +
                                "-Dsfs.url=http://shared-file-store:8099/ " +
                                "-DlocalTransform.core-aio.url=http://transform-core-aio:8090/ " +
                                "-Xmx768m -XshowSettings:vm")
                .withNetwork(network)
                .withNetworkAliases("alfresco")
                .withExposedPorts(8080);
    }

    private void createTransformRouterContainer(Network network)
    {
        newContainer(GenericContainer.class, cfg.getTransformRouterImage())
                .withEnv("JAVA_OPTS", " -Xmx384m -XshowSettings:vm")
                .withEnv("ACTIVEMQ_URL", "nio://activemq:61616")
                .withEnv("CORE_AIO_URL", "http://transform-core-aio:8090")
                .withEnv("FILE_STORE_URL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file")
                .withNetwork(network)
                .withNetworkAliases("transform-router");
    }

    private void createTransformCoreAIOContainer(Network network)
    {
        newContainer(GenericContainer.class, cfg.getTransformCoreAIOImage())
                .withEnv("JAVA_OPTS", " -Xmx1024m -XshowSettings:vm")
                .withEnv("ACTIVEMQ_URL", "nio://activemq:61616")
                .withEnv("FILE_STORE_URL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file")
                .withNetwork(network)
                .withNetworkAliases("transform-core-aio");
    }

    private void createSharedFileStoreContainer(Network network)
    {
        newContainer(GenericContainer.class, cfg.getSharedFileStoreImage())
                .withEnv("JAVA_OPTS", " -Xmx384m -XshowSettings:vm")
                .withEnv("scheduler.content.age.millis", "86400000")
                .withEnv("scheduler.cleanup.interval", "86400000")
                .withNetwork(network)
                .withNetworkAliases("shared-file-store");
    }

    private void createActiveMqContainer(Network network)
    {
        newContainer(GenericContainer.class, cfg.getActiveMqImage())
                .withNetwork(network)
                .withNetworkAliases("activemq");
    }

    private GenericContainer<?> createPostgresContainer(Network network)
    {
        return newContainer(PostgreSQLContainer.class, cfg.getPostgreSQLImage())
                .withPassword("alfresco")
                .withUsername("alfresco")
                .withDatabaseName("alfresco")
                .withNetwork(network)
                .withNetworkAliases("postgres");
    }

    private GenericContainer<?> createReIndexingContainer(long fromId, long toId)
    {
        return newContainer(GenericContainer.class, cfg.getReIndexingImage())
                .withEnv("ELASTICSEARCH_INDEXNAME", cfg.getIndexName())
                .withEnv("SPRING_ELASTICSEARCH_REST_URIS", "http://" + cfg.getElasticsearchHostname() + ":9200")
                .withEnv("SPRING_ACTIVEMQ_BROKERURL", "nio://activemq:61616")
                .withEnv("ALFRESCO_SHAREDFILESTORE_BASEURL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file/")
                .withEnv("ALFRESCO_ACCEPTEDCONTENTMEDIATYPESCACHE_BASEURL", "http://transform-core-aio:8090/transform/config")
                .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/alfresco")
                .withEnv("ALFRESCO_REINDEX_PATHINDEXINGENABLED", "true")
                .withEnv("ALFRESCO_REINDEX_FROM_ID", Long.toString(fromId))
                .withEnv("ALFRESCO_REINDEX_TO_ID", Long.toString(toId))
                .withEnv("ALFRESCO_REINDEX_JOB_NAME", "reindexByIds")
                .withNetwork(alfresco.getNetwork());
    }

    private GenericContainer<?> createLiveIndexingContainer()
    {
        return newContainer(GenericContainer.class, cfg.getLiveIndexingImage())
                .withEnv("ELASTICSEARCH_INDEXNAME", cfg.getIndexName())
                .withEnv("SPRING_ELASTICSEARCH_REST_URIS", "http://" + cfg.getElasticsearchHostname() + ":9200")
                .withEnv("SPRING_ACTIVEMQ_BROKERURL", "nio://activemq:61616")
                .withEnv("ALFRESCO_SHAREDFILESTORE_BASEURL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file/")
                .withEnv("ALFRESCO_ACCEPTEDCONTENTMEDIATYPESCACHE_BASEURL", "http://transform-core-aio:8090/transform/config")
                .withNetwork(alfresco.getNetwork());
    }

    private <T extends GenericContainer<?>> T newContainer(Class<T> clazz, String image)
    {
        try
        {
            final T container = clazz.getConstructor(String.class).newInstance(image);
            createdContainers.add(container);
            return container;
        } catch (Exception e)
        {
            throw new RuntimeException("Failed to create a container for `" + image + "`.", e);
        }
    }
}
