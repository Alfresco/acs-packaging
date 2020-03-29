package org.alfresco.rest.audit;

import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

@Test(groups = {TestGroup.REQUIRE_JMX})
public class PutAuditTests extends AuditTest
{

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify that the admin user can enable/disable tagging application auditing")
    public void enableDisableTaggingApplicationAuditingAsAdminUser() throws Exception
    {
        // disable tagging audit app
        taggingRestAuditAppModel = getTaggingRestAuditAppModel(adminUser);
        restClient.authenticateUser(adminUser).withCoreAPI().usingAudit().updateAuditApp(taggingRestAuditAppModel, "isEnabled",
                "false");

        // check isEnabled=false
        taggingRestAuditAppModel = getTaggingRestAuditAppModel(adminUser);
        taggingRestAuditAppModel.assertThat().field("isEnabled").is(false);
        taggingRestAuditAppModel.assertThat().field("name").is("Alfresco Tagging Service");
        taggingRestAuditAppModel.assertThat().field("id").is("tagging");

        // enable tagging audit app
        restClient.authenticateUser(adminUser).withCoreAPI().usingAudit().updateAuditApp(taggingRestAuditAppModel, "isEnabled",
                "true");

        // check isEnabled=true
        taggingRestAuditAppModel = getTaggingRestAuditAppModel(adminUser);
        taggingRestAuditAppModel.assertThat().field("isEnabled").is(true);
        taggingRestAuditAppModel.assertThat().field("name").is("Alfresco Tagging Service");
        taggingRestAuditAppModel.assertThat().field("id").is("tagging");

    }


    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify that the normal user can't enable/disable tagging application auditing")
    public void enableDisableTaggingApplicationAuditingAsNormalUser() throws Exception
    {
        // disable tagging audit app
        taggingRestAuditAppModel = getTaggingRestAuditAppModel(adminUser);
        restClient.authenticateUser(userModel).withCoreAPI().usingAudit().updateAuditApp(taggingRestAuditAppModel, "isEnabled",
                "false");

        // permission denied
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);

        // disable tagging audit app
        restClient.authenticateUser(userModel).withCoreAPI().usingAudit().updateAuditApp(taggingRestAuditAppModel, "isEnabled",
                "true");

        // permission denied
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

}