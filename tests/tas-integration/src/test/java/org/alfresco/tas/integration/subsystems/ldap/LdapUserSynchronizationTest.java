package org.alfresco.tas.integration.subsystems.ldap;

import org.alfresco.tas.integration.IntegrationTest;
import org.alfresco.utility.Utility;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.network.JmxBuilder;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.alfresco.utility.report.log.Step.STEP;

@Test(groups = {TestGroup.REQUIRE_JMX})
public class LdapUserSynchronizationTest extends IntegrationTest
{
    @Autowired
    protected JmxBuilder jmxBuilder;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        //Check ldap is present in the chain
        String ldapEnabled = jmxBuilder.getJmxClient().readProperty("Alfresco:Type=Configuration,Category=Authentication,id1=managed,id2=ldap1", "ldap.authentication.active").toString();
        Assert.assertEquals(ldapEnabled, Boolean.TRUE.toString(), String.format("Property ldap.authentication.active is [%s]", ldapEnabled));

        //Disable auto sync at subsystem start
        jmxBuilder.getJmxClient().writeProperty("Alfresco:Category=Synchronization,Type=Configuration,id1=default", "synchronization.syncOnStartup", "false");
    }

    @Test(groups = {TestGroup.LDAP})
    @TestRail(section = {TestGroup.LDAP}, executionType = ExecutionType.SANITY, description = "Verify if the user synchronization job runs after a property was changed (the subsystem is restarted)")
    public void triggerSyncJobAndCheckStatus() throws Exception
    {
        STEP("1. Change the cron expression -> causes the subsystem to restart");
        jmxBuilder.getJmxClient().writeProperty("Alfresco:Category=Synchronization,Type=Configuration,id1=default", "synchronization.import.cron", "0 0 3 * * ?");

        // Wait for Synchronization (LDAP) subsystem to restart
        Utility.sleep(500, 5000, () ->
        {
            String ldapEnabled = jmxBuilder.getJmxClient().readProperty("Alfresco:Type=Configuration,Category=Authentication,id1=managed,id2=ldap1", "ldap.authentication.active").toString();
            Assert.assertEquals(ldapEnabled, Boolean.TRUE.toString(), String.format("Property ldap.authentication.active is [%s]", ldapEnabled));
        });

        String syncStatusPrevExecutionTime = jmxBuilder.getJmxClient().readProperty("Alfresco:Name=BatchJobs,Type=Synchronization,Category=manager", "SyncEndTime").toString();

        STEP("2. Trigger the ldap users sync job");
        jmxBuilder.getJmxClient().executeJMXMethod("Alfresco:Name=Schedule,Group=DEFAULT,Type=MonitoredCronTrigger,Trigger=syncTrigger", "executeNow");

        // Wait for the sync job to finish
        Utility.sleep(300, 5000, () ->
        {
            STEP("3. Verify the execution time of the sync job");
            String syncStatusNewExecutionTime = jmxBuilder.getJmxClient().readProperty("Alfresco:Name=BatchJobs,Type=Synchronization,Category=manager", "SyncEndTime").toString();
            Assert.assertNotEquals(syncStatusPrevExecutionTime, syncStatusNewExecutionTime,
                    String.format("The ldap user sync job didn't execute.\nPrevious run time: [%s]\nNew run time: [%s]", syncStatusPrevExecutionTime, syncStatusNewExecutionTime));
        });
    }
}