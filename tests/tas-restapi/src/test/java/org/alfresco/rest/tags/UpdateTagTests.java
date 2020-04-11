package org.alfresco.rest.tags;

import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.utility.Utility;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 10/4/2016.
 */
@Test(groups = {TestGroup.REQUIRE_SOLR})
public class UpdateTagTests extends TagsDataPrep
{
    private RestTagModel oldTag;
    private String randomTag = "";

    @BeforeMethod(alwaysRun=true)
    public void addTagToDocument() throws Exception
    {
        restClient.authenticateUser(adminUserModel);
        oldTag = restClient.withCoreAPI().usingResource(document).addTag(RandomData.getRandomName("old"));
        randomTag = RandomData.getRandomName("tag");
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, description = "Verify Manager user can't update tags with Rest API and status code is 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION })
    public void managerIsNotAbleToUpdateTag() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingTag(oldTag).update(randomTag);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS },
            executionType = ExecutionType.REGRESSION, description = "Verify Collaborator user can't update tags with Rest API and status code is 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION })
    public void collaboratorIsNotAbleToUpdateTagCheckDefaultErrorModelSchema() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        restClient.withCoreAPI().usingTag(oldTag).update(randomTag);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
            .containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS },
            executionType = ExecutionType.REGRESSION, description = "Verify Contributor user can't update tags with Rest API and status code is 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION })
    public void contributorIsNotAbleToUpdateTag() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        restClient.withCoreAPI().usingTag(oldTag).update(randomTag);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS },
            executionType = ExecutionType.SANITY, description = "Verify Consumer user can't update tags with Rest API and status code is 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION })
    public void consumerIsNotAbleToUpdateTag() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        restClient.withCoreAPI().usingTag(oldTag).update(randomTag);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS },
            executionType = ExecutionType.SANITY, description = "Verify user gets status code 401 if authentication call fails")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
//    @Bug(id="MNT-16904", description = "It fails only on environment with tenants")
    public void userIsNotAbleToUpdateTagIfAuthenticationFails() throws Exception
    {
        UserModel siteManager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        String managerPassword = siteManager.getPassword();
        siteManager.setPassword("wrongPassword");
        restClient.authenticateUser(siteManager);
        restClient.withCoreAPI().usingTag(oldTag).update(randomTag);
        siteManager.setPassword(managerPassword);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, description = "Verify admin is not able to update tag with invalid id")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION })
    public void adminIsNotAbleToUpdateTagWithInvalidId() throws Exception
    {
        String invalidTagId = "invalid-id";
        RestTagModel tag = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).addTag(RandomData.getRandomName("tag"));
        tag.setId(invalidTagId);
        restClient.withCoreAPI().usingTag(tag).update(RandomData.getRandomName("tag"));
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, invalidTagId));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, description = "Verify admin is not able to update tag with empty id")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION })
    public void adminIsNotAbleToUpdateTagWithEmptyId() throws Exception
    {
        RestTagModel tag = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).addTag(RandomData.getRandomName("tag"));
        tag.setId("");
        restClient.withCoreAPI().usingTag(tag).update(RandomData.getRandomName("tag"));
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED)
                .assertLastError().containsSummary(RestErrorModel.PUT_EMPTY_ARGUMENT);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, description = "Verify admin is not able to update tag with invalid body")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION })
    public void adminIsNotAbleToUpdateTagWithEmptyBody() throws Exception
    {
        RestTagModel tag = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).addTag(RandomData.getRandomName("tag"));
        restClient.withCoreAPI().usingTag(tag).update("");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary(RestErrorModel.EMPTY_TAG);
    }
}