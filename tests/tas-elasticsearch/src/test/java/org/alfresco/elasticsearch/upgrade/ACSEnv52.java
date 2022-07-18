package org.alfresco.elasticsearch.upgrade;

import com.github.dockerjava.api.command.CreateContainerCmd;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;

class ACSEnv52 extends LegacyACSEnv
{
    private final GenericContainer<?> postgres;
    private final GenericContainer<?> alfresco;

    public ACSEnv52(Config cfg, final Network network)
    {
        super(cfg);
        postgres = createPostgresContainer(network);

        createLibreOfficeContainer(network);
        createSolr6Container(network);

        alfresco = createRepositoryContainer(network);
    }

    @Override
    protected GenericContainer<?> getAlfresco()
    {
        return alfresco;
    }

    @Override
    protected GenericContainer<?> getPostgres()
    {
        return postgres;
    }

    @Override
    protected String getContainerAlfDataPath()
    {
        return "/usr/local/alf_data";
    }

    private GenericContainer<?> createPostgresContainer(Network network)
    {
        return newContainer(PostgreSQLContainer.class, "postgres:9.4")
                .withPassword("alfresco")
                .withUsername("alfresco")
                .withDatabaseName("alfresco")
                .withNetwork(network)
                .withNetworkAliases("postgres");
    }

    private void createLibreOfficeContainer(Network network)
    {
        newContainer(GenericContainer.class, "xcgd/libreoffice")
                .withNetwork(network)
                .withNetworkAliases("libreoffice");
    }

    private void createSolr6Container(Network network)
    {
        newContainer(GenericContainer.class,"alfresco/alfresco-search-services:1.3.0.6")
                .withEnv("SOLR_ALFRESCO_HOST", "alfresco")
                .withEnv("SOLR_ALFRESCO_PORT", "8080")
                .withEnv("ALFRESCO_SECURE_COMMS", "none")
                .withEnv("SOLR_SOLR_HOST", "solr6")
                .withEnv("SOLR_SOLR_PORT", "8983")
                .withEnv("SOLR_CREATE_ALFRESCO_DEFAULTS", "alfresco,archive")
                .withEnv("SOLR_JAVA_MEM", "-Xms1g -Xmx1g")
                .withNetwork(network)
                .withNetworkAliases("solr6");
    }

    private GenericContainer<?> createRepositoryContainer(Network network)
    {
        return newContainer(GenericContainer.class, "quay.io/alfresco/alfresco-content-repository-52:5.2.6")
                .withExposedPorts(8080)
                .withEnv("JAVA_TOOL_OPTIONS",
                        "-Dsolr.host=solr6 " +
                                "-Dsolr.port=8983 " +
                                "-Dsolr.secureComms=none "+
                                "-Dsolr.base.url=/solr " +
                                "-Dindex.subsystem.name=solr6")
                .withNetwork(network)
                .withCreateContainerCmdModifier(cmd -> ((CreateContainerCmd)cmd).withUser("2000:2000"))
                .withNetworkAliases("alfresco");
    }

    @Override
    public ACSEnv upgradeToCurrent()
    {
        removeCreatedContainer(postgres);
        close();

        final ACSEnv upgradedEnv = new ACSEnv(postgres, cfg, "elasticsearch");

        upgradedEnv.setContentStoreHostPath(this.getContentStoreHostPath());

        return upgradedEnv;
    }
}
