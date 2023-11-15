package org.alfresco.rest.mtls;

import org.alfresco.rest.MtlsRestTest;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.Utility;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.UserModel;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ContextConfiguration("classpath:alfresco-mtls-context.xml")
public class SearchServiceTest extends MtlsRestTest
{
    private static final Random RANDOM = new Random();
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String UNIQUE_WORD = getAlphabeticUUID();

    private UserModel adminUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        adminUser = dataUser.getAdminUser();
    }

    @Test
    public void testRenditionWithMTLSEnabledTest() throws InterruptedException
    {
        FolderModel folderModel = selectSharedFolder(adminUser);

        restClient.authenticateUser(adminUser).configureRequestSpec().addMultiPart("filedata", Utility.getTestResourceFile("testing-search-mtls.txt"));
        restClient.authenticateUser(adminUser).withCoreAPI().usingNode(folderModel).createNode();

        Thread.sleep(240000);

        RestRequestQueryModel queryModel = new RestRequestQueryModel();
        queryModel.setLanguage("afts");
        queryModel.setQuery("incomprehensible"); // AND TEXT:

        SearchRequest searchRequest = new SearchRequest(queryModel);
        SearchResponse searchResponse = restClient.authenticateUser(adminUser).withSearchAPI().search(searchRequest);
        Assert.assertEquals(searchResponse.getEntries().size(), 1);
    }

    /** Generate a random 'word' containing 16 alphabetic characters. */
    public static String getAlphabeticUUID()
    {
        return IntStream.range(0, 16)
                .map(i -> ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())))
                .mapToObj(Character::toString)
                .collect(Collectors.joining());
    }
}
