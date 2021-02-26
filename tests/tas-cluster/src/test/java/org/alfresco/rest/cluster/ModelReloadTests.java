package org.alfresco.rest.cluster;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;

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
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

public class ModelReloadTests extends ClusterTest
{
    private static final String CM_MODEL_ACTIVE = "cm:modelActive";
    private static final String ACTIVE = "ACTIVE";
    private static final String ROOT = "-root-";
    private static final String INCLUDE_PROPERTIES = "include=properties";

    /**
     *  The test can be run only once. Consecutive runs require cleaning environment due to nature of content modelling
     */
    @TestRail(section = { TestGroup.REST_API,TestGroup.CLUSTER }, executionType = ExecutionType.SANITY,
            description = "Verify new data model can be activated on both nodes of the cluster")
    @Test(groups = { TestGroup.REST_API, TestGroup.CLUSTER, TestGroup.SANITY})
    public void testUploadActivateModel() throws Exception
    {
        restClientServer1.authenticateUser(dataContent.getAdminUser());

        RestNodeBodyModel modelNode = new RestNodeBodyModel();
        modelNode.setName("testModel.xml");
        modelNode.setNodeType("cm:content");

        ContentModel root = new ContentModel();
        root.setName(ROOT);
        root.setNodeRef(ROOT);

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
                .usingParams(INCLUDE_PROPERTIES).getNode();

        restClientServer1.assertStatusCodeIs(HttpStatus.OK);
        assertNotNull(restNodeModel.getProperties());
        assertFalse("The model should not be activated after upload.", (Boolean) ((Map) restNodeModel.getProperties()).get(CM_MODEL_ACTIVE));

        //"{\n" +
        //"  \"properties\":\n" +
        //"  {\n" +
        //"    \"cm:modelActive\":\"true\"\n" +
        //"  }\n" +
        //"}"
        JsonObject activateModelJson = Json.createObjectBuilder().add("properties",
                Json.createObjectBuilder().add(CM_MODEL_ACTIVE, true))
                .build();

        // Activate the model
        restClientServer1.withCoreAPI().usingNode(fileModel).updateNode(activateModelJson.toString());
        restClientServer1.assertStatusCodeIs(HttpStatus.OK);

        restNodeModel = restClientServer1.withCoreAPI()
                .usingNode(fileModel)
                .usingParams(INCLUDE_PROPERTIES).getNode();

        // Check the activation flag on the first node
        restClientServer1.assertStatusCodeIs(HttpStatus.OK);
        assertNotNull(restNodeModel.getProperties());
        assertTrue("The model should be activated on node 1 now.", (Boolean) ((Map) restNodeModel.getProperties()).get(CM_MODEL_ACTIVE));

        CustomContentModel customContentModel = new CustomContentModel();
        customContentModel.setName("test-model.xml");

        // Check CMM if the model is actually deployed on the first node
        RestCustomModel restCustomModel = restClientServer1.withPrivateAPI().usingCustomModel(customContentModel).getModel();
        assertEquals("The model was not deployed on node 1.", String.valueOf(HttpStatus.OK.value()), restClientServer1.getStatusCode());
        assertNotNull(restCustomModel.getStatus());
        assertEquals("The model should be published on node 1 now.", ACTIVE, restCustomModel.getStatus());

        // Check the activation flag on the second node
        restClientServer2.authenticateUser(dataContent.getAdminUser());
        restNodeModel = restClientServer2.withCoreAPI()
                .usingNode(fileModel)
                .usingParams(INCLUDE_PROPERTIES).getNode();
        restClientServer2.assertStatusCodeIs(HttpStatus.OK);
        assertNotNull(restNodeModel.getProperties());
        assertTrue("The model was not activated on node 2.", (Boolean) ((Map) restNodeModel.getProperties()).get(CM_MODEL_ACTIVE));

        // Check CMM if the model is actually deployed on the second node
        restCustomModel = restClientServer2.withPrivateAPI().usingCustomModel(customContentModel).getModel();
        assertEquals("The model was not deployed on node 2.", String.valueOf(HttpStatus.OK.value()), restClientServer2.getStatusCode());
        assertNotNull(restCustomModel.getStatus());
        assertEquals("The model should be published on node 2 now.", ACTIVE, restCustomModel.getStatus());

        // Check that the new types defined in the model are visible through CMIS on the second node in the cluster
        cmisApiServer2.authenticateUser(dataContent.getAdminUser());

        cmisApiServer2.usingObjectType(BaseTypeId.CMIS_DOCUMENT.value())
                .withPropertyDefinitions()
                .hasChildren("D:mf:test").propertyDefinitionIsNotEmpty();
    }

    /**
     *  The test can be run only once. Consecutive runs require cleaning environment due to nature of content modelling
     */
    @TestRail(section = { TestGroup.REST_API,TestGroup.CLUSTER }, executionType = ExecutionType.SANITY,
            description = "Verify syncing a huge data model in the cluster")
    @Test(groups = { TestGroup.REST_API, TestGroup.CLUSTER, TestGroup.SANITY})
    public void testUploadActivateHugeModel() throws Exception
    {
        restClientServer1.authenticateUser(dataContent.getAdminUser());

        RestNodeBodyModel modelNode = new RestNodeBodyModel();
        modelNode.setName("hugeDataModel.xml");
        modelNode.setNodeType("cm:content");

        ContentModel root = new ContentModel();
        root.setName(ROOT);
        root.setNodeRef(ROOT);

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
                .usingParams(INCLUDE_PROPERTIES).getNode();

        restClientServer1.assertStatusCodeIs(HttpStatus.OK);
        assertNotNull(restNodeModel.getProperties());
        assertFalse("The model should not be activated after upload.", (Boolean) ((Map) restNodeModel.getProperties()).get(CM_MODEL_ACTIVE));

        //"{\n" +
        //"  \"properties\":\n" +
        //"  {\n" +
        //"    \"cm:modelActive\":\"true\"\n" +
        //"  }\n" +
        //"}"
        JsonObject activateModelJson = Json.createObjectBuilder().add("properties",
                Json.createObjectBuilder().add(CM_MODEL_ACTIVE, true))
                .build();

        // Activate the model
        restClientServer1.withCoreAPI().usingNode(fileModel).updateNode(activateModelJson.toString());
        restClientServer1.assertStatusCodeIs(HttpStatus.OK);

        restNodeModel = restClientServer1.withCoreAPI()
                .usingNode(fileModel)
                .usingParams(INCLUDE_PROPERTIES).getNode();

        // Check the activation flag on the first node
        restClientServer1.assertStatusCodeIs(HttpStatus.OK);
        assertNotNull(restNodeModel.getProperties());
        assertTrue("The model should be activated on node 1 now.", (Boolean) ((Map) restNodeModel.getProperties()).get(CM_MODEL_ACTIVE));

        CustomContentModel customContentModel = new CustomContentModel();
        customContentModel.setName("hugeDataModel.xml");

        // Check CMM if the model is actually deployed on the first node
        RestCustomModel restCustomModel = restClientServer1.withPrivateAPI().usingCustomModel(customContentModel).getModel();
        assertEquals("The model was not deployed on node 1.", String.valueOf(HttpStatus.OK.value()), restClientServer1.getStatusCode());
        assertNotNull(restCustomModel.getStatus());
        assertEquals("The model should be published on node 1 now.", ACTIVE, restCustomModel.getStatus());

        // Check the activation flag on the second node
        restClientServer2.authenticateUser(dataContent.getAdminUser());
        restNodeModel = restClientServer2.withCoreAPI()
                .usingNode(fileModel)
                .usingParams(INCLUDE_PROPERTIES).getNode();
        restClientServer2.assertStatusCodeIs(HttpStatus.OK);
        assertNotNull(restNodeModel.getProperties());
        assertTrue("The model was not activated on node 2.", (Boolean) ((Map) restNodeModel.getProperties()).get(CM_MODEL_ACTIVE));

        // Check CMM if the model is actually deployed on the second node
        restCustomModel = restClientServer2.withPrivateAPI().usingCustomModel(customContentModel).getModel();
        assertEquals("The model was not deployed on node 2.", String.valueOf(HttpStatus.OK.value()), restClientServer2.getStatusCode());
        assertNotNull(restCustomModel.getStatus());
        assertEquals("The model should be published on node 2 now.", ACTIVE, restCustomModel.getStatus());

        // Check that the new types defined in the model are visible through CMIS on the second node in the cluster
        cmisApiServer2.authenticateUser(dataContent.getAdminUser());

        cmisApiServer2.usingObjectType(BaseTypeId.CMIS_DOCUMENT.value())
                .withPropertyDefinitions()
                .hasChildren("D:hdm:hdmtype56").propertyDefinitionIsNotEmpty();
    }


    /**
     *  The test can be run only once. Consecutive runs require cleaning environment due to nature of content modelling
     */
    @TestRail(section = { TestGroup.REST_API,TestGroup.CLUSTER }, executionType = ExecutionType.SANITY,
            description = "Verify a data model is updated on both nodes of the cluster")
    @Test(groups = { TestGroup.REST_API, TestGroup.CLUSTER, TestGroup.SANITY})
    public void testUploadActivateUpdateModel() throws Exception
    {
        String modelXMLFileName = "test-model-simple.xml";
        restClientServer1.authenticateUser(dataContent.getAdminUser());

        RestNodeBodyModel modelNode = new RestNodeBodyModel();
        modelNode.setName("testModelSimple.xml");
        modelNode.setNodeType("cm:content");

        ContentModel root = new ContentModel();
        root.setName(ROOT);
        root.setNodeRef(ROOT);

        RestNodeModel modelsFolder = restClientServer1.withCoreAPI().usingNode(root).usingParams("relativePath=Data Dictionary/Models").getNode();

        FileModel fileModel = new FileModel();
        fileModel.setNodeRef(modelsFolder.getId());

        restClientServer1.configureRequestSpec().addMultiPart("filedata", Utility.getResourceTestDataFile(modelXMLFileName));
        restClientServer1.withCoreAPI().usingNode(fileModel).createNode(modelNode);
        restClientServer1.assertStatusCodeIs(HttpStatus.CREATED);

        RestNodeModel customModel = restClientServer1.withCoreAPI().usingNode(root).usingParams("relativePath=Data Dictionary/Models/" + modelXMLFileName).getNode();
        restClientServer1.assertStatusCodeIs(HttpStatus.OK);

        fileModel = new FileModel();
        fileModel.setNodeRef(customModel.getId());

        RestNodeModel restNodeModel = restClientServer1.withCoreAPI()
                .usingNode(fileModel)
                .usingParams(INCLUDE_PROPERTIES).getNode();

        restClientServer1.assertStatusCodeIs(HttpStatus.OK);
        assertNotNull(restNodeModel.getProperties());
        assertFalse("The model should not be activated after upload.", (Boolean) ((Map) restNodeModel.getProperties()).get(CM_MODEL_ACTIVE));

        //"{\n" +
        //"  \"properties\":\n" +
        //"  {\n" +
        //"    \"cm:modelActive\":\"true\"\n" +
        //"  }\n" +
        //"}"
        JsonObject activateModelJson = Json.createObjectBuilder().add("properties",
                Json.createObjectBuilder().add(CM_MODEL_ACTIVE, true))
                .build();

        // Activate the model
        restClientServer1.withCoreAPI().usingNode(fileModel).updateNode(activateModelJson.toString());
        restClientServer1.assertStatusCodeIs(HttpStatus.OK);

        restNodeModel = restClientServer1.withCoreAPI()
                .usingNode(fileModel)
                .usingParams(INCLUDE_PROPERTIES).getNode();

        // Check the activation flag on the first node
        restClientServer1.assertStatusCodeIs(HttpStatus.OK);
        assertNotNull(restNodeModel.getProperties());
        assertTrue("The model should be activated on node 1 now.", (Boolean) ((Map) restNodeModel.getProperties()).get(CM_MODEL_ACTIVE));

        CustomContentModel customContentModel = new CustomContentModel();
        customContentModel.setName(modelXMLFileName);

        // Check CMM if the model is actually deployed on the first node
        RestCustomModel restCustomModel = restClientServer1.withPrivateAPI().usingCustomModel(customContentModel).getModel();
        assertEquals("The model was not deployed on node 1.", String.valueOf(HttpStatus.OK.value()), restClientServer1.getStatusCode());
        assertNotNull(restCustomModel.getStatus());
        assertEquals("The model should be published on node 1 now.", ACTIVE, restCustomModel.getStatus());


        // Check the activation flag on the second node
        restClientServer2.authenticateUser(dataContent.getAdminUser());
        restNodeModel = restClientServer2.withCoreAPI()
                .usingNode(fileModel)
                .usingParams(INCLUDE_PROPERTIES).getNode();
        restClientServer2.assertStatusCodeIs(HttpStatus.OK);
        assertNotNull(restNodeModel.getProperties());
        assertTrue("The model was not activated on node 2.", (Boolean) ((Map) restNodeModel.getProperties()).get(CM_MODEL_ACTIVE));

        // Check CMM if the model is actually deployed on the second node
        restCustomModel = restClientServer2.withPrivateAPI().usingCustomModel(customContentModel).getModel();
        assertEquals("The model was not deployed on node 2.", String.valueOf(HttpStatus.OK.value()), restClientServer2.getStatusCode());
        assertNotNull(restCustomModel.getStatus());

        assertEquals("The model should be published on node 2 now.", ACTIVE, restCustomModel.getStatus());

        String testTypeName = "D:ms:test";
        // Check that the new types defined in the model are visible through CMIS on the se
        // cond node in the cluster
        cmisApiServer2.authenticateUser(dataContent.getAdminUser());

        cmisApiServer2.usingObjectType(BaseTypeId.CMIS_DOCUMENT.value())
                .withPropertyDefinitions()
                .hasChildren(testTypeName).propertyDefinitionIsNotEmpty();

        assertTrue(typeHasProperty(testTypeName, "ms:freetext_underscore"));
        assertFalse(typeHasProperty(testTypeName, "ms:prop-new"));

        // Update model content on first node, a new property is added - ms:prop-new
        File updatedContent = Utility.getResourceTestDataFile("test-model-simple_updated.xml");
        restNodeModel = restClientServer1.withCoreAPI()
                .usingNode(fileModel).updateNodeContent(updatedContent);
        restClientServer1.assertStatusCodeIs(HttpStatus.OK);

        // Check the new model type property is visible on second node 
        assertTrue("The model was not updated on node 2.", typeHasProperty(testTypeName, "ms:prop-new"));
    }

    private boolean typeHasProperty(String objectTypeID, String propertyName)
    {
        ItemIterable<ObjectType> values = cmisApiServer2.withCMISUtil().getTypeChildren(BaseTypeId.CMIS_DOCUMENT.value(), true);
        for (Iterator<ObjectType> iterator = values.iterator(); iterator.hasNext(); )
        {
            ObjectType type = (ObjectType) iterator.next();
            if (type.getId().equals(objectTypeID))
            {
                return type.getPropertyDefinitions().containsKey(propertyName);
            }
        }
        return false;
    }
}
