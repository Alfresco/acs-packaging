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
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ConnectToNetworkCmd;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.google.gson.Gson;

import org.alfresco.tas.SearchEngineType;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

class Elasticsearch implements AutoCloseable
{
    private static final int ES_API_TIMEOUT_MS = 5_000;

    private final Config cfg;
    private final GenericContainer<?> searchContainer;
    private final Collection<String> additionalNetworks;
    private final Gson gson = new Gson();

    public Elasticsearch(Config cfg, Network network, Network... networks)
    {
        this.cfg = cfg;

        additionalNetworks = Stream.of(networks).map(Network::getId).collect(Collectors.toUnmodifiableSet());

        searchContainer = new GenericContainer<>(cfg.getSearchEngineImage())
                .withEnv("discovery.type", "single-node")
                .withEnv("network.publish_host", cfg.getElasticsearchHostname())
                .withNetworkAliases(cfg.getElasticsearchHostname())
                .withNetwork(network)
                .withExposedPorts(9200).withCreateContainerCmdModifier(cmd -> {
                    cmd.getHostConfig()
                            .withMemory((long)3400*1024*1024)
                            .withMemorySwap((long)3400*1024*1024);
                });

        if(SearchEngineType.ELASTICSEARCH_ENGINE == cfg.getSearchEngineType())
        {
            searchContainer.withEnv("xpack.security.enabled", "false");
        }
        if(SearchEngineType.OPENSEARCH_ENGINE == cfg.getSearchEngineType())
        {
            searchContainer.withEnv("plugins.security.disabled", "true");
        }

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

    public void waitForIndexCreation(Duration timeout)
    {
        waitFor("Elasticsearch Index created", timeout, () -> {
            try
            {
                return isIndexCreated();
            } catch (IOException e)
            {
                return false;
            }
        });
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
        if (searchContainer.isRunning())
        {
            throw new IllegalStateException("Already started");
        }

        searchContainer.start();
        waitForAvailability();

        connectElasticSearchToAdditionalNetworks();
        waitForAvailability();
    }

    private URL getRequestUrl(String path)
    {
        try
        {
            final URI uri = URI
                    .create("http://" + searchContainer.getHost() + ":" + searchContainer.getMappedPort(9200))
                    .resolve(path);
            return uri.toURL();
        } catch (MalformedURLException e)
        {
            throw new RuntimeException("Failed to create a valid url.", e);
        }
    }

    private void waitForAvailability()
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

    private void connectElasticSearchToAdditionalNetworks()
    {
        final DockerClient client = searchContainer.getDockerClient();
        final String containerId = searchContainer.getContainerId();

        additionalNetworks
                .stream()
                .map(client.connectToNetworkCmd()
                           .withContainerId(containerId)
                           .withContainerNetwork(new ContainerNetwork()
                                   .withAliases(searchContainer.getNetworkAliases()))::withNetworkId)
                .forEach(ConnectToNetworkCmd::exec);
    }

    @Override
    public void close()
    {
        searchContainer.close();
    }
}
