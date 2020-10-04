package org.alfresco.rest.discovery;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.model.RestDiscoveryModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DiscoveryTests extends RestTest
{
    private UserModel adminModel, userModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {  
        adminModel = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.DISCOVERY, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.DISCOVERY }, executionType = ExecutionType.SANITY,
            description = "Sanity tests for GET /discovery endpoint")
    public void getRepositoryInformation() throws Exception
    {
        // Get repository info from admin console
        RestResponse adminConsoleRepoInfo = restClient.authenticateUser(adminModel).withAdminConsole().getAdminConsoleRepoInfo();
        String id = adminConsoleRepoInfo.getResponse().getBody().path("data.id");
        String edition = adminConsoleRepoInfo.getResponse().getBody().path("data.edition");
        String schema = adminConsoleRepoInfo.getResponse().getBody().path("data.schema");
        String version = adminConsoleRepoInfo.getResponse().getBody().path("data.version");

        // Get repository info using Discovery API
        RestDiscoveryModel response = restClient.authenticateUser(userModel).withDiscoveryAPI().getRepositoryInfo();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        // Compare information
        response.getRepository().getVersion().assertThat().field("major").is(version.charAt(0));
        response.getRepository().getVersion().assertThat().field("minor").is(version.charAt(2));
        response.getRepository().getVersion().assertThat().field("patch").is(version.charAt(4));
        response.getRepository().getVersion().assertThat().field("schema").is(schema);
        response.getRepository().getId().equals(id);
        response.getRepository().getEdition().equals(edition);
        response.getRepository().getStatus().assertThat().field("isReadOnly").is(false);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.DISCOVERY, TestGroup.ALL_AMPS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.DISCOVERY }, executionType = ExecutionType.SANITY,
            description = "Sanity tests for GET /discovery endpoint")
    public void getRepositoryInstalledModules() throws Exception
    {
        // Get repository info using Discovery API
        restClient.authenticateUser(userModel).withDiscoveryAPI().getRepositoryInfo();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        // Check that all modules are present
        List<String> modules = restClient.onResponse().getResponse().jsonPath().getList("entry.repository.modules.id", String.class);
        List<String> expectedModules = Arrays.asList(
                "alfresco-saml-repo",
//                "org_alfresco_device_sync_repo",
                "org_alfresco_module_xamconnector",
                "org.alfresco.module.TransformationServer",
                "alfresco-rm-enterprise-repo",
                "org.alfresco.module.KofaxAddon",
//              "org_alfresco_module_rm",
                "alfresco-aos-module",
                "org.alfresco.integrations.google.docs",
//              "alfresco-ai-repo",
                "alfresco-share-services",
//                "org_alfresco_integrations_AzureConnector",
                "org_alfresco_mm_repo",
//              "alfresco-trashcan-cleaner",
                "alfresco-content-connector-for-salesforce-repo");

        // Check that all installed modules are in INSTALLED state
        List<String> modulesStates = restClient.onResponse().getResponse().jsonPath().getList("entry.repository.modules.installState", String.class);
        if (expectedModules.size() != Collections.frequency(modulesStates, "INSTALLED"))
        {
            //([[installState:INSTALLED, versionMin:6.0, versionMax:6.99, installDate:2020-09-26T23:39:08.513+0000, description:The Repository piece of the Alfresco SAML Module, id:alfresco-saml-repo, title:Alfresco SAML Repository AMP Module, version:1.1.1],
    		// [installState:INSTALLED, versionMin:6.0, versionMax:999, installDate:2020-09-26T23:39:08.921+0000, description:Alfresco Content Connector for EMC Centera, id:org_alfresco_module_xamconnector, title:Centera Connector, version:2.2.1],
    		// [installState:INSTALLED, versionMin:6.0.0, versionMax:6.99.99, installDate:2020-09-26T23:39:08.833+0000, description:Alfresco Document Transformation Engine Module for Repository, id:org.alfresco.module.TransformationServer, title:Document Transformation Engine AMP for Repository, version:2018.221],
    		// [installState:INSTALLED, versionMin:6.0, versionMax:999, installDate:2020-09-26T23:39:09.468+0000, description:Alfresco Governance Services Enterprise Repository Extension, id:alfresco-rm-enterprise-repo, title:AGS Enterprise Repo, version:3.0.0],
	    	// [installState:INSTALLED, versionMin:2.1, versionMax:999, installDate:2020-09-26T23:39:08.746+0000, description:Alfresco Kofax Integration, id:org.alfresco.module.KofaxAddon, title:Kofax Add-on, version:2.0.0],
	    	// [installState:UNKNOWN, versionMin:6.0, versionMax:999, description:Alfresco Governance Services Repository Extension, id:org_alfresco_module_rm, title:AGS Repo, version:3.0.0],
    		// [installState:INSTALLED, versionMin:6.0, versionMax:999, installDate:2020-09-26T23:39:09.486+0000, description:Allows applications that can talk to a SharePoint server to talk to your Alfresco installation, id:alfresco-aos-module, title:Alfresco Office Services Module, version:1.2.2.1],
		    // [installState:INSTALLED, versionMin:6.0.0, versionMax:6.99.99, installDate:2020-09-26T23:39:08.907+0000, description:The Repository side artifacts of the Alfresco / Google Docs Integration., id:org.alfresco.integrations.google.docs, title:Alfresco / Google Docs Integration, version:3.1.0],
	    	// [installState:UNKNOWN, versionMin:6.1.0, versionMax:6.99.99, description:Platform/Repo JAR Module (to be included in the alfresco.war) - part of AIO - SDK 4.0, id:alfresco-ai-repo, title:Alfresco Platform/Repository JAR Module, version:1.0.1],
	    	// [installState:INSTALLED, versionMin:6.1, versionMax:999, installDate:2020-09-26T23:39:08.985+0000, description:Module to be applied to alfresco.war, containing APIs for Alfresco Share, id:alfresco-share-services, title:Alfresco Share Services AMP, version:6.1.0],
	    	// [installState:INSTALLED, versionMin:6.1.0, versionMax:6.99.99, installDate:2020-09-26T23:39:09.075+0000, description:Extensions in the Alfresco repository to provide media / digital asset management (DAM) features, id:org_alfresco_mm_repo, title:Alfresco Media Management Repository AMP, version:1.3.0],
	    	// [installState:UNKNOWN, versionMin:0, versionMax:999, description:The Alfresco Trashcan Cleaner (Alfresco Module), id:alfresco-trashcan-cleaner, title:alfresco-trashcan-cleaner project, version:2.3],
	    	// [installState:INSTALLED, versionMin:6.0.0, versionMax:6.99.99, installDate:2020-09-26T23:39:08.821+0000, description:Alfresco Repository artifacts needed for the Alfresco Content Connector for Salesforce Repository Amp, id:alfresco-content-connector-for-salesforce-repo, title:Alfresco Content Connector for Salesforce Repository AMP, version:2.1.0]])

            String mods = restClient.onResponse().getResponse().jsonPath().getString("entry.repository.modules");
            assertEquals("Number of amps installed ("+mods+") should match expected", expectedModules.size(),
                    Collections.frequency(modulesStates, "INSTALLED"));
        }
        expectedModules.forEach(module ->
                assertTrue(modules.contains(module), String.format("Expected module %s is not installed", module)));
    }
}
