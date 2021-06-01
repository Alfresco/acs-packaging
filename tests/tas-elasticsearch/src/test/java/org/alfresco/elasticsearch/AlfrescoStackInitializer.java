package org.alfresco.elasticsearch;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.lifecycle.Startables;
import org.testng.Assert;

import java.io.FileReader;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class AlfrescoStackInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>
{

    public static Network network;

    public static GenericContainer alfresco;

    public static ElasticsearchContainer elasticsearch;

    public static  GenericContainer metadataLiveIndexer;

    public static  GenericContainer contentLiveIndexer;

    public static  GenericContainer mediationLiveIndexer;


    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext)
    {

        Properties env = loadEnvProperties();

        network = Network.newNetwork();

        alfresco = new GenericContainer("alfresco/alfresco-content-repository:latest")
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
                                    "-Delasticsearch.indexName=custom-alfresco-index " +
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
                           .waitingFor(new LogMessageWaitStrategy()
                                               .withRegEx(".*Server startup in.*\\n")
                                               .withStartupTimeout(Duration.ofSeconds(400)))
                           .withExposedPorts(8080);

        GenericContainer transformRouter = new GenericContainer("quay.io/alfresco/alfresco-transform-router:" + env.getProperty("TRANSFORM_ROUTER_TAG"))
                                                   .withNetwork(network)
                                                   .withNetworkAliases("transform-router")
                                                   .withEnv("JAVA_OPTS", "-Xms256m -Xmx512m")
                                                   .withEnv("ACTIVEMQ_URL", "nio://activemq:61616")
                                                   .withEnv("CORE_AIO_URL", "http://transform-core-aio:8090")
                                                   .withEnv("FILE_STORE_URL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file");

        GenericContainer transformCore = new GenericContainer("alfresco/alfresco-transform-core-aio:" + env.getProperty("TRANSFORMERS_TAG"))
                                                 .withNetwork(network)
                                                 .withNetworkAliases("transform-core-aio")
                                                 .withEnv("JAVA_OPTS", "-Xms256m -Xmx1536m")
                                                 .withEnv("ACTIVEMQ_URL", "nio://activemq:61616")
                                                 .withEnv("FILE_STORE_URL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file");

        GenericContainer sfs = new GenericContainer("alfresco/alfresco-shared-file-store:" + env.getProperty("SFS_TAG"))
                                       .withNetwork(network)
                                       .withNetworkAliases("shared-file-store")
                                       .withEnv("JAVA_OPTS", "-Xms256m -Xmx512m")
                                       .withEnv("scheduler.content.age.millis", "86400000")
                                       .withEnv("scheduler.cleanup.interval", "86400000");

        PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer("postgres:" + env.getProperty("POSTGRES_TAG"))
                                                                     .withPassword("alfresco")
                                                                     .withUsername("alfresco")
                                                                     .withDatabaseName("alfresco")
                                                                     .withNetwork(network)
                                                                     .withNetworkAliases("postgres");

        GenericContainer activemq = new GenericContainer("alfresco/alfresco-activemq:5.15.8")
                                            .withNetwork(network)
                                            .withNetworkAliases("activemq")
                                            .waitingFor(Wait.forListeningPort())
                                            .withExposedPorts(61616, 8161, 5672, 61613);

        elasticsearch = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:" + env.getProperty("ES_TAG"))
                                .withNetwork(network)
                                .withNetworkAliases("elasticsearch")
                                .withExposedPorts(9200)
                                .withEnv("xpack.security.enabled", "false")
                                .withEnv("discovery.type", "single-node");

        metadataLiveIndexer = new GenericContainer("quay.io/alfresco/alfresco-elasticsearch-live-indexing-metadata:" + env.getProperty("ES_CONNECTOR_TAG"))
                              .withNetwork(network)
                              .withNetworkAliases("metadata-indexing")
                              .withEnv("JAVA_OPTS", "-Xms256m -Xmx256m")
                              .withEnv("ELASTICSEARCH_INDEXNAME", "custom-alfresco-index")
                              .withEnv("SPRING_ELASTICSEARCH_REST_URIS", "http://elasticsearch:9200")
                              .withEnv("SPRING_ACTIVEMQ_BROKERURL", "nio://activemq:61616");

        contentLiveIndexer = new GenericContainer("quay.io/alfresco/alfresco-elasticsearch-live-indexing-content:" + env.getProperty("ES_CONNECTOR_TAG"))
                .withNetwork(network)
                .withNetworkAliases("content-indexing")
                .withEnv("JAVA_OPTS", "-Xms256m -Xmx256m")
                .withEnv("ELASTICSEARCH_INDEXNAME", "custom-alfresco-index")
                .withEnv("SPRING_ELASTICSEARCH_REST_URIS", "http://elasticsearch:9200")
                .withEnv("SPRING_ACTIVEMQ_BROKERURL", "nio://activemq:61616")
                .withEnv("ALFRESCO_SHAREDFILESTORE_BASEURL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file/");

        mediationLiveIndexer = new GenericContainer("quay.io/alfresco/alfresco-elasticsearch-live-indexing-mediation:" + env.getProperty("ES_CONNECTOR_TAG"))
                .withNetwork(network)
                .withNetworkAliases("mediation")
                .withEnv("JAVA_OPTS", "-Xms256m -Xmx256m")
                .withEnv("SPRING_ACTIVEMQ_BROKERURL", "nio://activemq:61616");

        CompletableFuture<Void> start = Startables.deepStart(elasticsearch, postgres, alfresco, activemq, transformCore, transformRouter, metadataLiveIndexer, contentLiveIndexer, mediationLiveIndexer, sfs);

        try
        {
            start.get();
        } catch (Exception e)
        {
            Assert.fail("unable to start containers");
        }

        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(configurableApplicationContext,
                                                                  "alfresco.server=" + alfresco.getContainerIpAddress(),
                                                                  "alfresco.port=" + alfresco.getFirstMappedPort());

    }

    private Properties loadEnvProperties()
    {
        Properties env = new Properties();
        try (FileReader reader = new FileReader("../environment/.env"))
        {
            env.load(reader);
        } catch (Exception e)
        {
            Assert.fail("unable to load .env property file ");
        }
        return env;
    }
}
