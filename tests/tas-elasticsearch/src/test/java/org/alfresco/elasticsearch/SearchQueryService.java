package org.alfresco.elasticsearch;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchNodeModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.Utility;
import org.springframework.beans.factory.annotation.Autowired;

/** A class providing methods for testing search queries. */
public class SearchQueryService
{
    @Autowired
    private RestWrapper client;

    public void expectResultsFromQuery(String queryString, org.alfresco.utility.model.UserModel user, String... expected)
    {
        try
        {
            Utility.sleep(1000, 20000, () ->
            {
                SearchRequest query = new SearchRequest();
                RestRequestQueryModel queryReq = new RestRequestQueryModel();
                queryReq.setQuery(queryString);
                query.setQuery(queryReq);
                SearchResponse response = client.authenticateUser(user)
                                                .withSearchAPI()
                                                .search(query);
                assertSearchResults(response, expected);
            });
        }
        catch (InterruptedException e)
        {
            fail("InterruptedException received while waiting for results.");
        }
    }

    private void assertSearchResults(SearchResponse actual, String... expected)
    {
        Set<String> result = actual.getEntries().stream()
                                   .map(SearchNodeModel::getModel)
                                   .map(SearchNodeModel::getName)
                                   .collect(Collectors.toSet());
        Set<String> expectedList = Sets.newHashSet(expected);
        assertEquals(result, expectedList, "Unexpected search results.");
    }
}
