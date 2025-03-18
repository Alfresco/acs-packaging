package org.alfresco.elasticsearch.upgrade;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import com.google.common.util.concurrent.RateLimiter;
import org.testcontainers.containers.Network;

class Utils
{
    public static Path createTempContentStoreDirectory()
    {
        try
        {
            final Path tempDir = Files.createTempDirectory("alf_data");
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))
            {
                Files.setPosixFilePermissions(tempDir, PosixFilePermissions.fromString("rwxrwxrwx"));
            }
            return tempDir;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Couldn't create aa temp directory.", e);
        }
    }

    public static Network createNetwork(String prefix)
    {
        return Network
                .builder()
                .createNetworkCmdModifier(cmd -> cmd.withAttachable(true).withName(prefix + UUID.randomUUID()))
                .build();
    }

    public static void waitFor(String description, final Duration timeout, final BooleanSupplier condition)
    {
        final RateLimiter rateLimiter = RateLimiter.create(5);
        final long numberOfIterations = Math.max(1L, timeout.getSeconds() * (long) rateLimiter.getRate());
        for (long i = 0; i < numberOfIterations; i++)
        {
            rateLimiter.acquire();
            if (condition.getAsBoolean())
            {
                return;
            }
        }
        throw new RuntimeException("Failed to wait for " + description + ".");
    }
}
