package org.alfresco.rest.cluster;

import org.alfresco.rest.ClusterTest;
import org.alfresco.rest.model.RestCustomModel;
import org.alfresco.rest.model.RestNodeBodyModel;
import org.alfresco.rest.model.RestNodeModel;
import org.alfresco.utility.Utility;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.CustomContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

public class ModelReloadTests extends ClusterTest
{

    /**
     *  The test can be run only once. Consecutive runs require cleaning environment due to nature of content modelling
     */
    @TestRail(section = { TestGroup.REST_API,TestGroup.CLUSTER }, executionType = ExecutionType.SANITY,
            description = "Verify new data model can be activated on both nodes of the cluster")
    @Test(groups = { TestGroup.REST_API, TestGroup.CLUSTER, TestGroup.SANITY})
    public void testUploadActivateModel() throws Exception
    {
        restClientServer1.configureServerEndpoint();
        restClientServer1.authenticateUser(dataContent.getAdminUser());

        RestNodeBodyModel modelNode = new RestNodeBodyModel();
        modelNode.setName("testModel.xml");
        modelNode.setNodeType("cm:content");

        ContentModel root = new ContentModel();
        root.setName("-root-");
        root.setNodeRef("-root-");

        RestNodeModel modelsFolder = restClientServer1.withCoreAPI().usingNode(root).usingParams("relativePath=Data Dictionary/Models").getNode();

        FileModel fileModel = new FileModel();
        fileModel.setNodeRef(modelsFolder.getId());

        restClientServer1.configureRequestSpec().addMultiPart("filedata", Utility.getResourceTestDataFile("test-model.xml"));
        restClientServer1.withCoreAPI().usingNode(fileModel).createNode(modelNode);
        restClientServer1.assertStatusCodeIs(HttpStatus.CREATED);

        RestNodeModel customModel = restClientServer1.withCoreAPI().usingNode(root).usingParams("relativePath=Data Dictionary/Models/test-model.xml").getNode();
        restClientServer1.assertStatusCodeIs(HttpStatus.OK);

        fileModel = new FileModel();
        fileModel.setNodeRef(customModel.getId());

        RestNodeModel restNodeModel = restClientServer1.withCoreAPI()
                .usingNode(fileModel)
                .usingParams("include=properties").getNode();

        restClientServer1.assertStatusCodeIs(HttpStatus.OK);
        assertNotNull(restNodeModel.getProperties());
        assertFalse("The model should not be activated after upload.", (Boolean) ((Map) restNodeModel.getProperties()).get("cm:modelActive"));

        //"{\n" +
        //"  \"properties\":\n" +
        //"  {\n" +
        //"    \"cm:modelActive\":\"true\"\n" +
        //"  }\n" +
        //"}"
        JsonObject activateModelJson = Json.createObjectBuilder().add("properties",
                Json.createObjectBuilder().add("cm:modelActive", true))
                .build();

        // Activate the model
        restClientServer1.withCoreAPI().usingNode(fileModel).updateNode(activateModelJson.toString());
        restClientServer1.assertStatusCodeIs(HttpStatus.OK);

        restNodeModel = restClientServer1.withCoreAPI()
                .usingNode(fileModel)
                .usingParams("include=properties").getNode();

        // Check the activation flag on the first node
        restClientServer1.assertStatusCodeIs(HttpStatus.OK);
        assertNotNull(restNodeModel.getProperties());
        assertTrue("The model should be activated on node1 now.", (Boolean) ((Map) restNodeModel.getProperties()).get("cm:modelActive"));

        CustomContentModel customContentModel = new CustomContentModel();
        customContentModel.setName("test-model.xml");

        // Check CMM if the model is actually deployed on the first node
        RestCustomModel restCustomModel = restClientServer1.withPrivateAPI().usingCustomModel(customContentModel).getModel();
        assertEquals("The model was not deployed on node 1.", String.valueOf(HttpStatus.OK.value()), restClientServer1.getStatusCode());
        assertNotNull(restCustomModel.getStatus());
        assertEquals("The model should be published on node1 now.", "ACTIVE", restCustomModel.getStatus());

        restClientServer2.configureServerEndpoint();
        // Check the activation flag on the second node
        restClientServer2.authenticateUser(dataContent.getAdminUser());
        restNodeModel = restClientServer2.withCoreAPI()
                .usingNode(fileModel)
                .usingParams("include=properties").getNode();
        restClientServer2.assertStatusCodeIs(HttpStatus.OK);
        assertNotNull(restNodeModel.getProperties());
        assertTrue("The model was not activated on node2.", (Boolean) ((Map) restNodeModel.getProperties()).get("cm:modelActive"));

        // Check CMM if the model is actually deployed on the second node
        restCustomModel = restClientServer2.withPrivateAPI().usingCustomModel(customContentModel).getModel();
        assertEquals("The model was not deployed on node 2.", String.valueOf(HttpStatus.OK.value()), restClientServer2.getStatusCode());
        assertNotNull(restCustomModel.getStatus());
        assertEquals("The model should be published on node2 now.", "ACTIVE", restCustomModel.getStatus());
    }

    /**
     *  The test can be run only once. Consecutive runs require cleaning environment due to nature of content modelling
     */
    @TestRail(section = { TestGroup.REST_API,TestGroup.CLUSTER }, executionType = ExecutionType.SANITY,
            description = "Verify syncing a huge datamodel in the cluster")
    @Test(groups = { TestGroup.REST_API, TestGroup.CLUSTER, TestGroup.SANITY})
    public void testUploadActivateHugeModel() throws Exception
    {
        restClientServer1.configureServerEndpoint();
        restClientServer1.authenticateUser(dataContent.getAdminUser());

        RestNodeBodyModel modelNode = new RestNodeBodyModel();
        modelNode.setName("hugeDataModel.xml");
        modelNode.setNodeType("cm:content");

        ContentModel root = new ContentModel();
        root.setName("-root-");
        root.setNodeRef("-root-");

        RestNodeModel modelsFolder = restClientServer1.withCoreAPI().usingNode(root).usingParams("relativePath=Data Dictionary/Models").getNode();

        FileModel fileModel = new FileModel();
        fileModel.setNodeRef(modelsFolder.getId());

        restClientServer1.configureRequestSpec().addMultiPart("filedata", Utility.getResourceTestDataFile("hugeDataModel.xml"));
        restClientServer1.withCoreAPI().usingNode(fileModel).createNode(modelNode);
        restClientServer1.assertStatusCodeIs(HttpStatus.CREATED);

        RestNodeModel customModel = restClientServer1.withCoreAPI().usingNode(root).usingParams("relativePath=Data Dictionary/Models/hugeDataModel.xml").getNode();
        restClientServer1.assertStatusCodeIs(HttpStatus.OK);

        fileModel = new FileModel();
        fileModel.setNodeRef(customModel.getId());

        RestNodeModel restNodeModel = restClientServer1.withCoreAPI()
                .usingNode(fileModel)
                .usingParams("include=properties").getNode();

        restClientServer1.assertStatusCodeIs(HttpStatus.OK);
        assertNotNull(restNodeModel.getProperties());
        assertFalse("The model should not be activated after upload.", (Boolean) ((Map) restNodeModel.getProperties()).get("cm:modelActive"));

        //"{\n" +
        //"  \"properties\":\n" +
        //"  {\n" +
        //"    \"cm:modelActive\":\"true\"\n" +
        //"  }\n" +
        //"}"
        JsonObject activateModelJson = Json.createObjectBuilder().add("properties",
                Json.createObjectBuilder().add("cm:modelActive", true))
                .build();

        // Activate the model
        restClientServer1.withCoreAPI().usingNode(fileModel).updateNode(activateModelJson.toString());
        restClientServer1.assertStatusCodeIs(HttpStatus.OK);

        restNodeModel = restClientServer1.withCoreAPI()
                .usingNode(fileModel)
                .usingParams("include=properties").getNode();

        // Check the activation flag on the first node
        restClientServer1.assertStatusCodeIs(HttpStatus.OK);
        assertNotNull(restNodeModel.getProperties());
        assertTrue("The model should be activated on node1 now.", (Boolean) ((Map) restNodeModel.getProperties()).get("cm:modelActive"));

        CustomContentModel customContentModel = new CustomContentModel();
        customContentModel.setName("hugeDataModel.xml");

        // Check CMM if the model is actually deployed on the first node
        RestCustomModel restCustomModel = restClientServer1.withPrivateAPI().usingCustomModel(customContentModel).getModel();
        assertEquals("The model was not deployed on node 1.", String.valueOf(HttpStatus.OK.value()), restClientServer1.getStatusCode());
        assertNotNull(restCustomModel.getStatus());
        assertEquals("The model should be published on node1 now.", "ACTIVE", restCustomModel.getStatus());

        restClientServer2.configureServerEndpoint();
        // Check the activation flag on the second node
        restClientServer2.authenticateUser(dataContent.getAdminUser());
        restNodeModel = restClientServer2.withCoreAPI()
                .usingNode(fileModel)
                .usingParams("include=properties").getNode();
        restClientServer2.assertStatusCodeIs(HttpStatus.OK);
        assertNotNull(restNodeModel.getProperties());
        assertTrue("The model was not activated on node2.", (Boolean) ((Map) restNodeModel.getProperties()).get("cm:modelActive"));

        // Check CMM if the model is actually deployed on the second node
        restCustomModel = restClientServer2.withPrivateAPI().usingCustomModel(customContentModel).getModel();
        assertEquals("The model was not deployed on node 2.", String.valueOf(HttpStatus.OK.value()), restClientServer2.getStatusCode());
        assertNotNull(restCustomModel.getStatus());
        assertEquals("The model should be published on node2 now.", "ACTIVE", restCustomModel.getStatus());
    }

}
