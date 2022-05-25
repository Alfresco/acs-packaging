package org.alfresco.elasticsearch.upgrade;

import static java.time.Duration.ofMinutes;

import static org.alfresco.elasticsearch.upgrade.Utils.waitFor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ConnectToNetworkCmd;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.google.gson.Gson;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

class Elasticsearch implements AutoCloseable
{
    private static final int ES_API_TIMEOUT_MS = 5_000;

    private final Config cfg;
    private final GenericContainer<?> elasticsearch;
    private final Collection<String> additionalNetworks;
    private final Gson gson = new Gson();

    public Elasticsearch(Config cfg, Network network, Network... networks)
    {
        this.cfg = cfg;

        additionalNetworks = Stream.of(networks).map(Network::getId).collect(Collectors.toUnmodifiableSet());

        elasticsearch = new GenericContainer<>(cfg.getElasticsearchImage())
                .withEnv("xpack.security.enabled", "false")
                .withEnv("discovery.type", "single-node")
                .withNetworkAliases(cfg.getElasticsearchHostname())
                .withNetwork(network)
                .withExposedPorts(9200);
    }

    public long getIndexedDocumentCount() throws IOException
    {
        final Map<?, ?> countResponse = gson.fromJson(getString("/" + cfg.getIndexName() + "/_count"), Map.class);
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
            return getString("/" + cfg.getIndexName() + "/_mapping").contains("cm%3Acontent");
        } catch (FileNotFoundException e)
        {
            return false;
        }
    }

    private String getString(String path) throws IOException
    {
        final URL url = getRequestUrl(path);

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
        waitForAvailability();

        connectElasticSearchToAdditionalNetworks();
        waitForAvailability();
    }

    private URL getRequestUrl(String path)
    {
        try
        {
            final URI uri = URI
                    .create("http://" + elasticsearch.getHost() + ":" + elasticsearch.getMappedPort(9200))
                    .resolve(path);
            return uri.toURL();
        } catch (MalformedURLException e)
        {
            throw new RuntimeException("Failed to create a valid url.", e);
        }
    }

    private void waitForAvailability()
    {
        for (int i = 0; i < 3; i++)
        {
            waitFor("Elasticsearch Startup", ofMinutes(1), () -> {
                try
                {
                    return !isIndexCreated();
                } catch (IOException e)
                {
                    return false;
                }
            });
        }
    }

    private void connectElasticSearchToAdditionalNetworks()
    {
        final DockerClient client = elasticsearch.getDockerClient();
        final String containerId = elasticsearch.getContainerId();

        additionalNetworks
                .stream()
                .map(client.connectToNetworkCmd()
                           .withContainerId(containerId)
                           .withContainerNetwork(new ContainerNetwork()
                                   .withAliases(elasticsearch.getNetworkAliases()))::withNetworkId)
                .forEach(ConnectToNetworkCmd::exec);
    }

    @Override
    public void close()
    {
        elasticsearch.close();
    }
}
