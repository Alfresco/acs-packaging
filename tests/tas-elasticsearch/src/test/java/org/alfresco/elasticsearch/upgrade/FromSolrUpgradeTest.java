package org.alfresco.elasticsearch.upgrade;

import java.io.BufferedReader;
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
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ConnectToNetworkCmd;
import com.github.dockerjava.api.command.CreateNetworkCmd;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.Gson;

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

            final Elasticsearch elasticsearch = scenario.startElasticsearch();
            Assert.assertFalse(elasticsearch.isIndexCreated());

            final long initialReIndexingUpperBound;

            try (ACSEnv mirroredEnv = scenario.startMirroredEnvWitElasticsearchBasedSearchService())
            {
                mirroredEnv.expectSearchResult(Duration.ofMinutes(1), "babekyrtso", Set.of());
                Assert.assertEquals(initialEnv.getMaxNodeDbId(), mirroredEnv.getMaxNodeDbId());

                Assert.assertTrue(elasticsearch.isIndexCreated());
                Assert.assertEquals(elasticsearch.getIndexedDocumentCount(), 0);
                mirroredEnv.expectSearchResult(Duration.ofMinutes(1), "babekyrtso", Set.of());

                mirroredEnv.startLiveIndexing();
                initialReIndexingUpperBound = mirroredEnv.getMaxNodeDbId();
                mirroredEnv.reindexByIds(0, initialReIndexingUpperBound);
                Assert.assertTrue(elasticsearch.getIndexedDocumentCount() > 0);
                mirroredEnv.expectSearchResult(Duration.ofMinutes(1), "babekyrtso", Set.of("test1.pdf"));
            }

            initialEnv.startLiveIndexing();
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.MINUTES);
            initialEnv.expectSearchResult(Duration.ofMinutes(1), "babekyrtso", Set.of("test1.pdf"));
            final long documentsCount = elasticsearch.getIndexedDocumentCount();
            System.out.println(initialEnv.uploadFile(getClass().getResource("test.pdf"), "test2.pdf"));
            initialEnv.expectSearchResult(Duration.ofMinutes(1), "babekyrtso", Set.of("test1.pdf", "test2.pdf"));
            Assert.assertTrue(elasticsearch.getIndexedDocumentCount() > documentsCount);
        }
    }
}

class UpgradeScenario implements AutoCloseable
{
    private final Network initialEnvNetwork = Network.builder().createNetworkCmdModifier(cmd -> cmd.withAttachable(true).withName("B")).build();
    private final GenericContainer solr6;
    private final ACSEnv initialEnv;

    private final Path sharedContentStorePath;
    private final Elasticsearch elasticsearch;

    private final Network mirroredEnvNetwork = Network.builder().createNetworkCmdModifier(cmd -> cmd.withAttachable(true).withName("A")).build();
    private final ACSEnv mirroredEnv;

    public UpgradeScenario()
    {
        try
        {
            sharedContentStorePath = Files.createTempDirectory("alf_data");
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))
            {
                Files.setPosixFilePermissions(sharedContentStorePath, PosixFilePermissions.fromString("rwxrwxrwx"));
            }
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
    private final Collection<String> additionalNetworks;
    private final Gson gson = new Gson();

    public Elasticsearch(Network network, Network... networks)
    {
        additionalNetworks = Stream.of(networks).map(Network::getId).collect(Collectors.toUnmodifiableSet());

        elasticsearch = new GenericContainer("elasticsearch:7.10.1")
                .withEnv("xpack.security.enabled", "false")
                .withEnv("discovery.type", "single-node")
                .withNetworkAliases("elasticsearch")
                .withNetwork(network)
                .withExposedPorts(9200);
    }

    public long getIndexedDocumentCount() throws IOException
    {
        final Map<String, ?> countResponse = gson.fromJson(getString("/alfresco/_count"), Map.class);
        return Optional
                .ofNullable(countResponse.get("count"))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::longValue)
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

        System.err.println("URL! " + url);
        final HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(ES_API_TIMEOUT_MS);
        c.setReadTimeout(ES_API_TIMEOUT_MS);
        c.setDoOutput(true);

        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream())))
        {
            String response = r.lines().collect(Collectors.joining(System.lineSeparator()));
            System.err.println("Response! " + response);
            return response;
        }
    }

    public void start()
    {
        if (elasticsearch.isRunning())
        {
            throw new IllegalStateException("Already started");
        }

        elasticsearch.start();
        for (int i = 0; i < 3; i++)
        {
            ACSEnv.waitFor("Elasticsearch Startup", Duration.ofMinutes(1), () -> {
                try
                {
                    return !isIndexCreated();
                } catch (IOException e)
                {
                    return false;
                }
            });
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! IS RUNNING");
        }

        connectElasticSearchToAdditionalNetworks();
        System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! NETWORK ATTACHED");

        for (int i = 0; i < 3; i++)
        {
            ACSEnv.waitFor("Elasticsearch Startup", Duration.ofMinutes(1), () -> {
                try
                {
                    return !isIndexCreated();
                } catch (IOException e)
                {
                    return false;
                }
            });
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! IS RUNNING");
        }
    }

    private void connectElasticSearchToAdditionalNetworks()
    {
        final DockerClient client = elasticsearch.getDockerClient();
        final String containerId = elasticsearch.getContainerId();

        System.err.println(client.inspectContainerCmd(containerId).exec().getNetworkSettings());
        additionalNetworks
                .stream()
                .map(client.connectToNetworkCmd()
                           .withContainerId(containerId)
                           .withContainerNetwork(new ContainerNetwork()
                                   .withAliases("elasticsearch"))::withNetworkId)
                .forEach(ConnectToNetworkCmd::exec);

        System.err.println(client.inspectContainerCmd(containerId).exec().getNetworkSettings());
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
    private final List<GenericContainer> createdContainers = new ArrayList<>();

    private RepoHttpClient repoHttpClient;

    private String metadataDumpToRestore;
    private Path alfDataHostPath;
    private boolean readOnlyContentStore;

    public ACSEnv(final Network network, final String indexSubsystemName)
    {
        postgres = newContainer(PostgreSQLContainer.class,"postgres:13.3")
                .withPassword("alfresco")
                .withUsername("alfresco")
                .withDatabaseName("alfresco")
                .withNetwork(network)
                .withNetworkAliases("postgres");

        newContainer(GenericContainer.class, "alfresco/alfresco-activemq:5.16.4-jre11-centos7")
                .withNetwork(network)
                .withNetworkAliases("activemq");

        newContainer(GenericContainer.class, "quay.io/alfresco/alfresco-shared-file-store:0.16.1")
                .withEnv("JAVA_OPTS", " -Xmx384m -XshowSettings:vm")
                .withEnv("scheduler.content.age.millis", "86400000")
                .withEnv("scheduler.cleanup.interval", "86400000")
                .withNetwork(network)
                .withNetworkAliases("shared-file-store");

        newContainer(GenericContainer.class, "alfresco/alfresco-transform-core-aio:2.5.7")
                .withEnv("JAVA_OPTS", " -Xmx1024m -XshowSettings:vm")
                .withEnv("ACTIVEMQ_URL", "nio://activemq:61616")
                .withEnv("FILE_STORE_URL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file")
                .withNetwork(network)
                .withNetworkAliases("transform-core-aio");

        newContainer(GenericContainer.class, "quay.io/alfresco/alfresco-transform-router:1.5.2")
                .withEnv("JAVA_OPTS", " -Xmx384m -XshowSettings:vm")
                .withEnv("ACTIVEMQ_URL", "nio://activemq:61616")
                .withEnv("CORE_AIO_URL", "http://transform-core-aio:8090")
                .withEnv("FILE_STORE_URL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file")
                .withNetwork(network)
                .withNetworkAliases("transform-router");

        alfresco = newContainer(GenericContainer.class, "quay.io/alfresco/alfresco-content-repository:7.2.0")
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
            alfresco.addFileSystemBind(hostPath, "/usr/local/tomcat/alf_data", bindMode);
        }

        createdContainers.forEach(GenericContainer::start);
        repoHttpClient = new RepoHttpClient(URI.create("http://" + alfresco.getHost() + ":" + alfresco.getMappedPort(8080)));

        expectSearchResult(Duration.ofMinutes(5), UUID.randomUUID().toString(), Set.of());
    }

    public String getMetadataDump()
    {
        return execInPostgres("pg_dump -c -U alfresco alfresco").getStdout();
    }

    public long getMaxNodeDbId()
    {
        return Long.valueOf(execInPostgres("psql -U alfresco -t -c 'SELECT max(id) FROM alf_node'").getStdout().strip());
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
        createdContainers.forEach(GenericContainer::stop);
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
                Optional<Set<String>> actual = repoHttpClient.searchForFiles(term);
                if (actual.map(expected::equals).orElse(false)) return;
                lastResult = actual.map(Object::toString).orElse("unknown");
                Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
            } catch (IOException e)
            {
                lastResult = e.getClass().getSimpleName() + ": " + e.getMessage();
                Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            }
        }
        throw new IllegalStateException("Couldn't reach the point where `" + expected + "` was returned. Last seen result: `" + lastResult + "`");
    }

    public UUID uploadFile(URL contentUrl, String fileName) throws IOException
    {
        return repoHttpClient.uploadFile(contentUrl, fileName);
    }

    public void reindexByIds(long fromId, long toId)
    {
        final GenericContainer reIndexing = newContainer(GenericContainer.class, "quay.io/alfresco/alfresco-elasticsearch-reindexing:3.1.1")
                .withEnv("ELASTICSEARCH_INDEXNAME", "alfresco")
                .withEnv("SPRING_ELASTICSEARCH_REST_URIS", "http://elasticsearch:9200")
                .withEnv("SPRING_ACTIVEMQ_BROKERURL", "nio://activemq:61616")
                .withEnv("ALFRESCO_SHAREDFILESTORE_BASEURL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file/")
                .withEnv("ALFRESCO_ACCEPTEDCONTENTMEDIATYPESCACHE_BASEURL", "http://transform-core-aio:8090/transform/config")
                .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/alfresco")
                .withEnv("ALFRESCO_REINDEX_FROM_ID", Long.toString(fromId))
                .withEnv("ALFRESCO_REINDEX_TO_ID", Long.toString(toId))
                .withEnv("ALFRESCO_REINDEX_JOB_NAME", "reindexByIds")
                .withLogConsumer(of -> System.err.print("[reIndexing]" + ((OutputFrame) of).getUtf8String()))
                .withNetwork(alfresco.getNetwork());

        reIndexing.start();
        waitFor("Re-indexing Startup", Duration.ofMinutes(1), reIndexing::isRunning);
        waitFor("Re-indexing Exit", Duration.ofMinutes(5), () -> !reIndexing.isRunning());
    }

    public static void waitFor(String description, final Duration timeout, final BooleanSupplier condition)
    {
        final Duration step = Duration.ofMillis(200);
        Duration remaining = timeout;
        while (!remaining.isNegative())
        {
            if (condition.getAsBoolean())
            {
                return;
            }
            remaining = remaining.minus(step);
            Uninterruptibles.sleepUninterruptibly(step.toMillis(), TimeUnit.MILLISECONDS);
        }
        throw new IllegalStateException("Failed to wait for " + description + ".");
    }

    public void startLiveIndexing()
    {
        final GenericContainer liveIndexing = newContainer(GenericContainer.class, "quay.io/alfresco/alfresco-elasticsearch-live-indexing:3.1.1")
                .withEnv("ELASTICSEARCH_INDEXNAME", "alfresco")
                .withEnv("SPRING_ELASTICSEARCH_REST_URIS", "http://elasticsearch:9200")
                .withEnv("SPRING_ACTIVEMQ_BROKERURL", "nio://activemq:61616")
                .withEnv("ALFRESCO_SHAREDFILESTORE_BASEURL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file/")
                .withEnv("ALFRESCO_ACCEPTEDCONTENTMEDIATYPESCACHE_BASEURL", "http://transform-core-aio:8090/transform/config")
                .withEnv("LOGGING_LEVEL_ORG_ALFRESCO", "DEBUG")
                .withLogConsumer(of -> System.err.print("[liveIndexing]" + ((OutputFrame) of).getUtf8String()))
                .withNetwork(alfresco.getNetwork());

        liveIndexing.start();
    }

    private <T extends GenericContainer> T  newContainer(Class<T> clazz, String imageRepository)
    {
        try
        {
            final T container = clazz.getConstructor(String.class).newInstance(imageRepository);
            createdContainers.add(container);
            return container;
        } catch (Exception e)
        {
            throw new IllegalStateException("Unexpected!");
        }
    }
}

class RepoHttpClient
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
        try (CloseableHttpResponse response = client.execute(request))
        {
            if (response.getStatusLine().getStatusCode() != requiredStatusCode)
            {
                return Optional.empty();
            }

            final ContentType contentType = ContentType.parse(response.getEntity().getContentType().getValue());
            if (!ContentType.APPLICATION_JSON.getMimeType().equals(contentType.getMimeType()))
            {
                return Optional.empty();
            }

            return Optional.of(gson.fromJson(EntityUtils.toString(response.getEntity()), Map.class));
        }
    }

    private String searchQuery(String term)
    {
        return "{\"query\":{\"language\":\"afts\",\"query\":\"" + term + "\"}}";
    }
}
