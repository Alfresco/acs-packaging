package org.alfresco.rest.workflow.processes;

import javax.json.JsonObject;

import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.JsonBodyGenerator;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author Cristina Axinte
 *
 */
public class AddProcessFullTests extends RestTest
{
    private UserModel userWhoStartsProcess, assignee;
    RestProcessModel addedProcess;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        assignee = dataUser.createRandomTestUser();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user cannot start new process with invalid processDefinitionKey using REST API and status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION })
    public void userStartsNewProcessWithInvalidProcessDefinitionKey() throws JsonToModelConversionException, Exception
    { 
        userWhoStartsProcess = dataUser.createRandomTestUser();

        addedProcess = restClient.authenticateUser(userWhoStartsProcess).withWorkflowAPI().addProcess("activitiAdhocc", assignee, false, Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsErrorKey(String.format(RestErrorModel.NO_WORKFLOW_DEFINITION_FOUND, "activitiAdhocc"))
            .containsSummary(String.format(RestErrorModel.NO_WORKFLOW_DEFINITION_FOUND, "activitiAdhocc"))
            .stackTraceIs(RestErrorModel.STACKTRACE)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user cannot start new process with invalid assignee using REST API and status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION })
    public void userStartsNewProcessWithInvalidAssignee() throws JsonToModelConversionException, Exception
    { 
        userWhoStartsProcess = dataUser.createRandomTestUser();
        UserModel invalidAssignee = new UserModel("invalidAssignee", "password");

        addedProcess = restClient.authenticateUser(userWhoStartsProcess).withWorkflowAPI().addProcess("activitiAdhoc", invalidAssignee, false, Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsErrorKey(String.format(RestErrorModel.INVALID_USER_ID, invalidAssignee.getUsername()))
            .containsSummary(String.format(RestErrorModel.INVALID_USER_ID, invalidAssignee.getUsername()))
            .stackTraceIs(RestErrorModel.STACKTRACE)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }
}
