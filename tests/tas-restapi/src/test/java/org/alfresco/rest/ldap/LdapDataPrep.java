package org.alfresco.rest.ldap;

import org.alfresco.rest.RestTest;
import org.alfresco.utility.Utility;
import org.alfresco.utility.model.UserModel;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;

public class LdapDataPrep extends RestTest
{
    protected UserModel adminUser;
    protected UserModel ldapUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {

        adminUser = dataUser.getAdminUser();
        ldapUser = new UserModel("gica", "gica");

        // Wait for LDAP users to sync
        Utility.sleep(500, 60000, () ->
        {
            restClient.authenticateUser(ldapUser).withCoreAPI().usingUser(ldapUser).getPerson();
            restClient.assertStatusCodeIs(HttpStatus.OK);
        });
    }


}
