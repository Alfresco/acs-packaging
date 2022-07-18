package org.alfresco.elasticsearch.upgrade;

import static org.alfresco.elasticsearch.upgrade.Utils.createNetwork;
import static org.alfresco.elasticsearch.upgrade.Utils.createTempContentStoreDirectory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;

import org.springframework.util.FileSystemUtils;
import org.testcontainers.containers.Network;

public class LegacyAcsUpgradeScenario implements AutoCloseable
{
    private final LegacyACSEnv initialEnv;
    private final Elasticsearch elasticsearch;
    private final ACSEnv mirroredEnv;
    private final Path initialEnvContentStorePath;
    private final Path mirroredEnvContentStorePath;

    LegacyAcsUpgradeScenario(Config cfg)
    {
        final Network initialEnvNetwork = createNetwork("B");
        final Network mirroredEnvNetwork = createNetwork("A");

        initialEnvContentStorePath = createTempContentStoreDirectory();
        mirroredEnvContentStorePath = createTempContentStoreDirectory();

        initialEnv = new ACSEnv52(cfg, initialEnvNetwork);
        initialEnv.setContentStoreHostPath(initialEnvContentStorePath);

        elasticsearch = new Elasticsearch(cfg, mirroredEnvNetwork, initialEnvNetwork);

        mirroredEnv = new ACSEnv(cfg, mirroredEnvNetwork, "elasticsearch");
        mirroredEnv.setContentStoreHostPath(mirroredEnvContentStorePath);
    }

    public LegacyACSEnv startLegacyEnv()
    {
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
        try
        {
            System.out.println("initialEnvContentStorePath: " + initialEnvContentStorePath);
            System.out.println("mirroredEnvContentStorePath: " + mirroredEnvContentStorePath);
            Files.walkFileTree(initialEnvContentStorePath, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                {
                    System.out.print(file);
                    System.out.println(" F-> " + PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
                    return super.visitFile(file, attrs);
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
                {
                    System.out.print(dir);
                    System.out.println(" F-> " + PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
                    return super.preVisitDirectory(dir, attrs);
                }
            });
            FileSystemUtils.copyRecursively(initialEnvContentStorePath, mirroredEnvContentStorePath);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to replicate content store.", e);
        }
        mirroredEnv.start();

        uploadLicence(mirroredEnv);

        return mirroredEnv;
    }

    @Override
    public void close()
    {
        initialEnv.close();
        elasticsearch.close();
        mirroredEnv.close();
    }

    public ACSEnv upgradeLegacyEnvironmentToCurrent()
    {
        final ACSEnv upgradedEnv = initialEnv.upgradeToCurrent();
        upgradedEnv.start();

        uploadLicence(upgradedEnv);

        return upgradedEnv;
    }

    private void uploadLicence(ACSEnv env)
    {
        if (!env.uploadLicence("/Users/pzurek/Downloads/alf73-allenabled.lic"))
        {
            throw new RuntimeException("Failed to upload licence.");
        }
    }
}
