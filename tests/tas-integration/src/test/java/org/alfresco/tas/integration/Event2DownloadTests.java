package org.alfresco.tas.integration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.jms.ConnectionFactory;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.repo.event.databind.ObjectMapperFactory;
import org.alfresco.repo.event.v1.model.EventData;
import org.alfresco.repo.event.v1.model.NodeResource;
import org.alfresco.repo.event.v1.model.RepoEvent;
import org.alfresco.rest.RestTest;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.DataFormat;
import org.springframework.http.HttpStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Iulian Aftene
 */
public class Event2DownloadTests extends RestTest
{
    private UserModel adminUser;
    private FileModel document;
    private SiteModel siteModel;

    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String TOPIC_NAME = "alfresco.repo.event2";
    private static final String CAMEL_ROUTE = "jms:topic:" + TOPIC_NAME;
    private static final DataFormat DATA_FORMAT = new JacksonDataFormat(
        ObjectMapperFactory.createInstance(), RepoEvent.class);
    private static final RepoEventContainer EVENT_CONTAINER = new RepoEventContainer();
    private static final CamelContext CAMEL_CONTEXT = new DefaultCamelContext();

    private static boolean isCamelConfigured;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        isCamelConfigured = false;
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUser).createPrivateRandomSite();
    }

    @BeforeMethod
    public void setUp() throws Exception
    {
        if (!isCamelConfigured)
        {
            configRoute();
            isCamelConfigured = true;
        }
    }

    @AfterMethod
    public void tearDown()
    {
        EVENT_CONTAINER.reset();
    }

    @AfterClass
    public static void afterAll() throws Exception
    {
        CAMEL_CONTEXT.stop();
    }

    @Test(groups = { TestGroup.INTEGRATION, TestGroup.CORE })
    @TestRail(section = { TestGroup.INTEGRATION,
        TestGroup.CONTENT }, executionType = ExecutionType.SANITY, description = "Create, catch and validate event2 download for a file")
    public void testDownloadContent() throws Exception
    {
        document = dataContent.usingSite(siteModel)
            .usingUser(adminUser)
            .createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        document.setContent("download event test");

        //node.Created and node.Updated events should be generated
        checkNumOfEvents(2);

        restClient.authenticateUser(adminUser)
            .withCoreAPI()
            .usingNode(document)
            .getNodeContent();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        //node.Downloaded event should be generated
        checkNumOfEvents(3);

        RepoEvent<NodeResource> downloadedRepoEvent = getRepoEvent(3);
        assertEquals("Wrong repo event type.", "org.alfresco.event.node.Downloaded",
            downloadedRepoEvent.getType());
        assertEquals(EventData.JSON_SCHEMA, downloadedRepoEvent.getDataschema());
        assertNotNull("The event should not have null id", downloadedRepoEvent.getId());
        assertNotNull("The event should not have null time", downloadedRepoEvent.getTime());

        NodeResource nodeResource = downloadedRepoEvent.getData().getResource();
        assertNotNull("Resource ID is null", nodeResource.getId());
        assertNotNull("Default aspects were not added. ", nodeResource.getAspectNames());
        assertNotNull("Missing createdByUser property.", nodeResource.getCreatedByUser());
        assertNotNull("Missing createdAt property.", nodeResource.getCreatedAt());
        assertNotNull("Missing modifiedByUser property.", nodeResource.getModifiedByUser());
        assertNotNull("Missing modifiedAt property.", nodeResource.getModifiedAt());
        assertNotNull("Missing node resource properties", nodeResource.getProperties());
        assertTrue("Incorrect value for isFile field", nodeResource.isFile());
        assertFalse("Incorrect value for isFolder files", nodeResource.isFolder());
        assertNull("ResourceBefore is not null", downloadedRepoEvent.getData().getResourceBefore());
    }

    @Test(groups = { TestGroup.INTEGRATION, TestGroup.CORE })
    @TestRail(section = { TestGroup.INTEGRATION,
        TestGroup.CONTENT }, executionType = ExecutionType.SANITY, description = "Create, catch and validate event2 download for a file, twice")
    public void testDownloadContentTwice() throws Exception
    {
        document = dataContent.usingSite(siteModel)
            .usingUser(adminUser)
            .createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        document.setContent("download event test");

        //node.Created and node.Updated events should be generated
        checkNumOfEvents(2);

        restClient.authenticateUser(adminUser).withCoreAPI().usingNode(document).getNodeContent();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        RepoEventContainer repoEventsContainer = getRepoEventsContainer();
        RepoEvent<NodeResource> createRepoEvent = repoEventsContainer.getEvent(1);

        RepoEvent<NodeResource> downloadedRepoEvent = getRepoEvent(3);
        assertEquals("Wrong repo event type.", "org.alfresco.event.node.Downloaded",
            downloadedRepoEvent.getType());
        assertEquals("Downloaded event does not have the correct id",
            getNodeResource(createRepoEvent).getId(), getNodeResource(downloadedRepoEvent).getId());
        assertNull("ResourceBefore field is not null",
            downloadedRepoEvent.getData().getResourceBefore());

        restClient.authenticateUser(adminUser)
            .withCoreAPI()
            .usingNode(document)
            .getNodeContent();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        RepoEvent<NodeResource> downloadedRepoEvent2 = getRepoEvent(4);
        assertEquals("Wrong repo event type.", "org.alfresco.event.node.Downloaded",
            downloadedRepoEvent.getType());
        assertEquals("Downloaded event does not have the correct id",
            getNodeResource(createRepoEvent).getId(),
            getNodeResource(downloadedRepoEvent2).getId());
        assertNull("ResourceBefore field is not null",
            downloadedRepoEvent.getData().getResourceBefore());
    }

    @Test(groups = { TestGroup.INTEGRATION, TestGroup.CORE })
    @TestRail(section = { TestGroup.INTEGRATION,
        TestGroup.CONTENT }, executionType = ExecutionType.SANITY, description = "Create, catch and validate event2 download a file by different")
    public void testDownloadContentWithDifferentUsers() throws Exception
    {
        UserModel adminUser2 = dataUser.getAdminUser();
        
        document = dataContent.usingSite(siteModel)
            .usingUser(adminUser)
            .createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        document.setContent("download event test");

        //node.Created and node.Updated events should be generated
        checkNumOfEvents(2);

        restClient.authenticateUser(adminUser)
            .withCoreAPI()
            .usingNode(document)
            .getNodeContent();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        RepoEventContainer repoEventsContainer = getRepoEventsContainer();
        RepoEvent<NodeResource> createRepoEvent = repoEventsContainer.getEvent(1);

        RepoEvent<NodeResource> downloadedRepoEvent = getRepoEvent(3);
        assertEquals("Wrong repo event type.", "org.alfresco.event.node.Downloaded",
            downloadedRepoEvent.getType());
        assertEquals("Downloaded event does not have the correct id",
            getNodeResource(createRepoEvent).getId(), getNodeResource(downloadedRepoEvent).getId());
        assertNull("ResourceBefore field is not null",
            downloadedRepoEvent.getData().getResourceBefore());

        restClient.authenticateUser(adminUser2)
            .withCoreAPI()
            .usingNode(document)
            .getNodeContent();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        RepoEvent<NodeResource> downloadedRepoEvent2 = getRepoEvent(4);
        assertEquals("Wrong repo event type.", "org.alfresco.event.node.Downloaded",
            downloadedRepoEvent.getType());
        assertEquals("Downloaded event does not have the correct id",
            getNodeResource(createRepoEvent).getId(),
            getNodeResource(downloadedRepoEvent2).getId());
        assertNull("ResourceBefore field is not null",
            downloadedRepoEvent.getData().getResourceBefore());
    }

    protected RepoEventContainer getRepoEventsContainer()
    {
        return EVENT_CONTAINER;
    }

    protected RepoEvent<NodeResource> getRepoEvent(int eventSequenceNumber)
    {
        waitUntilNumOfEvents(eventSequenceNumber);

        RepoEventContainer eventContainer = getRepoEventsContainer();
        RepoEvent<NodeResource> event = eventContainer.getEvent(eventSequenceNumber);
        assertNotNull(event);

        return event;
    }

    protected EventData<NodeResource> getEventData(int eventSequenceNumber)
    {
        RepoEvent<NodeResource> event = getRepoEvent(eventSequenceNumber);
        EventData<NodeResource> eventData = event.getData();
        assertNotNull(eventData);

        return eventData;
    }

    protected NodeResource getNodeResource(RepoEvent<NodeResource> repoEvent)
    {
        assertNotNull(repoEvent);
        EventData<NodeResource> eventData = repoEvent.getData();
        assertNotNull(eventData);
        NodeResource resource = eventData.getResource();
        assertNotNull(resource);

        return resource;
    }

    protected void waitUntilNumOfEvents(int numOfEvents)
    {
        await().atMost(5, SECONDS).until(() -> EVENT_CONTAINER.getEvents().size() == numOfEvents);
    }

    protected void checkNumOfEvents(int expected)
    {
        try
        {
            waitUntilNumOfEvents(expected);
        }
        catch (Exception ex)
        {
            assertEquals("Wrong number of events ", expected, EVENT_CONTAINER.getEvents().size());
        }
    }

    private void configRoute() throws Exception
    {
        final ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
        CAMEL_CONTEXT
            .addComponent("jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

        CAMEL_CONTEXT.addRoutes(new RouteBuilder()
        {
            @Override
            public void configure()
            {
                from(CAMEL_ROUTE).id("RepoEvent2Test").unmarshal(DATA_FORMAT)
                    .process(EVENT_CONTAINER);
            }
        });

        CAMEL_CONTEXT.start();
    }

    public static class RepoEventContainer implements Processor
    {
        private final List<RepoEvent<NodeResource>> events = new ArrayList<>();

        @SuppressWarnings("unchecked")
        @Override
        public void process(Exchange exchange)
        {
            Object object = exchange.getIn().getBody();
            events.add((RepoEvent<NodeResource>) object);
        }

        public List<RepoEvent<NodeResource>> getEvents()
        {
            return events;
        }

        public RepoEvent<NodeResource> getEvent(int eventSequenceNumber)
        {
            int index = eventSequenceNumber - 1;
            if (index < events.size())
            {
                return events.get(index);
            }
            return null;
        }

        public void reset()
        {
            events.clear();
        }
    }

}
