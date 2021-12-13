package org.alfresco.elasticsearch;

import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@ContextConfiguration (locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
public class ElasticsearchProximitySearchTests extends AbstractTestNGSpringContextTests
{
    @Autowired
    ServerHealth serverHealth;

    @Autowired
    DataUser dataUser;

    @Autowired
    DataContent dataContent;

    @BeforeClass (alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();

//        testUser = dataUser.createRandomTestUser();
//        testFolder = dataContent
//                .usingAdmin()
//                .usingResource(contentRoot())
//                .createFolder(new FolderModel(unique("FOLDER")));
    }

    @TestRail (section = { TestGroup.SEARCH, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION,
            description = "Verify proximity queries work correctly")
    @Test (groups = { TestGroup.SEARCH, TestGroup.TAGS, TestGroup.REGRESSION })
    public void testTAGUseCases()
    {
        throw new RuntimeException("FAIL");
    }
}
