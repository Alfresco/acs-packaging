package org.alfresco.elasticsearch.upgrade;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class FromSolrUpgradeTest
{
    @Test
    public void testIt() throws IOException, InterruptedException
    {
        try (RepoWithSolrSearchEngine initialEnv = RepoWithSolrSearchEngine.createRunning())
        {
            initialEnv.expectSearchResult(Duration.ofSeconds(5), "alabama", Set.of());

            initialEnv.uploadFile(getClass().getResource("test.pdf"), "test1.pdf");
            initialEnv.expectSearchResult(Duration.ofMinutes(1), "babekyrtso", Set.of("test1.pdf"));

            initialEnv.uploadFile(getClass().getResource("test.pdf"), "test2.pdf");
            initialEnv.expectSearchResult(Duration.ofMinutes(1), "babekyrtso", Set.of("test1.pdf", "test2.pdf"));
        }
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
                try
                {
                    Optional<Set<String>> actual = repoHttpClient.searchForFiles(term);
                    if (actual.map(expected::equals).orElse(false)) return;
                    Thread.sleep(500);
                }
                catch (IOException e)
                {
                    System.out.println("Failed " + e.getClass() + " -> " + e.getMessage());
                    Thread.sleep(1_000);
                }
            } catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted.", e);
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

        public String uploadFile(URL contentUrl, String fileName) throws IOException
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

                System.out.println(uploadResult);
                return "OK";

            }
        }

        public Optional<Set<String>> searchForFiles(String term) throws IOException
        {
            final HttpPost searchRequest = new HttpPost(searchApiUri);
            searchRequest.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=");
            searchRequest.setEntity(new StringEntity(searchQuery(term), ContentType.APPLICATION_JSON));

            final Optional<Map<String, ?>> searchResult = getJsonResponse(searchRequest, HttpStatus.SC_OK);

            System.out.println(searchResult);
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
                    System.err.println(EntityUtils.toString(response.getEntity()));
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
}
