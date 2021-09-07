package org.alfresco.elasticsearch;

import static org.alfresco.elasticsearch.SearchQueryService.req;

import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Basic test for Elasticsearch server with Basic Authentication.
 * The aim of this class is to test that Basic Authentication is working as expected,
 * feature testing is expected to be covered by ElasticsearchLiveIndexingTests class.
 */
@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
    initializers = AlfrescoStackInitializerESBasicAuth.class)
public class ElasticsearchBasicAuthTests extends AbstractTestNGSpringContextTests
{
  private static final String FILE_0_NAME = "test.txt";

  @Autowired
  private ServerHealth serverHealth;
  @Autowired
  private DataUser dataUser;
  @Autowired
  private DataContent dataContent;
  @Autowired
  private DataSite dataSite;
  @Autowired
  private SearchQueryService searchQueryService;

  private UserModel userSite1;
  private SiteModel siteModel1;

  @BeforeClass(alwaysRun = true)
  public void dataPreparation()
  {
    serverHealth.assertServerIsOnline();
    userSite1 = dataUser.createRandomTestUser();
    siteModel1 = dataSite.usingUser(userSite1).createPrivateRandomSite();
    createContent(FILE_0_NAME, "This is the first test", siteModel1, userSite1);
  }

  private FileModel createContent(String filename, String content, SiteModel site, UserModel user)
  {
    FileModel fileModel = new FileModel(filename, FileType.TEXT_PLAIN, content);
    return dataContent.usingUser(user).usingSite(site)
        .createContent(fileModel);
  }

  @TestRail(section = TestGroup.SEARCH,
      executionType = ExecutionType.REGRESSION,
      description = "Verify that the simpler Elasticsearch search works as expected.")
  @Test(groups = TestGroup.SEARCH)
  public void searchCanFindAFile()
  {
    searchQueryService.expectResultsFromQuery(req("first"), userSite1, FILE_0_NAME);
  }
}