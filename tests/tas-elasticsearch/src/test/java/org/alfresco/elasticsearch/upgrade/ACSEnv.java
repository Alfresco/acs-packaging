package org.alfresco.elasticsearch.upgrade;

import static java.time.Duration.ofMinutes;

import static org.alfresco.elasticsearch.upgrade.Utils.waitFor;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

class ACSEnv extends BaseACSEnv
{
    private final GenericContainer<?> postgres;
    private final GenericContainer<?> alfresco;

    public ACSEnv(Config cfg, final Network network, final String indexSubsystemName)
    {
        super(cfg);

        postgres = createPostgresContainer(network);

        createActiveMqContainer(network);
        createSharedFileStoreContainer(network);
        createTransformCoreAIOContainer(network);
        createTransformRouterContainer(network);

        alfresco = createRepositoryContainer(network, indexSubsystemName);
    }

    public ACSEnv(GenericContainer<?> postgres, Config cfg, String indexSubsystemName)
    {
        super(cfg);
        Network network = postgres.getNetwork();

        this.postgres = postgres;
        registerCreatedContainer(postgres);

        createActiveMqContainer(network);
        createSharedFileStoreContainer(network);
        createTransformCoreAIOContainer(network);
        createTransformRouterContainer(network);

        alfresco = createRepositoryContainer(network, indexSubsystemName);
    }

    @Override
    public GenericContainer<?> getAlfresco()
    {
        return alfresco;
    }

    @Override
    public GenericContainer<?> getPostgres()
    {
        return postgres;
    }

    public void reindexByIds(long fromId, long toId)
    {
        final GenericContainer<?> reIndexing = createReIndexingContainer(fromId, toId);

        reIndexing.start();
        waitFor("Re-indexing Startup", ofMinutes(1), reIndexing::isRunning);
        waitFor("Re-indexing Exit", ofMinutes(5), () -> !reIndexing.isRunning());
    }

    public void startLiveIndexing()
    {
        final GenericContainer<?> liveIndexing = createLiveIndexingContainer();
        liveIndexing.start();
    }

    private GenericContainer<?> createRepositoryContainer(Network network, String indexSubsystemName)
    {
        return newContainer(GenericContainer.class, cfg.getRepositoryImage())
                .withEnv("JAVA_TOOL_OPTIONS",
                        "-Dencryption.keystore.type=JCEKS " +
                                "-Dencryption.cipherAlgorithm=DESede/CBC/PKCS5Padding " +
                                "-Dencryption.keyAlgorithm=DESede " +
                                "-Dencryption.keystore.location=/usr/local/tomcat/shared/classes/alfresco/extension/keystore/keystore " +
                                "-Dmetadata-keystore.password=mp6yc0UD9e -Dmetadata-keystore.aliases=metadata " +
                                "-Dmetadata-keystore.metadata.password=oKIWzVdEdA -Dmetadata-keystore.metadata.algorithm=DESede")
                .withEnv("JAVA_OPTS",
                        "-Ddb.driver=org.postgresql.Driver " +
                                "-Ddb.username=alfresco " +
                                "-Ddb.password=alfresco " +
                                "-Ddb.url=jdbc:postgresql://postgres:5432/alfresco " +
                                "-Dsolr.host=solr6 " +
                                "-Dsolr.port=8983 " +
                                "-Dsolr.secureComms=secret " +
                                "-Dsolr.sharedSecret=secret " +
                                "-Dsolr.base.url=/solr " +
                                "-Delasticsearch.createIndexIfNotExists=true " +
                                "-Delasticsearch.host=" + cfg.getElasticsearchHostname() + " " +
                                "-Delasticsearch.indexName=" + cfg.getIndexName() + " " +
                                "-Dindex.subsystem.name=" + indexSubsystemName + " " +
                                "-Dalfresco.host=localhost " +
                                "-Dalfresco.port=8080 " +
                                "-Dmessaging.broker.url=\"failover:(nio://activemq:61616)?timeout=3000&jms.useCompression=true\" " +
                                "-Dmessaging.broker.username=admin" +
                                "-Dmessaging.broker.password=admin" +
                                "-Ddeployment.method=DOCKER_COMPOSE " +
                                "-Dtransform.service.enabled=true " +
                                "-Dtransform.service.url=http://transform-router:8095 " +
                                "-Dsfs.url=http://shared-file-store:8099/ " +
                                "-DlocalTransform.core-aio.url=http://transform-core-aio:8090/ " +
                                "-Xmx2g -XshowSettings:vm")
                .withNetwork(network)
                .withNetworkAliases("alfresco")
                .withExposedPorts(8080);
    }

    private void createTransformRouterContainer(Network network)
    {
        newContainer(GenericContainer.class, cfg.getTransformRouterImage())
                .withEnv("JAVA_OPTS", " -Xmx512m -XshowSettings:vm")
                .withEnv("ACTIVEMQ_URL", "nio://activemq:61616")
                .withEnv("SPRING_ACTIVEMQ_USER", "admin")
                .withEnv("SPRING_ACTIVEMQ_PASSWORD", "admin")
                .withEnv("CORE_AIO_URL", "http://transform-core-aio:8090")
                .withEnv("FILE_STORE_URL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file")
                .withNetwork(network)
                .withNetworkAliases("transform-router")
                .waitingFor(new LogMessageWaitStrategy().withRegEx(".+Starting application components.+Done.*").withStartupTimeout(ofMinutes(2)));
    }

    private void createTransformCoreAIOContainer(Network network)
    {
        newContainer(GenericContainer.class, cfg.getTransformCoreAIOImage())
                .withEnv("JAVA_OPTS", " -Xmx1g -XshowSettings:vm")
                .withEnv("ACTIVEMQ_URL", "nio://activemq:61616")
                .withEnv("SPRING_ACTIVEMQ_USER", "admin")
                .withEnv("SPRING_ACTIVEMQ_PASSWORD", "admin")
                .withEnv("FILE_STORE_URL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file")
                .withNetwork(network)
                .withNetworkAliases("transform-core-aio")
                .waitingFor(new LogMessageWaitStrategy().withRegEx(".+Starting application components.+Done.*").withStartupTimeout(ofMinutes(2)));
    }

    private void createSharedFileStoreContainer(Network network)
    {
        newContainer(GenericContainer.class, cfg.getSharedFileStoreImage())
                .withEnv("JAVA_OPTS", " -Xmx512m -XshowSettings:vm")
                .withEnv("scheduler.content.age.millis", "86400000")
                .withEnv("scheduler.cleanup.interval", "86400000")
                .withEnv("_JAVA_OPTIONS", "-Dlogging.level.org.alfresco=DEBUG")
                .withNetwork(network)
                .withNetworkAliases("shared-file-store");
    }

    private void createActiveMqContainer(Network network)
    {
        newContainer(GenericContainer.class, cfg.getActiveMqImage())
                .withNetwork(network)
                .withNetworkAliases("activemq");
    }

    private GenericContainer<?> createPostgresContainer(Network network)
    {
        return newContainer(PostgreSQLContainer.class, cfg.getPostgreSQLImage())
                .withPassword("alfresco")
                .withUsername("alfresco")
                .withDatabaseName("alfresco")
                .withNetwork(network)
                .withNetworkAliases("postgres");
    }

    private GenericContainer<?> createReIndexingContainer(long fromId, long toId)
    {
        return newContainer(GenericContainer.class, cfg.getReIndexingImage())
                .withEnv("ELASTICSEARCH_INDEXNAME", cfg.getIndexName())
                .withEnv("SPRING_ELASTICSEARCH_REST_URIS", "http://" + cfg.getElasticsearchHostname() + ":9200")
                .withEnv("SPRING_ACTIVEMQ_BROKERURL", "nio://activemq:61616")
                .withEnv("SPRING_ACTIVEMQ_USER", "admin")
                .withEnv("SPRING_ACTIVEMQ_PASSWORD", "admin")
                .withEnv("ALFRESCO_SHAREDFILESTORE_BASEURL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file/")
                .withEnv("ALFRESCO_ACCEPTEDCONTENTMEDIATYPESCACHE_BASEURL", "http://transform-core-aio:8090/transform/config")
                .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/alfresco")
                .withEnv("ALFRESCO_REINDEX_PATHINDEXINGENABLED", "true")
                .withEnv("ALFRESCO_REINDEX_FROM_ID", Long.toString(fromId))
                .withEnv("ALFRESCO_REINDEX_TO_ID", Long.toString(toId))
                .withEnv("ALFRESCO_REINDEX_JOB_NAME", "reindexByIds")
                .withNetwork(alfresco.getNetwork());
    }

    private GenericContainer<?> createLiveIndexingContainer()
    {
        return newContainer(GenericContainer.class, cfg.getLiveIndexingImage())
                .withEnv("ELASTICSEARCH_INDEXNAME", cfg.getIndexName())
                .withEnv("SPRING_ELASTICSEARCH_REST_URIS", "http://" + cfg.getElasticsearchHostname() + ":9200")
                .withEnv("SPRING_ACTIVEMQ_BROKERURL", "nio://activemq:61616")
                .withEnv("SPRING_ACTIVEMQ_USER", "admin")
                .withEnv("SPRING_ACTIVEMQ_PASSWORD", "admin")
                .withEnv("ALFRESCO_SHAREDFILESTORE_BASEURL", "http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file/")
                .withEnv("ALFRESCO_ACCEPTEDCONTENTMEDIATYPESCACHE_BASEURL", "http://transform-core-aio:8090/transform/config")
                .withEnv("_JAVA_OPTIONS", "-Dlogging.level.org.alfresco=DEBUG")
                .withNetwork(alfresco.getNetwork())
                .waitingFor(new LogMessageWaitStrategy().withRegEx(".+Started LiveIndexingApp.+").withStartupTimeout(ofMinutes(1)));
    }
}
