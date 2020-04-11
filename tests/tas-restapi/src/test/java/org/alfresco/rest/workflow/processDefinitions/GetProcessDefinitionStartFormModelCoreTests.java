package org.alfresco.rest.workflow.processDefinitions;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessDefinitionModel;
import org.alfresco.rest.model.RestProcessDefinitionModelsCollection;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 12/6/2016.
 */
public class GetProcessDefinitionStartFormModelCoreTests extends RestTest
{
    private UserModel adminUser;
    private RestProcessDefinitionModel randomProcessDefinition;
    private RestProcessDefinitionModelsCollection allProcessDefinitions;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        allProcessDefinitions = restClient.authenticateUser(adminUser).withWorkflowAPI().getAllProcessDefinitions();
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify any user gets a model of the start form type definition for non-network deployments using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.REGRESSION })
    public void nonNetworkUserGetsStartFormModel() throws Exception
    {
        UserModel nonNetworkUser = dataUser.createRandomTestUser();
        randomProcessDefinition = allProcessDefinitions.getOneRandomEntry().onModel();
        restClient.authenticateUser(nonNetworkUser).withWorkflowAPI()
                .usingProcessDefinitions(randomProcessDefinition).getProcessDefinitionStartFormModel()
                .assertThat().entriesListIsNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}
