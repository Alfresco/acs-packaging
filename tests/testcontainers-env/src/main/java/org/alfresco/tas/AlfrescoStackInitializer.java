package org.alfresco.tas;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.github.dockerjava.api.command.CreateContainerCmd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.Startables;
import org.testng.Assert;
import org.testng.util.Strings;

public class AlfrescoStackInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>
{
    public static final String CUSTOM_ALFRESCO_INDEX = "custom-alfresco-index";
    private static Logger LOGGER = LoggerFactory.getLogger(AlfrescoStackInitializer.class);
    private static Slf4jLogConsumer LOG_CONSUMER = new Slf4jLogConsumer(LOGGER);

    public static Network network;

    public static GenericContainer alfresco;

    public static GenericContainer searchEngineContainer;

    /** To create the kibana container for a test run then pass -Dkibana=true as an argument to the mvn command. */
    public static GenericContainer dashboardsContainer;

    public static GenericContainer liveIndexer;

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext)
    {

        // Wait till existing containers are stopped
        if (alfresco != null)
        {
            if (alfresco.getDockerClient().listContainersCmd().withShowAll(true).exec().size() > 0)
            {
                try
                {
                    LOGGER.info("Waiting for living containers to be stopped...");
                    Thread.sleep(10000);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }

        network = Network.newNetwork();

        alfresco = createAlfrescoContainer();

        GenericContainer transformRouter = createTransformRouterContainer();

        GenericContainer transformCore = createTransformCoreContainer();

        GenericContainer sfs = createSfsContainer();

        PostgreSQLContainer postgres = createPosgresContainer();

        GenericContainer activemq = createAMQContainer();

        searchEngineContainer = createSearchEngineContainer();

        startOrFail(searchEngineContainer);

        configureSecuritySettings(searchEngineContainer);

        startOrFail(postgres);

        startOrFail(activemq, sfs);

        startOrFail(transformCore, transformRouter);

        // We don't want Kibana to run on our CI, but it can be useful when investigating issues locally.
        if (Objects.equals(System.getProperty("kibana"), "true"))
        {
            dashboardsContainer = createDashboardsContainer();
            startOrFail(dashboardsContainer);
        }

        liveIndexer = createLiveIndexingContainer();

        startOrFail(liveIndexer);

        startOrFail(alfresco);

        alfresco.followOutput(LOG_CONSUMER);

        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(configurableApplicationContext,
                                                                  "alfresco.server=" + alfresco.getContainerIpAddress(),
                                                                  "alfresco.port=" + alfresco.getFirstMappedPort());

    }

    public void configureSecuritySettings(GenericContainer searchEngineContainer)
    {
        //empty for default execution
    }

    /**
     * Run the alfresco-elasticsearch-reindexing container with path reindexing enabled.
     */
    public static void reindexEverything()
    {
        reindex(Map.of("ALFRESCO_REINDEX_PATHINDEXINGENABLED", "true", // Ensure path reindexing is enabled.
                "ALFRESCO_REINDEX_JOB_NAME", "reindexByDate"));
    }

    public static Map<String, String> getReindexEnvBasic()
    {
        Map<String, String> env = new HashMap<>(
            Map.of("SPRING_ELASTICSEARCH_REST_URIS", "http://elasticsearch:9200",
                        "SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/alfresco",
                        "ELASTICSEARCH_INDEX_NAME", CUSTOM_ALFRESCO_INDEX,
                        "SPRING_ACTIVEMQ_BROKER-URL", "nio://activemq:61616",
                        "JAVA_TOOL_OPTIONS", "-Xmx1g",
                        "ALFRESCO_ACCEPTEDCONTENTMEDIATYPESCACHE_BASEURL", "http://transform-core-aio:8090/transform/config"));
        return env;
    }

        /**
         * Run the alfresco-elasticsearch-reindexing container.
         *
         * @param envParam Any environment variables to override from the defaults.
         */
    public static void reindex(Map<String, String> envParam)
    {
        // Run the reindexing container.
        Map<String, String> env = AlfrescoStackInitializer.getReindexEnvBasic();
        env.putAll(envParam);

        try (GenericContainer reindexingComponent = new GenericContainer(getImagesConfig().getReIndexingImage())
                .withEnv(env)
                .withNetwork(AlfrescoStackInitializer.network)
                .withStartupCheckStrategy(
                        new IndefiniteWaitOneShotStartupCheckStrategy()))
        {
            reindexingComponent.start();
        }
    }

    private void startOrFail(Startable... startables)
    {
        try
        {
            Startables.deepStart(startables).get();
        } catch (Exception e)
        {
            Assert.fail("Unable to start containers", e);
        }

    }

    protected GenericContainer createLiveIndexingContainer()
    {
        return new GenericContainer(getImagesConfig().getLiveIndexingImage())
                       .withNetwork(network)
                       .withNetworkAliases("live-indexing")
                       .withEnv("ELASTICSEARCH_INDEXNAME", CUSTOM_ALFRESCO_INDEX)
                       .withEnv("SPRING_ELASTICSEARCH_REST_URIS", "http://elasticsearch:9200")
                       .withEnv("SPRING_ACTIVEMQ_BROKERURL", "nio://activemq:61616")
                       .withEnv("ALFRESCO_SHAREDFILESTORE_BASEURL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file/")
                       .withEnv("ALFRESCO_ACCEPTEDCONTENTMEDIATYPESCACHE_BASEURL", "http://transform-core-aio:8090/transform/config");
    }

    protected GenericContainer createSearchEngineContainer()
    {
        return getImagesConfig().getSearchEngineType() == SearchEngineType.OPENSEARCH_ENGINE ?
                createOpensearchContainer() : createElasticContainer();
    }

    protected GenericContainer createDashboardsContainer()
    {
        return getImagesConfig().getSearchEngineType() == SearchEngineType.OPENSEARCH_ENGINE ?
                createOpensearchDashboardsContainer() : createKibanaContainer();
    }

    protected GenericContainer createElasticContainer()
    {
        return new GenericContainer<>(getImagesConfig().getElasticsearchImage())
                .withNetwork(network)
                .withNetworkAliases("elasticsearch")
                .withExposedPorts(9200)
                .withEnv("xpack.security.enabled", "false")
                .withEnv("discovery.type", "single-node")
                .withEnv("ES_JAVA_OPTS", "-Xms1g -Xmx1g")
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.getHostConfig()
                            .withMemory((long) 3400 * 1024 * 1024)
                            .withMemorySwap((long) 3400 * 1024 * 1024);
                });
    }

    protected GenericContainer createOpensearchContainer()
    {
        return new GenericContainer<>(getImagesConfig().getOpensearchImage())
                .withNetwork(network)
                .withNetworkAliases("elasticsearch")
                .withExposedPorts(9200)
                .withEnv("plugins.security.disabled", "true")
                .withEnv("discovery.type", "single-node")
                .withEnv("OPENSEARCH_JAVA_OPTS", "-Xms1g -Xmx1g")
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.getHostConfig()
                            .withMemory((long) 3400*1024*1024)
                            .withMemorySwap((long) 3400*1024*1024);
    });
    }

    protected GenericContainer createOpensearchDashboardsContainer()
    {
        return new GenericContainer(getImagesConfig().getOpensearchDashboardsImage())
                .withNetwork(network)
                .withNetworkAliases("kibana")
                .withExposedPorts(5601)
                .withEnv("ELASTICSEARCH_HOSTS", "http://elasticsearch:9200");
    }

    protected GenericContainer createKibanaContainer()
    {
        return new GenericContainer(getImagesConfig().getKibanaImage())
                .withNetwork(network)
                .withNetworkAliases("kibana")
                .withExposedPorts(5601)
                .withEnv("ELASTICSEARCH_HOSTS", "http://elasticsearch:9200");
    }

    private GenericContainer createAMQContainer()
    {
        return new GenericContainer(getImagesConfig().getActiveMqImage())
                       .withNetwork(network)
                       .withNetworkAliases("activemq")
                       .withEnv("JAVA_OPTS", "-Xms256m -Xmx512m")
                       .waitingFor(Wait.forListeningPort())
                       .withStartupTimeout(Duration.ofMinutes(2))
                       .withExposedPorts(61616, 8161, 5672, 61613);
    }

    private PostgreSQLContainer createPosgresContainer()
    {
        return (PostgreSQLContainer) new PostgreSQLContainer(getImagesConfig().getPostgreSQLImage())
                                             .withPassword("alfresco")
                                             .withUsername("alfresco")
                                             .withDatabaseName("alfresco")
                                             .withNetwork(network)
                                             .withNetworkAliases("postgres")
                                             .withStartupTimeout(Duration.ofMinutes(2));
    }

    private GenericContainer createSfsContainer()
    {
        return new GenericContainer(getImagesConfig().getSharedFileStoreImage())
                       .withNetwork(network)
                       .withNetworkAliases("shared-file-store")
                       .withEnv("JAVA_OPTS", "-Xms256m -Xmx256m")
                       .withEnv("scheduler.content.age.millis", "86400000")
                       .withEnv("scheduler.cleanup.interval", "86400000")
                       .withExposedPorts(8099)
                       .waitingFor(Wait.forListeningPort())
                       .withStartupTimeout(Duration.ofMinutes(2));
    }

    private GenericContainer createTransformCoreContainer()
    {
        return new GenericContainer(getImagesConfig().getTransformCoreAIOImage())
                       .withNetwork(network)
                       .withNetworkAliases("transform-core-aio")
                       .withEnv("JAVA_OPTS", "-Xms512m -Xmx512m")
                       .withEnv("ACTIVEMQ_URL", "nio://activemq:61616")
                       .withEnv("FILE_STORE_URL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file")
                       .withExposedPorts(8090)
                       .waitingFor(Wait.forListeningPort())
                       .withStartupTimeout(Duration.ofMinutes(2));
    }

    private GenericContainer createTransformRouterContainer()
    {
        return new GenericContainer(getImagesConfig().getTransformRouterImage())
                       .withNetwork(network)
                       .withNetworkAliases("transform-router")
                       .withEnv("JAVA_OPTS", "-Xms256m -Xmx256m")
                       .withEnv("ACTIVEMQ_URL", "nio://activemq:61616")
                       .withEnv("CORE_AIO_URL", "http://transform-core-aio:8090")
                       .withEnv("FILE_STORE_URL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file")
                       .withExposedPorts(8095)
                       .waitingFor(Wait.forListeningPort())
                       .withStartupTimeout(Duration.ofMinutes(2));
    }

    protected GenericContainer createAlfrescoContainer()
    {
        return new GenericContainer(getImagesConfig().getRepositoryImage())
                       .withEnv("CATALINA_OPTS", "\"-agentlib:jdwp=transport=dt_socket,address=*:8000,server=y,suspend=n\"")
                       .withEnv("JAVA_TOOL_OPTIONS",
                                "-Dencryption.keystore.type=JCEKS " +
                                "-Dencryption.cipherAlgorithm=DESede/CBC/PKCS5Padding " +
                                "-Dencryption.keyAlgorithm=DESede " +
                                "-Dencryption.keystore.location=/usr/local/tomcat/shared/classes/alfresco/extension/keystore/keystore " +
                                "-Dmetadata-keystore.password=mp6yc0UD9e -Dmetadata-keystore.aliases=metadata " +
                                "-Dmetadata-keystore.metadata.password=oKIWzVdEdA -Dmetadata-keystore.metadata.algorithm=DESede")
                       .withEnv("JAVA_OPTS",
                                "-Delasticsearch.createIndexIfNotExists=true " +
                                "-Ddb.driver=org.postgresql.Driver " +
                                "-Ddb.username=alfresco " +
                                "-Ddb.password=alfresco " +
                                "-Ddb.url=jdbc:postgresql://postgres:5432/alfresco " +
                                "-Dindex.subsystem.name=elasticsearch " +
                                "-Delasticsearch.host=elasticsearch " +
                                "-Delasticsearch.indexName=" + CUSTOM_ALFRESCO_INDEX + " " +
                                "-Dshare.host=127.0.0.1 " +
                                "-Dshare.port=8080 " +
                                "-Dalfresco.host=localhost " +
                                "-Dalfresco.port=8080 " +
                                "-Daos.baseUrlOverwrite=http://localhost:8080/alfresco/aos " +
                                "-Dmessaging.broker.url=\"failover:(nio://activemq:61616)?timeout=3000&jms.useCompression=true\" " +
                                "-Ddeployment.method=DOCKER_COMPOSE " +
                                "-Dtransform.service.enabled=true " +
                                "-Dtransform.service.url=http://transform-router:8095 " +
                                "-Dsfs.url=http://shared-file-store:8099 " +
                                "-DlocalTransform.core-aio.url=http://transform-core-aio:8090/ " +
                                "-Dcsrf.filter.enabled=false " +
                                "-Dalfresco.restApi.basicAuthScheme=true " +
                                "-Dquery.cmis.queryConsistency=EVENTUAL " +
                                "-Xms1500m -Xmx1500m ")
                       .withNetwork(network)
                       .withNetworkAliases("alfresco")
                       .waitingFor(new LogMessageWaitStrategy().withRegEx(".*Server startup in.*\\n"))
                       .withStartupTimeout(Duration.ofMinutes(7))
                       .withExposedPorts(8080, 8000)
                       .withClasspathResourceMapping("exactTermSearch.properties",
                        "/usr/local/tomcat/webapps/alfresco/WEB-INF/classes/alfresco/search/elasticsearch/config/exactTermSearch.properties",
                        BindMode.READ_ONLY);
    }

    public static ImagesConfig getImagesConfig()
    {
        return DefaultImagesConfig.INSTANCE;
    }

    public interface ImagesConfig
    {
        String getReIndexingImage();

        String getLiveIndexingImage();

        String getElasticsearchImage();

        String getOpensearchImage();

        String getOpensearchDashboardsImage();

        String getActiveMqImage();

        String getTransformRouterImage();

        String getTransformCoreAIOImage();

        String getSharedFileStoreImage();

        String getPostgreSQLImage();

        String getRepositoryImage();

        String getKibanaImage();

        SearchEngineType getSearchEngineType();
    }

    private static final class DefaultImagesConfig implements ImagesConfig
    {
        private static final DefaultImagesConfig INSTANCE = new DefaultImagesConfig(EnvHelper::getEnvProperty, MavenPropertyHelper::getMavenProperty);
        private final Function<String, String> envProperties;
        private final Function<String, String> mavenProperties;

        DefaultImagesConfig(Function<String, String> envProperties, Function<String, String> mavenProperties)
        {
            this.envProperties = envProperties;
            this.mavenProperties = mavenProperties;
        }

        @Override
        public String getReIndexingImage()
        {
            return "quay.io/alfresco/alfresco-elasticsearch-reindexing:" + getElasticsearchConnectorImageTag();
        }

        @Override
        public String getLiveIndexingImage()
        {
            return "quay.io/alfresco/alfresco-elasticsearch-live-indexing:" + getElasticsearchConnectorImageTag();
        }

        @Override
        public String getElasticsearchImage()
        {
            return "docker.elastic.co/elasticsearch/elasticsearch:" + envProperties.apply("ES_TAG");
        }

        @Override
        public String getOpensearchImage() {
            return "opensearchproject/opensearch:" + envProperties.apply("OPENSEARCH_TAG");
        }

        @Override
        public String getOpensearchDashboardsImage() {
            return "opensearchproject/opensearch-dashboards:" + envProperties.apply("OPENSEARCH_DASHBOARDS_TAG");
        }

        @Override
        public String getActiveMqImage()
        {
            return "alfresco/alfresco-activemq:" + envProperties.apply("ACTIVEMQ_TAG");
        }

        @Override
        public String getTransformRouterImage()
        {
            return "quay.io/alfresco/alfresco-transform-router:" + mavenProperties.apply("dependency.alfresco-transform-service.version");
        }

        @Override
        public String getTransformCoreAIOImage()
        {
            return "alfresco/alfresco-transform-core-aio:" + mavenProperties.apply("dependency.alfresco-transform-core.version");
        }

        @Override
        public String getSharedFileStoreImage()
        {
            return "quay.io/alfresco/alfresco-shared-file-store:" + mavenProperties.apply("dependency.alfresco-transform-service.version");
        }

        @Override
        public String getPostgreSQLImage()
        {
            return "postgres:" + envProperties.apply("POSTGRES_TAG");
        }

        @Override
        public String getRepositoryImage()
        {
            return "alfresco/alfresco-content-repository:latest";
        }

        @Override
        public String getKibanaImage()
        {
            return "kibana:" + envProperties.apply("KIBANA_TAG");
        }

        @Override
        public SearchEngineType getSearchEngineType() {
            String searchEngineTypeProperty = mavenProperties.apply("search.engine.type");
            if(Strings.isNullOrEmpty(searchEngineTypeProperty))
            {
                throw new IllegalArgumentException("Property 'search.engine.type' not set.");

            }
            return SearchEngineType.from(searchEngineTypeProperty);
        }

        private String getElasticsearchConnectorImageTag()
        {
            final String fromEnv = envProperties.apply("ES_CONNECTOR_TAG");
            if (fromEnv != null && !fromEnv.isBlank())
            {
                return fromEnv;
            }
            return mavenProperties.apply("dependency.elasticsearch-shared.version");
        }
    }
}
