package org.alfresco.elasticsearch.upgrade;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.UUID;
import java.util.function.Consumer;

import org.awaitility.core.ConditionEvaluationListener;
import org.awaitility.core.EvaluatedCondition;
import org.awaitility.core.TimeoutEvent;
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

    public static ConditionEvaluationListener<Object> onTimeout(Consumer<TimeoutEvent> action)
    {
        return new ConditionEvaluationListener<>() {
            @Override
            public void conditionEvaluated(EvaluatedCondition<Object> condition)
            {
                // Only the timeout is of interest.
            }

            @Override
            public void onTimeout(TimeoutEvent timeoutEvent)
            {
                action.accept(timeoutEvent);
            }
        };
    }
}
