package org.alfresco.elasticsearch;

import static org.alfresco.elasticsearch.EnvHelper.getEnvProperty;
import static org.alfresco.elasticsearch.MavenPropertyHelper.getMavenProperty;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.Startables;
import org.testng.Assert;

public class AlfrescoStackInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>
{
    public static final String CUSTOM_ALFRESCO_INDEX = "custom-alfresco-index";
    private static Logger LOGGER = LoggerFactory.getLogger(AlfrescoStackInitializer.class);
    private static Slf4jLogConsumer LOG_CONSUMER = new Slf4jLogConsumer(LOGGER);

    public static Network network;

    public static GenericContainer alfresco;

    public static ElasticsearchContainer elasticsearch;

    /** To create the kibana container for a test run then pass -Dkibana=true as an argument to the mvn command. */
    public static GenericContainer kibana;

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

        Properties env = EnvHelper.loadEnvProperties();

        network = Network.newNetwork();

        alfresco = createAlfrescoContainer();

        GenericContainer transformRouter = createTransformRouterContainer(env);

        GenericContainer transformCore = createTransformCoreContainer(env);

        GenericContainer sfs = createSfsContainer(env);

        PostgreSQLContainer postgres = createPosgresContainer(env);

        GenericContainer activemq = createAMQContainer(env);

        elasticsearch = createElasticContainer(env);

        startOrFail(elasticsearch);

        startOrFail(postgres);

        startOrFail(activemq, sfs);

        startOrFail(transformCore, transformRouter);

        // We don't want Kibana to run on our CI, but it can be useful when investigating issues locally.
        if (Objects.equals(System.getProperty("kibana"), "true"))
        {
            kibana = createKibanaContainer(elasticsearch);
            startOrFail(kibana);
        }

        liveIndexer = createLiveIndexingContainer();

        startOrFail(liveIndexer);

        startOrFail(alfresco);

        alfresco.followOutput(LOG_CONSUMER);

        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(configurableApplicationContext,
                                                                  "alfresco.server=" + alfresco.getContainerIpAddress(),
                                                                  "alfresco.port=" + alfresco.getFirstMappedPort());

    }

    public static String getElasticsearchConnectorImageTag()
    {
        final String fromEnv = getEnvProperty("ES_CONNECTOR_TAG");
        if (fromEnv != null && !fromEnv.isBlank())
        {
            return fromEnv;
        }
        return getMavenProperty("dependency.elasticsearch-shared.version");
    }

    /**
     * Run the alfresco-elasticsearch-reindexing container with path reindexing enabled.
     */
    public static void reindexEverything()
    {
        // Run the reindexing container.
        Map<String, String> env = new HashMap<>(
                Map.of("ALFRESCO_REINDEX_PATHINDEXINGENABLED", "true", // Ensure path reindexing is enabled.
                        "SPRING_ELASTICSEARCH_REST_URIS", "http://elasticsearch:9200",
                        "SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/alfresco",
                        "ELASTICSEARCH_INDEX_NAME", CUSTOM_ALFRESCO_INDEX,
                        "SPRING_ACTIVEMQ_BROKER-URL", "nio://activemq:61616",
                        "ALFRESCO_ACCEPTEDCONTENTMEDIATYPESCACHE_BASEURL", "http://transform-core-aio:8090/transform/config",
                        "ALFRESCO_REINDEX_JOB_NAME", "reindexByDate"));

        try (GenericContainer reindexingComponent = new GenericContainer("quay.io/alfresco/alfresco-elasticsearch-reindexing:" + getElasticsearchConnectorImageTag())
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
        return new GenericContainer("quay.io/alfresco/alfresco-elasticsearch-live-indexing:" + getElasticsearchConnectorImageTag())
                       .withNetwork(network)
                       .withNetworkAliases("live-indexing")
                       .withEnv("ELASTICSEARCH_INDEXNAME", CUSTOM_ALFRESCO_INDEX)
                       .withEnv("SPRING_ELASTICSEARCH_REST_URIS", "http://elasticsearch:9200")
                       .withEnv("SPRING_ACTIVEMQ_BROKERURL", "nio://activemq:61616")
                       .withEnv("ALFRESCO_SHAREDFILESTORE_BASEURL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file/")
                       .withEnv("ALFRESCO_ACCEPTEDCONTENTMEDIATYPESCACHE_BASEURL", "http://transform-core-aio:8090/transform/config");
    }

    protected ElasticsearchContainer createElasticContainer(Properties env)
    {
        return new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:" + env.getProperty("ES_TAG"))
                .withNetwork(network)
                .withNetworkAliases("elasticsearch")
                .withExposedPorts(9200)
                .withEnv("xpack.security.enabled", "false")
                .withEnv("discovery.type", "single-node")
                .withEnv("ES_JAVA_OPTS", "-Xms1g -Xmx1g");
    }

    protected GenericContainer createKibanaContainer(ElasticsearchContainer elasticsearch)
    {
        return new GenericContainer("kibana:7.10.1")
                .withNetwork(network)
                .withNetworkAliases("kibana")
                .withExposedPorts(5601)
                .withEnv("ELASTICSEARCH_HOSTS", "http://elasticsearch:9200");
    }

    private GenericContainer createAMQContainer(Properties env)
    {
        return new GenericContainer("alfresco/alfresco-activemq:" + env.getProperty("ACTIVEMQ_TAG"))
                       .withNetwork(network)
                       .withNetworkAliases("activemq")
                       .withEnv("JAVA_OPTS", "-Xms256m -Xmx512m")
                       .waitingFor(Wait.forListeningPort())
                       .withStartupTimeout(Duration.ofMinutes(2))
                       .withExposedPorts(61616, 8161, 5672, 61613);
    }

    private PostgreSQLContainer createPosgresContainer(Properties env)
    {
        return (PostgreSQLContainer) new PostgreSQLContainer("postgres:" + env.getProperty("POSTGRES_TAG"))
                                             .withPassword("alfresco")
                                             .withUsername("alfresco")
                                             .withDatabaseName("alfresco")
                                             .withNetwork(network)
                                             .withNetworkAliases("postgres")
                                             .withStartupTimeout(Duration.ofMinutes(2));
    }

    private GenericContainer createSfsContainer(Properties env)
    {
        return new GenericContainer("alfresco/alfresco-shared-file-store:" + env.getProperty("TRANSFORM_ROUTER_TAG"))
                       .withNetwork(network)
                       .withNetworkAliases("shared-file-store")
                       .withEnv("JAVA_OPTS", "-Xms256m -Xmx256m")
                       .withEnv("scheduler.content.age.millis", "86400000")
                       .withEnv("scheduler.cleanup.interval", "86400000")
                       .withExposedPorts(8099)
                       .waitingFor(Wait.forListeningPort())
                       .withStartupTimeout(Duration.ofMinutes(2));
    }

    private GenericContainer createTransformCoreContainer(Properties env)
    {
        return new GenericContainer("alfresco/alfresco-transform-core-aio:" + env.getProperty("TRANSFORMERS_TAG"))
                       .withNetwork(network)
                       .withNetworkAliases("transform-core-aio")
                       .withEnv("JAVA_OPTS", "-Xms512m -Xmx512m")
                       .withEnv("ACTIVEMQ_URL", "nio://activemq:61616")
                       .withEnv("FILE_STORE_URL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file")
                       .withExposedPorts(8090)
                       .waitingFor(Wait.forListeningPort())
                       .withStartupTimeout(Duration.ofMinutes(2));
    }

    private GenericContainer createTransformRouterContainer(Properties env)
    {
        return new GenericContainer("quay.io/alfresco/alfresco-transform-router:" + env.getProperty("TRANSFORM_ROUTER_TAG"))
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
        return new GenericContainer("alfresco/alfresco-content-repository:latest")
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
                                "-Xms1500m -Xmx1500m ")
                       .withNetwork(network)
                       .withNetworkAliases("alfresco")
                       .waitingFor(new LogMessageWaitStrategy().withRegEx(".*Server startup in.*\\n"))
                       .withStartupTimeout(Duration.ofMinutes(7))
                       .withExposedPorts(8080);
    }
}
