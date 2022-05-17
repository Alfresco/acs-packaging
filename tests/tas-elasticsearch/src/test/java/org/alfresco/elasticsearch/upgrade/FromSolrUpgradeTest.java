package org.alfresco.elasticsearch.upgrade;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ConnectToNetworkCmd;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.Gson;

import org.alfresco.elasticsearch.upgrade.RepoWithSolrSearchEngine.RepoHttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.builder.Transferable;
import org.testng.Assert;
import org.testng.annotations.Test;

public class FromSolrUpgradeTest
{
    @Test
    public void testIt() throws IOException
    {
        try (final UpgradeScenario scenario = new UpgradeScenario())
        {
            final ACSEnv initialEnv = scenario.startInitialEnvWithSolrBasedSearchService();
            System.out.println(initialEnv.uploadFile(getClass().getResource("test.pdf"), "test1.pdf"));
            initialEnv.expectSearchResult(Duration.ofMinutes(1), "babekyrtso", Set.of("test1.pdf"));

//            final Elasticsearch elasticsearch = scenario.startElasticsearch();
//            Assert.assertFalse(elasticsearch.isIndexCreated());
//
//            ACSEnv mirroredEnv = scenario.startMirroredEnvWitElasticsearchBasedSearchService();
//            mirroredEnv.expectSearchResult(Duration.ofMinutes(1), "babekyrtso", Set.of());
//
//            Assert.assertTrue(elasticsearch.isIndexCreated());
//            Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), 0);
//            mirroredEnv.expectSearchResult(Duration.ofMinutes(1), "babekyrtso", Set.of());
        }

//        try (RepoWithSolrSearchEngine initialEnv = RepoWithSolrSearchEngine.createRunning())
//        {
//            initialEnv.expectSearchResult(Duration.ofSeconds(5), "alabama", Set.of());
//
//            initialEnv.uploadFile(getClass().getResource("test.pdf"), "test1.pdf");
//            initialEnv.expectSearchResult(Duration.ofMinutes(1), "babekyrtso", Set.of("test1.pdf"));
//
//            initialEnv.uploadFile(getClass().getResource("test.pdf"), "test2.pdf");
//            initialEnv.expectSearchResult(Duration.ofMinutes(1), "babekyrtso", Set.of("test1.pdf", "test2.pdf"));
//
//            try (MirroredRepoWithElasticsearch mirroredEnv = initialEnv.mirrorCompletely())
//            {
//
//            }
//            String dump = initialEnv.dumpMetadataDb();
//            System.out.println(dump);
//        }
    }
}

class UpgradeScenario implements AutoCloseable
{
    private final Network initialEnvNetwork = Network.newNetwork();
    private final GenericContainer solr6;
    private final ACSEnv initialEnv;

    private final Path sharedContentStorePath;
    private final Elasticsearch elasticsearch;

    private final Network mirroredEnvNetwork = Network.newNetwork();
    private final ACSEnv mirroredEnv;

    public UpgradeScenario()
    {
        try
        {
            FileAttribute<?>[] folderAttributes = new FileAttribute[]{};
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))
            {
                Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxrwxrwx");
                folderAttributes = new FileAttribute[]{ PosixFilePermissions.asFileAttribute(permissions) };
            }
            sharedContentStorePath = Files.createTempDirectory(Path.of(System.getProperty("user.home")), "alf_data", folderAttributes);
            System.err.println("CREATED!!!! " + sharedContentStorePath + " / " + Arrays.toString(folderAttributes));
        } catch (IOException e)
        {
            throw new RuntimeException("Unexpected.", e);
        }

        solr6 = new GenericContainer("alfresco/alfresco-search-services:2.0.3")
                .withEnv("SOLR_ALFRESCO_HOST", "alfresco")
                .withEnv("SOLR_ALFRESCO_PORT", "8080")
                .withEnv("SOLR_SOLR_HOST", "solr6")
                .withEnv("SOLR_SOLR_PORT", "8983")
                .withEnv("SOLR_CREATE_ALFRESCO_DEFAULTS", "alfresco,archive")
                .withEnv("ALFRESCO_SECURE_COMMS", "secret")
                .withEnv("JAVA_TOOL_OPTIONS", "-Dalfresco.secureComms.secret=secret")
                .withNetwork(initialEnvNetwork)
                .withNetworkAliases("solr6");
        initialEnv = new ACSEnv(initialEnvNetwork, "solr6");
        initialEnv.setContentStoreHostPath(sharedContentStorePath);

        elasticsearch = new Elasticsearch(mirroredEnvNetwork, initialEnvNetwork);

        mirroredEnv = new ACSEnv(mirroredEnvNetwork, "elasticsearch");
        mirroredEnv.setReadOnlyContentStoreHostPath(sharedContentStorePath);
    }

    public ACSEnv startInitialEnvWithSolrBasedSearchService()
    {
        solr6.start();
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

class Elasticsearch implements AutoCloseable
{
    private static final int ES_API_TIMEOUT_MS = 5_000;

    private final GenericContainer elasticsearch;
    private final Collection<String> networksToConnectTo;
    private final Gson gson = new Gson();

    public Elasticsearch(Network network, Network... networks)
    {
        networksToConnectTo = Stream.of(networks).map(Network::getId).collect(Collectors.toUnmodifiableSet());

        elasticsearch = new GenericContainer("elasticsearch:7.10.1")
                .withEnv("xpack.security.enabled", "false")
                .withEnv("discovery.type", "single-node")
                .withNetworkAliases("elasticsearch")
                .withNetwork(network)
                .withExposedPorts(9200);
    }

    public int getIndexedDocumentCount() throws IOException
    {
        final Map<String, ?> countResponse = gson.fromJson(getString("/alfresco/_count"), Map.class);
        return Optional
                .ofNullable(countResponse.get("count"))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::intValue)
                .orElseThrow();
    }

    public boolean isIndexCreated() throws IOException
    {
        try
        {
            return getString("/alfresco/_mapping").contains("cm%3Acontent");
        } catch (FileNotFoundException e)
        {
            return false;
        }
    }

    private String getString(String path) throws IOException
    {
        final URL url;
        try
        {
            final URI uri = URI
                    .create("http://" + elasticsearch.getHost() + ":" + elasticsearch.getMappedPort(9200))
                    .resolve(path);
            url = uri.toURL();
        } catch (MalformedURLException e)
        {
            throw new IllegalArgumentException("Failed to create a valid url.", e);
        }

        final HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(ES_API_TIMEOUT_MS);
        c.setReadTimeout(ES_API_TIMEOUT_MS);
        c.setDoOutput(true);

        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream())))
        {
            return r.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    public void start()
    {
        if (elasticsearch.isRunning())
        {
            throw new IllegalStateException("Already started");
        }

        elasticsearch.start();

        for (int i = 0; i < 30; i++)
        {
            try
            {
                isIndexCreated();
                return;
            } catch (IOException io)
            {
                Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            }
        }

        connectElasticSearchToAllNetworks();
    }

    private void connectElasticSearchToAllNetworks()
    {
        final DockerClient client = elasticsearch.getDockerClient();
        final String containerId = elasticsearch.getContainerId();

        networksToConnectTo
                .stream()
                .map(client.connectToNetworkCmd()
                           .withContainerId(containerId)
                           .withContainerNetwork(new ContainerNetwork()
                                   .withAliases("elasticsearch"))::withNetworkId)
                .forEach(ConnectToNetworkCmd::exec);
    }

    @Override
    public void close()
    {
        elasticsearch.close();
    }
}

class ACSEnv implements AutoCloseable
{
    private final GenericContainer alfresco;
    private final GenericContainer postgres;
    private final GenericContainer transformRouter;
    private final GenericContainer transformCoreAllInOne;
    private final GenericContainer sharedFileStore;
    private final GenericContainer activemq;

    private RepoHttpClient repoHttpClient;

    private String metadataDumpToRestore;
    private Path alfDataHostPath;
    private boolean readOnlyContentStore;

    public ACSEnv(final Network network, final String indexSubsystemName)
    {
        postgres = new PostgreSQLContainer("postgres:13.3")
                .withPassword("alfresco")
                .withUsername("alfresco")
                .withDatabaseName("alfresco")
                .withNetwork(network)
                .withNetworkAliases("postgres");

        activemq = new GenericContainer("alfresco/alfresco-activemq:5.16.4-jre11-centos7")
                .withNetwork(network)
                .withNetworkAliases("activemq");

        sharedFileStore = new GenericContainer("quay.io/alfresco/alfresco-shared-file-store:0.16.1")
                .withEnv("JAVA_OPTS", " -Xmx384m -XshowSettings:vm")
                .withEnv("scheduler.content.age.millis", "86400000")
                .withEnv("scheduler.cleanup.interval", "86400000")
                .withNetwork(network)
                .withNetworkAliases("shared-file-store");

        transformCoreAllInOne = new GenericContainer("alfresco/alfresco-transform-core-aio:2.5.7")
                .withEnv("JAVA_OPTS", " -Xmx1024m -XshowSettings:vm")
                .withEnv("ACTIVEMQ_URL", "nio://activemq:61616")
                .withEnv("FILE_STORE_URL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file")
                .withNetwork(network)
                .withNetworkAliases("transform-core-aio");

        transformRouter = new GenericContainer<>("quay.io/alfresco/alfresco-transform-router:1.5.2")
                .withEnv("JAVA_OPTS", " -Xmx384m -XshowSettings:vm")
                .withEnv("ACTIVEMQ_URL", "nio://activemq:61616")
                .withEnv("CORE_AIO_URL", "http://transform-core-aio:8090")
                .withEnv("FILE_STORE_URL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file")
                .withNetwork(network)
                .withNetworkAliases("transform-router");

        alfresco = new GenericContainer("quay.io/alfresco/alfresco-content-repository:7.2.0")
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
                                "-Delasticsearch.host=elasticsearch " +
                                "-Delasticsearch.indexName=alfresco " +
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
                .withLogConsumer(f -> {
                    OutputFrame of = (OutputFrame) f;
                    System.out.print("[alfresco] " + of.getUtf8String());
                })
                .withExposedPorts(8080);
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
            try
            {
                System.err.println("USING!!! " + hostPath + " in " + bindMode + " mode. Exists: " + new File(hostPath).exists() + ". Permissions: " + Files.getPosixFilePermissions(alfDataHostPath));
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            alfresco.addFileSystemBind(hostPath, "/usr/local/tomcat/alf_data", bindMode);
        }

        allContainers().forEach(GenericContainer::start);
        repoHttpClient = new RepoHttpClient(URI.create("http://" + alfresco.getHost() + ":" + alfresco.getMappedPort(8080)));

        expectSearchResult(Duration.ofMinutes(5), UUID.randomUUID().toString(), Set.of());
    }

    public String getMetadataDump()
    {
        return execInPostgres("pg_dump -c -U alfresco alfresco").getStdout();
    }

    private ExecResult execInPostgres(String command)
    {
        final ExecResult result;
        try
        {
            result = postgres.execInContainer("sh", "-c", command);
        } catch (IOException e)
        {
            throw new IllegalStateException("Failed to execute command `" + command + "`.", e);
        } catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Command execution has been interrupted..", e);
        }
        if (result.getExitCode() != 0)
        {
            throw new IllegalStateException("Failed to execute command `" + command + "`. " + result.getStderr());
        }

        return result;
    }

    @Override
    public void close()
    {
        allContainers().forEach(GenericContainer::stop);
    }

    public RepoHttpClient getRepoHttpClient()
    {
        if (repoHttpClient == null)
        {
            throw new IllegalStateException("Client not available. Environment not started.");
        }
        return repoHttpClient;
    }

    public void expectSearchResult(Duration timeout, String term, Set<String> expected)
    {
        final Instant start = Instant.now();
        String lastResult = "unknown";
        while (Instant.now().isBefore(start.plus(timeout)))
        {
            try
            {
                System.out.println("Expected: " + expected);
                Optional<Set<String>> actual = repoHttpClient.searchForFiles(term);
                System.out.println("Actual: " + actual);
                if (actual.map(expected::equals).orElse(false)) return;
                lastResult = actual.map(Object::toString).orElse("unknown");
                Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
            } catch (IOException e)
            {
                lastResult = e.getClass().getSimpleName() + ": " + e.getMessage();
                System.err.println("Exception: " + lastResult);
                Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            }
        }
        throw new IllegalStateException("Couldn't reach the point where `" + expected + "` was returned. Last seen result: `" + lastResult + "`");
    }

    public UUID uploadFile(URL contentUrl, String fileName) throws IOException
    {
        return repoHttpClient.uploadFile(contentUrl, fileName);
    }

    private Stream<GenericContainer> allContainers()
    {
        return Stream.of(postgres, activemq, sharedFileStore, transformCoreAllInOne, transformRouter, alfresco);
    }
}

class RepoWithSolrSearchEngine implements Closeable
{
    private final Network network;

    private final GenericContainer alfresco;
    private final GenericContainer postgres;
    private final GenericContainer solr6;
    private final GenericContainer transformRouter;
    private final GenericContainer transformCoreAllInOne;
    private final GenericContainer sharedFileStore;
    private final GenericContainer activemq;

    private RepoHttpClient repoHttpClient;

    public static RepoWithSolrSearchEngine createRunning()
    {
        RepoWithSolrSearchEngine env = new RepoWithSolrSearchEngine();

        env.start();

        return env;
    }

    private RepoWithSolrSearchEngine()
    {
        network = Network.newNetwork();

        postgres = new PostgreSQLContainer("postgres:13.3")
                .withPassword("alfresco")
                .withUsername("alfresco")
                .withDatabaseName("alfresco")
                .withNetwork(network)
                .withNetworkAliases("postgres");

        activemq = new GenericContainer("alfresco/alfresco-activemq:5.16.4-jre11-centos7")
                .withNetwork(network)
                .withNetworkAliases("activemq");

        solr6 = new GenericContainer("alfresco/alfresco-search-services:2.0.3")
                .withEnv("SOLR_ALFRESCO_HOST", "alfresco")
                .withEnv("SOLR_ALFRESCO_PORT", "8080")
                .withEnv("SOLR_SOLR_HOST", "solr6")
                .withEnv("SOLR_SOLR_PORT", "8983")
                .withEnv("SOLR_CREATE_ALFRESCO_DEFAULTS", "alfresco,archive")
                .withEnv("ALFRESCO_SECURE_COMMS", "secret")
                .withEnv("JAVA_TOOL_OPTIONS", "-Dalfresco.secureComms.secret=secret")
                .withNetwork(network)
                .withNetworkAliases("solr6");

        sharedFileStore = new GenericContainer("quay.io/alfresco/alfresco-shared-file-store:0.16.1")
                .withEnv("JAVA_OPTS", " -Xmx384m -XshowSettings:vm")
                .withEnv("scheduler.content.age.millis", "86400000")
                .withEnv("scheduler.cleanup.interval", "86400000")
                .withNetwork(network)
                .withNetworkAliases("shared-file-store");

        transformCoreAllInOne = new GenericContainer("alfresco/alfresco-transform-core-aio:2.5.7")
                .withEnv("JAVA_OPTS", " -Xmx1024m -XshowSettings:vm")
                .withEnv("ACTIVEMQ_URL", "nio://activemq:61616")
                .withEnv("FILE_STORE_URL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file")
                .withNetwork(network)
                .withNetworkAliases("transform-core-aio");

        transformRouter = new GenericContainer<>("quay.io/alfresco/alfresco-transform-router:1.5.2")
                .withEnv("JAVA_OPTS", " -Xmx384m -XshowSettings:vm")
                .withEnv("ACTIVEMQ_URL", "nio://activemq:61616")
                .withEnv("CORE_AIO_URL", "http://transform-core-aio:8090")
                .withEnv("FILE_STORE_URL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file")
                .withNetwork(network)
                .withNetworkAliases("transform-router");

        //alfresco = new GenericContainer("quay.io/alfresco/alfresco-content-repository:latest")
        alfresco = new GenericContainer("quay.io/alfresco/alfresco-content-repository:7.2.0")
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
                                "-Dindex.subsystem.name=solr6 " +
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
                .withAccessToHost(true).withExposedPorts(8080);
    }

    public void expectSearchResult(Duration timeout, String term, Set<String> expected)
    {
        final Instant start = Instant.now();
        while (Instant.now().isBefore(start.plus(timeout)))
        {
            try
            {
                Optional<Set<String>> actual = repoHttpClient.searchForFiles(term);
                if (actual.map(expected::equals).orElse(false)) return;
                Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
            } catch (IOException e)
            {
                Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            }
        }
        Assert.fail("Couldn't reach the point where `" + expected + "` was received.");
    }

    @Override
    public void close()
    {
        allContainers().forEach(GenericContainer::stop);
    }

    private Stream<GenericContainer> allContainers()
    {
        return Stream.of(solr6, postgres, activemq, sharedFileStore, transformCoreAllInOne, transformRouter, alfresco);
    }

    private void start()
    {
        allContainers().forEach(GenericContainer::start);

        repoHttpClient = new RepoHttpClient(URI.create("http://" + alfresco.getHost() + ":" + alfresco.getMappedPort(8080)));

        expectSearchResult(Duration.ofMinutes(3), "babekyrtso", Set.of());
    }

    public void uploadFile(URL contentUrl, String fileName) throws IOException, InterruptedException
    {
        repoHttpClient.uploadFile(contentUrl, fileName);
    }

    public String dumpMetadataDb() throws IOException, InterruptedException
    {
        final ExecResult dumpCreationResult = postgres
                .execInContainer("sh", "-c", "mkdir -p /opt/alfresco; pg_dump -c -U alfresco alfresco");
        if (dumpCreationResult.getExitCode() != 0)
        {
            throw new IllegalStateException("Creating dump failed. " + dumpCreationResult.getStderr());
        }

        final Path dumpFile = Files.createTempFile("pg-dump-", ".sql");
        Files.writeString(dumpFile, dumpCreationResult.getStdout());

        final GenericContainer pg2 = (GenericContainer) new PostgreSQLContainer("postgres:13.3")
                .withPassword("alfresco")
                .withUsername("alfresco")
                .withDatabaseName("alfresco")
                .withNetwork(Network.newNetwork())
                .withNetworkAliases("postgres")
                .withFileSystemBind(dumpFile.toString(), "/opt/alfresco/pg-dump-alfresco.sql");

        pg2.start();

        final ExecResult dumpRestorationResult = pg2
                .execInContainer("sh", "-c", "cat /opt/alfresco/pg-dump-alfresco.sql | psql -U alfresco");
        if (dumpRestorationResult.getExitCode() != 0)
        {
            throw new IllegalStateException("Restoring dump failed. " + dumpRestorationResult.getStderr());
        }

        return dumpRestorationResult.getStdout();
    }

    public static class RepoHttpClient
    {
        private static final int HTTP_TIMEOUT_MS = 5_000;

        final CloseableHttpClient client = HttpClientBuilder.create()
                                                            .setDefaultRequestConfig(
                                                                    RequestConfig.copy(RequestConfig.DEFAULT)
                                                                                 .setConnectionRequestTimeout(HTTP_TIMEOUT_MS)
                                                                                 .setSocketTimeout(HTTP_TIMEOUT_MS)
                                                                                 .setConnectionRequestTimeout(HTTP_TIMEOUT_MS)
                                                                                 .setRedirectsEnabled(false)
                                                                                 .build())
                                                            .build();

        final Gson gson = new Gson();
        private final URI searchApiUri;
        private final URI fileUploadApiUri;

        RepoHttpClient(final URI repoBaseUri)
        {
            searchApiUri = repoBaseUri.resolve("/alfresco/api/-default-/public/search/versions/1/search");
            fileUploadApiUri = repoBaseUri.resolve("/alfresco/api/-default-/public/alfresco/versions/1/nodes/-my-/children");
        }

        public UUID uploadFile(URL contentUrl, String fileName) throws IOException
        {
            try (InputStream is = contentUrl.openStream())
            {
                final HttpEntity uploadEntity = MultipartEntityBuilder
                        .create()
                        .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                        .addBinaryBody("filedata", is, ContentType.DEFAULT_BINARY, fileName)
                        .build();

                final HttpPost uploadRequest = new HttpPost(fileUploadApiUri);
                uploadRequest.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=");
                uploadRequest.setEntity(uploadEntity);

                final Optional<Map<String, ?>> uploadResult = getJsonResponse(uploadRequest, HttpStatus.SC_CREATED);

                return uploadResult
                        .map(r -> r.get("entry"))
                        .filter(Map.class::isInstance).map(Map.class::cast)
                        .map(e -> e.get("id"))
                        .filter(String.class::isInstance).map(String.class::cast)
                        .map(UUID::fromString)
                        .orElseThrow();
            }
        }

        public Optional<Set<String>> searchForFiles(String term) throws IOException
        {
            final HttpPost searchRequest = new HttpPost(searchApiUri);
            searchRequest.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=");
            searchRequest.setEntity(new StringEntity(searchQuery(term), ContentType.APPLICATION_JSON));

            final Optional<Map<String, ?>> searchResult = getJsonResponse(searchRequest, HttpStatus.SC_OK);
            System.err.println("JsonResponse: " + searchResult);

            final Optional<Collection<?>> possibleEntries = searchResult
                    .map(r -> r.get("list"))
                    .filter(Map.class::isInstance).map(Map.class::cast)
                    .map(m -> m.get("entries"))
                    .filter(Collection.class::isInstance).map(Collection.class::cast);

            if (possibleEntries.isEmpty())
            {
                return Optional.empty();
            }

            final Collection<?> entries = possibleEntries.get();
            final Set<String> names = entries
                    .stream()
                    .filter(Map.class::isInstance).map(Map.class::cast)
                    .map(m -> m.get("entry"))
                    .filter(Map.class::isInstance).map(Map.class::cast)
                    .map(m -> m.get("name"))
                    .filter(String.class::isInstance).map(String.class::cast)
                    .collect(Collectors.toUnmodifiableSet());

            return Optional.of(names);
        }

        private Optional<Map<String, ?>> getJsonResponse(HttpUriRequest request, int requiredStatusCode) throws IOException
        {
            System.err.println("Request: " + request);
            try (CloseableHttpResponse response = client.execute(request))
            {
                System.err.println("Status: " + response.getStatusLine());
                if (response.getStatusLine().getStatusCode() != requiredStatusCode)
                {
                    return Optional.empty();
                }

                System.err.println("ContentType: " + ContentType.parse(response.getEntity().getContentType().getValue()));

                final ContentType contentType = ContentType.parse(response.getEntity().getContentType().getValue());
                if (!ContentType.APPLICATION_JSON.getMimeType().equals(contentType.getMimeType()))
                {
                    return Optional.empty();
                }

                String entity = EntityUtils.toString(response.getEntity());
                System.err.println("Entity: " + entity);
                Map parsed = gson.fromJson(entity, Map.class);
                System.err.println("Parsed: " + parsed);
                return Optional.of(parsed);
            }
        }

        private String searchQuery(String term)
        {
            return "{\"query\":{\"language\":\"afts\",\"query\":\"" + term + "\"}}";
        }
    }
}
