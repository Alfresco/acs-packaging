package org.alfresco.elasticsearch.upgrade;

import static java.time.Duration.ofMinutes;

import static org.alfresco.elasticsearch.upgrade.Utils.waitFor;

import java.io.File;
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
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alfresco.elasticsearch.upgrade.AvailabilityProbe.ProbeResult;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.Transferable;

abstract class BaseACSEnv implements AutoCloseable
{
    protected final Config cfg;
    private final List<GenericContainer<?>> createdContainers = new ArrayList<>();

    private RepoHttpClient repoHttpClient;

    private String metadataDumpToRestore;
    private Path alfDataHostPath;
    private boolean readOnlyContentStore;

    private final AtomicReference<AvailabilityProbe> searchAPIAvailabilityProbe = new AtomicReference<>();

    protected BaseACSEnv(Config cfg)
    {
        this.cfg = Objects.requireNonNull(cfg);
    }

    protected abstract GenericContainer<?> getAlfresco();

    protected abstract GenericContainer<?> getPostgres();

    public void setMetadataDumpToRestore(String metadataDumpToRestore)
    {
        this.metadataDumpToRestore = Objects.requireNonNull(metadataDumpToRestore);
    }

    public void setContentStoreHostPath(Path hostPath)
    {
        readOnlyContentStore = false;
        alfDataHostPath = hostPath;
    }

    public Path getContentStoreHostPath()
    {
        return alfDataHostPath;
    }

    public void setReadOnlyContentStoreHostPath(Path hostPath)
    {
        readOnlyContentStore = true;
        alfDataHostPath = hostPath;
    }

    public void start()
    {
        if (getAlfresco().isRunning())
        {
            throw new IllegalStateException("Already started");
        }

        if (metadataDumpToRestore != null)
        {
            getPostgres().start();
            getPostgres().copyFileToContainer(Transferable.of(metadataDumpToRestore), "/opt/alfresco/pg-dump-alfresco.sql");
            execInPostgres("cat /opt/alfresco/pg-dump-alfresco.sql | psql -U alfresco");
        }

        if (alfDataHostPath != null)
        {
            final String hostPath = alfDataHostPath.toAbsolutePath().toString();
            final BindMode bindMode = readOnlyContentStore ? BindMode.READ_ONLY : BindMode.READ_WRITE;
            getAlfresco().addFileSystemBind(hostPath, getContainerAlfDataPath(), bindMode);
        }

        createdContainers.forEach(GenericContainer::start);
        repoHttpClient = new RepoHttpClient(URI.create("http://" + getAlfresco().getHost() + ":" + getAlfresco().getMappedPort(8080)));

        waitUntilServerIsUp(ofMinutes(5));
    }

    public AvailabilityProbe getRunningSearchAPIAvailabilityProbe()
    {
        final AvailabilityProbe current = searchAPIAvailabilityProbe.get();
        if (current != null) return current;

        final AvailabilityProbe created = AvailabilityProbe.create(10, this::checkSearchAPIAvailability);
        if (searchAPIAvailabilityProbe.compareAndSet(null, created))
        {
            created.start();
            return created;
        }

        return searchAPIAvailabilityProbe.get();
    }

    @Override
    public void close()
    {
        Optional.ofNullable(searchAPIAvailabilityProbe.get()).ifPresent(AvailabilityProbe::stop);
        createdContainers.forEach(GenericContainer::stop);
    }

    public String getMetadataDump()
    {
        return execInPostgres("pg_dump -c -U alfresco alfresco").getStdout();
    }

    public long getMaxNodeDbId()
    {
        return Long.parseLong(execInPostgres("psql -U alfresco -t -c 'SELECT max(id) FROM alf_node'").getStdout().strip());
    }

    public UUID uploadFile(URL contentUrl, String fileName) throws IOException
    {
        return repoHttpClient.uploadFile(contentUrl, fileName);
    }

    public void setElasticsearchSearchService() throws IOException
    {
        repoHttpClient.setSearchService("elasticsearch");
    }

    public void tryToUploadLicence(String licencePath)
    {
        try
        {
            File licence = new File(licencePath);
            if (!licence.isFile() || !licence.canRead())
            {
                throw new RuntimeException("Licence file at: %s is not a file or is not readable".formatted(licencePath));
            }
            repoHttpClient.uploadLicense(licence);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to upload a licence.", e);
        }
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

    protected String getContainerAlfDataPath()
    {
        return "/usr/local/tomcat/alf_data";
    }

    protected <T extends GenericContainer<?>> T newContainer(Class<T> clazz, String image)
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

    protected <T extends GenericContainer<?>> T newContainer(Class<T> clazz, ImageFromDockerfile image)
    {
        try
        {
            final T container = clazz.getConstructor(Future.class).newInstance(image);
            createdContainers.add(container);
            return container;
        } catch (Exception e)
        {
            throw new RuntimeException("Failed to create a container for `" + image + "`.", e);
        }
    }

    protected void removeCreatedContainer(GenericContainer<?> container)
    {
        createdContainers.remove(container);
    }

    protected void registerCreatedContainer(GenericContainer<?> container)
    {
        createdContainers.add(container);
    }

    private void waitUntilServerIsUp(Duration timeout)
    {
        waitFor("Reaching the point where server is up and running.", timeout, () -> {
            try
            {
                return repoHttpClient.isServerUp();
            } catch (IOException e)
            {
                return false;
            }
        });
    }

    private ExecResult execInPostgres(String command)
    {
        return execInContainer(getPostgres(), command);
    }

    private ExecResult execInContainer(GenericContainer<?> container, String command)
    {
        final ExecResult result;
        try
        {
            result = container.execInContainer("sh", "-c", command);
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

    private ProbeResult checkSearchAPIAvailability()
    {
        try
        {
            return repoHttpClient.searchForFiles("testing").map(v -> ProbeResult.ok()).orElseGet(ProbeResult::fail);
        } catch (Exception e)
        {
            return ProbeResult.fail(e);
        }
    }
}
