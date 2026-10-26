package org.alfresco.elasticsearch.upgrade;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.awaitility.core.ConditionEvaluationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

/**
 * Collects evidence from Testcontainers containers while they are still running.
 */
final class ContainerDiagnostics
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerDiagnostics.class);
    private static final int DEFAULT_LOG_LINES = 100;
    private static final String HEADER = "----- container diagnostics -----";

    private ContainerDiagnostics()
    {}

    public static ConditionEvaluationListener<Object> dumpOnTimeout(
            Collection<? extends GenericContainer<?>> containers)
    {
        return Utils.onTimeout(timeoutEvent -> {
            LOGGER.error("Timed out waiting for {} after {}ms. Container diagnostics follow.",
                    timeoutEvent.getAlias(), timeoutEvent.getElapsedTimeInMS());
            // One record per line - see %replace in log4j2.properties.
            of(containers).lines().forEach(LOGGER::error);
        });
    }

    public static String of(Collection<? extends GenericContainer<?>> containers)
    {
        return containers
                .stream()
                .map(ContainerDiagnostics::describe)
                .collect(Collectors.joining(System.lineSeparator(), HEADER + System.lineSeparator(), ""));
    }

    public static String exitDescription(GenericContainer<?> container)
    {
        final InspectContainerResponse.ContainerState state = container.getCurrentContainerInfo().getState();
        return "exit code " + state.getExitCodeLong() + " (OOM killed: " + state.getOOMKilled() + ")"
                + System.lineSeparator() + lastLogLines(container);
    }

    public static boolean exitedSuccessfully(GenericContainer<?> container)
    {
        final Long exitCode = container.getCurrentContainerInfo().getState().getExitCodeLong();
        return exitCode != null && exitCode == 0L;
    }

    public static String lastLogLines(GenericContainer<?> container)
    {
        return lastLogLines(container, DEFAULT_LOG_LINES);
    }

    public static String lastLogLines(GenericContainer<?> container, int lines)
    {
        try
        {
            final String[] allLines = container.getLogs().split("\\R");
            return Stream
                    .of(allLines)
                    .skip(Math.max(0, allLines.length - lines))
                    .collect(Collectors.joining(System.lineSeparator()));
        }
        catch (RuntimeException e)
        {
            // Never let diagnostics collection replace the failure it is trying to explain.
            return "failed to read the container logs: " + e;
        }
    }

    private static String describe(GenericContainer<?> container)
    {
        return "--- " + container.getDockerImageName() + " running=" + container.isRunning() + " ---"
                + System.lineSeparator() + lastLogLines(container);
    }
}
