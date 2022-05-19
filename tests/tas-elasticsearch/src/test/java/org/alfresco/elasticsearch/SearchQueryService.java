package org.alfresco.elasticsearch;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchNodeModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.Utility;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.log.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/** A class providing methods for testing search queries. */
public class SearchQueryService
{
    @Autowired
    private RestWrapper client;

    /** Assert that the query returns no results. */
    public void expectNoResultsFromQuery(SearchRequest searchRequest, UserModel testUser)
    {
        expectResultsFromQuery(searchRequest, testUser);
    }

    /** Assert that the query returns something, without checking exactly what it returns. */
    public void expectSomeResultsFromQuery(SearchRequest searchRequest, UserModel testUser)
    {
        Consumer<SearchResponse> assertNotEmpty = searchResponse -> assertFalse(searchResponse.isEmpty());
        expectResultsFromQuery(searchRequest, testUser, assertNotEmpty);
    }

    public void expectResultsInOrder(SearchRequest searchRequest, UserModel user, List<String> expected)
    {
        Consumer<SearchResponse> response = searchResponse -> assertNamesInOrder(searchResponse, expected);
        expectResultsFromQuery(searchRequest,user,response);
    }

    public void expectResultsFromQuery(SearchRequest searchRequest, UserModel user, String... expected)
    {
        Consumer<SearchResponse> assertNames = searchResponse -> assertNames(searchResponse, expected);
        expectResultsFromQuery(searchRequest, user, assertNames);
    }

    public void expectNodeTypesFromQuery(SearchRequest searchRequest, UserModel user, String... expected)
    {
        Consumer<SearchResponse> assertNodeTypes = searchResponse -> assertNodeTypes(searchResponse, expected);
        expectResultsFromQuery(searchRequest, user, assertNodeTypes);
    }

    public void expectNodeRefsFromQuery(SearchRequest searchRequest, UserModel user, String... expectedNodeRefs)
    {
        Consumer<SearchResponse> assertNames = searchResponse -> assertNodeRefs(searchResponse, expectedNodeRefs);
        expectResultsFromQuery(searchRequest, user, assertNames);
    }

    public void expectAllResultsFromQuery(SearchRequest searchRequest, UserModel user, Predicate<SearchNodeModel> assertionMethod)
    {
        Function<SearchNodeModel, String> failureMessage = searchNodeModel -> "'" + searchNodeModel.getName() + "' did not satisfy predicate.";
        expectAllResultsFromQuery(searchRequest, user, assertionMethod, failureMessage);
    }

    public void expectAllResultsFromQuery(SearchRequest searchRequest, UserModel user, Predicate<SearchNodeModel> assertionMethod, Function<SearchNodeModel, String> failureMessageFunction)
    {
        expectResultsFromQuery(searchRequest, user, searchResponse -> assertAllSearchResults(searchResponse, assertionMethod, failureMessageFunction));
    }

    private void expectResultsFromQuery(SearchRequest searchRequest, org.alfresco.utility.model.UserModel user, Consumer<SearchResponse> assertionMethod)
    {
        try
        {
            Utility.sleep(1000, 20000, () ->
            {
                SearchResponse response = client.authenticateUser(user)
                                                .withSearchAPI()
                                                .search(searchRequest);
                client.assertStatusCodeIs(HttpStatus.OK);
                assertionMethod.accept(response);
            });
        }
        catch (InterruptedException e)
        {
            fail("InterruptedException received while waiting for results.");
        }
    }

    public void expectErrorFromQuery(SearchRequest searchRequest, org.alfresco.utility.model.UserModel user, 
                                        HttpStatus expectedStatusCode, String containsErrorString)
    {
        client.authenticateUser(user).withSearchAPI().search(searchRequest);
        client.assertStatusCodeIs(expectedStatusCode);
        client.assertLastError().containsSummary(containsErrorString);
    }

    private void assertNodeRefs(SearchResponse actual, String... expected)
    {
        Set<String> result = actual.getEntries().stream()
                                   .map(SearchNodeModel::getModel)
                                   .map(SearchNodeModel::getId)
                                   .collect(Collectors.toSet());
        Set<String> expectedList = Sets.newHashSet(expected);
        assertEquals(result, expectedList, "Unexpected search results.");
    }

    private void assertNames(SearchResponse actual, String... expected)
    {
        Set<String> result = actual.getEntries().stream()
                                   .map(SearchNodeModel::getModel)
                                   .map(SearchNodeModel::getName)
                                   .collect(Collectors.toSet());
        Set<String> expectedList = Sets.newHashSet(expected);
        assertEquals(result, expectedList, "Unexpected search results.");
    }

    private void assertNamesInOrder(SearchResponse actual, List<String> expected)
    {
        List<String> result = actual.getEntries().stream()
                .map(SearchNodeModel::getModel)
                .map(SearchNodeModel::getName)
                .collect(Collectors.toList());
        System.out.println("result = " + result);
        assertEquals(result, expected, "Unexpected search results.");
    }

    private void assertNodeTypes(SearchResponse actual, String... expected)
    {
        Set<String> result = actual.getEntries().stream()
                                   .map(SearchNodeModel::getModel)
                                   .map(SearchNodeModel::getNodeType)
                                   .collect(Collectors.toSet());
        Set<String> expectedList = Sets.newHashSet(expected);
        assertEquals(result, expectedList, "Unexpected search results.");
    }

    private void assertAllSearchResults(SearchResponse actual, Predicate<SearchNodeModel> assertion, Function<SearchNodeModel, String> failureMessageFunction)
    {
        String result = actual.getEntries().stream()
                              .map(SearchNodeModel::getModel)
                              .filter(Predicate.not(assertion))
                              .map(failureMessageFunction)
                              .collect(Collectors.joining("\n"));
        assertTrue(result.isEmpty(), "assertAllSearchResults failed with these issues:\n" + result);
    }

    public static SearchRequest req(String query)
    {
        return req(null, query);
    }

    public static SearchRequest req(String language, String query)
    {
        RestRequestQueryModel restRequestQueryModel = new RestRequestQueryModel();
        restRequestQueryModel.setQuery(query);
        Optional.ofNullable(language).ifPresent(restRequestQueryModel::setLanguage);
        return new SearchRequest(restRequestQueryModel);
    }
}
