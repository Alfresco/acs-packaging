package org.alfresco.elasticsearch.upgrade;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testng.annotations.Test;

public class FromSolrUpgradeTest
{
    @Test
    public void testIt()
    {
        RepoWithSolrSearchEngine.createRunning();
    }
}

class RepoWithSolrSearchEngine implements Closeable
{
    private final Network network;
    private final GenericContainer alfresco;
    private final GenericContainer postgres;
    private final GenericContainer solr6;

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
                .withNetworkAliases("postgres")
                .withStartupTimeout(Duration.ofMinutes(2));

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

/*
* solr6:
    image: alfresco/alfresco-search-services:2.0.3
    deploy:
      resources:
        limits:
          memory: 1536M
    environment:
      # Solr needs to know how to register itself with Alfresco
      SOLR_ALFRESCO_HOST: "alfresco"
      SOLR_ALFRESCO_PORT: "8080"
      # Alfresco needs to know how to call solr
      SOLR_SOLR_HOST: "solr6"
      SOLR_SOLR_PORT: "8983"
      # Create the default alfresco and archive cores
      SOLR_CREATE_ALFRESCO_DEFAULTS: "alfresco,archive"
      # HTTPS or SECRET
      ALFRESCO_SECURE_COMMS: "secret"
      # SHARED SECRET VALUE
      JAVA_TOOL_OPTIONS: "
          -Dalfresco.secureComms.secret=secret
        "
    ports:
      - "8083:8983" # Browser port
* */

        alfresco = new GenericContainer("quay.io/alfresco/alfresco-content-repository:latest")
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
                .withExposedPorts(8080);
    }

    @Override
    public void close()
    {

    }

    private void start()
    {
        postgres.start();
        solr6.start();
        alfresco.start();

        LocalDateTime start = LocalDateTime.now();

        while (LocalDateTime.now().isBefore(start.plus(5, ChronoUnit.MINUTES)))
        {
            //System.out.println(LocalDateTime.now() + ":");
            try
            {
                HttpURLConnection c = (HttpURLConnection) new URL("http://localhost:" + alfresco.getMappedPort(8080) + "/alfresco/api/-default-/public/search/versions/1/search").openConnection();
                c.setConnectTimeout(5_000);
                c.setReadTimeout(5_000);
                c.setDoOutput(true);
                c.setRequestProperty("Authorization", "Basic YWRtaW46YWRtaW4=");
                c.setRequestProperty("Content-Type", "application/json");
                c.setRequestMethod("POST");


                try (OutputStreamWriter writer = new OutputStreamWriter(c.getOutputStream()))
                {
                    String query = "{\"query\":{\"language\":\"afts\",\"query\":\"alabama\"}}";
                    //System.out.println("writing: " + query);
                    writer.write(query);
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(c.getInputStream())))
                {
                    String response = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                    if (c.getResponseCode() == 200)
                    {
                        Map<String, ?> searchResponse = new Gson().fromJson(response, Map.class);
                        Collection<?> entries = ((Collection<?>) ((Map<String, ?>) searchResponse.get("list")).get("entries"));
                        if (entries.isEmpty())
                        {
                            System.out.println("Done in " + Duration.between(start, LocalDateTime.now()));
                            break;
                        }
                    }
                }
            } catch (IOException | UncheckedIOException e)
            {
                //System.err.println(e.getClass() + " -> " + e.getMessage());
            }
        }
    }
}

class TestEnvironment implements Closeable
{
    private final Map<String, Network> networks = new HashMap<>();
    private final Map<String, GenericContainer> containers = new HashMap<>();

    public void createContainer()
    {
        final GenericContainer c = new GenericContainer<>("quay.io/alfresco/alfresco-content-repository:latest").withExposedPorts(8080);

        c.start();
    }

    @Override
    public void close()
    {
        networks.values().forEach(n -> n.close());
        networks.clear();

        containers.values().forEach(c -> c.close());
        containers.clear();
    }
}
