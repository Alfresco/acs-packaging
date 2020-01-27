package org.alfresco.rest.ldap;

import org.alfresco.rest.model.RestPersonModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

import javax.json.Json;
import javax.json.JsonObject;

public class PeopleTests extends LdapDataPrep
{
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY, TestGroup.LDAP })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.LDAP }, executionType = ExecutionType.SANITY, description = "Verify user is not able to change LDAP synced attributes using REST API")
    public void userIsNotAbleToChangeLdapSyncedAttributes() throws Exception
    {
        RestPersonModel ldapPersonModel = restClient.authenticateUser(ldapUser).withCoreAPI().usingUser(ldapUser).getPerson();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        ldapPersonModel.assertThat().field("firstName").is("gica");
        ldapPersonModel.assertThat().field("lastName").is("gica");
        ldapPersonModel.assertThat().field("email").is("gica@example.com");

        JsonObject userPropsUpdate = Json.createObjectBuilder().add("firstName", "differentFirstName")
            .add("lastName", "differentLastName").add("email", "differentEmail@example.com")
            .add("skypeId", "idUpdatedByUser").build();

        String putBody = userPropsUpdate.toString();

        ldapPersonModel = restClient.withCoreAPI().usingUser(ldapUser).updatePerson(putBody);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        ldapPersonModel.assertThat().field("firstName").is("gica");
        ldapPersonModel.assertThat().field("lastName").is("gica");
        ldapPersonModel.assertThat().field("email").is("gica@example.com");
        ldapPersonModel.assertThat().field("skypeId").is("idUpdatedByUser");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY, TestGroup.LDAP })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.LDAP }, executionType = ExecutionType.SANITY, description = "Verify admin is not able to change LDAP synced attributes using REST API")
    public void adminIsNotAbleToChangeLdapSyncedAttributes() throws Exception
    {
        RestPersonModel ldapPersonModel = restClient.authenticateUser(adminUser).withCoreAPI().usingUser(ldapUser).getPerson();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        ldapPersonModel.assertThat().field("firstName").is("gica");
        ldapPersonModel.assertThat().field("lastName").is("gica");
        ldapPersonModel.assertThat().field("email").is("gica@example.com");

        JsonObject userPropsUpdate = Json.createObjectBuilder().add("firstName", "differentFirstName")
            .add("lastName", "differentLastName").add("email", "differentEmail@example.com")
            .add("skypeId", "idUpdatedByAdmin").build();

        String putBody = userPropsUpdate.toString();

        ldapPersonModel = restClient.withCoreAPI().usingUser(ldapUser).updatePerson(putBody);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        ldapPersonModel.assertThat().field("firstName").is("gica");
        ldapPersonModel.assertThat().field("lastName").is("gica");
        ldapPersonModel.assertThat().field("email").is("gica@example.com");
        ldapPersonModel.assertThat().field("skypeId").is("idUpdatedByAdmin");
    }
}