package org.alfresco.elasticsearch.upgrade;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

class RepoHttpClient
{
    private static final int HTTP_TIMEOUT_MS = 5_000;

    final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(
                    RequestConfig.copy(RequestConfig.DEFAULT)
                            .setConnectionRequestTimeout(HTTP_TIMEOUT_MS)
                            .setSocketTimeout(HTTP_TIMEOUT_MS)
                            .setConnectionRequestTimeout(HTTP_TIMEOUT_MS)
                            .setRedirectsEnabled(false)
                            .build())
            .build();

    final Gson gson = new Gson();
    private final URI searchApiUri;
    private final URI fileUploadApiUri;
    private final URI searchServiceAdminAppUri;
    private final URI uploadLicenseAdminApiUri;
    private final URI serverApiUri;
    // ACS-12862: endpoints used to seed the advanced migration scenarios.
    private final URI nodesApiUri;
    private final URI peopleApiUri;
    private final URI groupsApiUri;
    private final URI categoriesApiUri;

    RepoHttpClient(final URI repoBaseUri)
    {
        searchApiUri = repoBaseUri.resolve("/alfresco/api/-default-/public/search/versions/1/search");
        fileUploadApiUri = repoBaseUri.resolve("/alfresco/api/-default-/public/alfresco/versions/1/nodes/-my-/children");
        searchServiceAdminAppUri = repoBaseUri.resolve("/alfresco/s/enterprise/admin/admin-searchservice");
        uploadLicenseAdminApiUri = repoBaseUri.resolve("/alfresco/s/enterprise/admin/admin-license-upload");
        serverApiUri = repoBaseUri.resolve("/alfresco/service/api/server");
        // ACS-12862: nodesApiUri and categoriesApiUri keep a trailing slash so URI.resolve appends
        // to them; peopleApiUri and groupsApiUri are complete endpoints and must not have one.
        nodesApiUri = repoBaseUri.resolve("/alfresco/api/-default-/public/alfresco/versions/1/nodes/");
        peopleApiUri = repoBaseUri.resolve("/alfresco/api/-default-/public/alfresco/versions/1/people");
        groupsApiUri = repoBaseUri.resolve("/alfresco/api/-default-/public/alfresco/versions/1/groups");
        categoriesApiUri = repoBaseUri.resolve("/alfresco/api/-default-/public/alfresco/versions/1/categories/");
    }

    public void setSearchService(String implementation) throws IOException
    {
        final HttpGet getCsrfToken = authenticate(new HttpGet(searchServiceAdminAppUri));
        final HttpClientContext httpCtx = HttpClientContext.create();
        httpCtx.setCookieStore(new BasicCookieStore());

        final Cookie csrfCookie;

        try (CloseableHttpResponse response = client.execute(getCsrfToken, httpCtx))
        {
            final Map<String, Cookie> cookies = httpCtx
                    .getCookieStore()
                    .getCookies()
                    .stream()
                    .collect(Collectors.toUnmodifiableMap(Cookie::getName, Function.identity()));

            csrfCookie = Objects.requireNonNull(cookies.get("alf-csrftoken"));
        }

        final URI changeSearchServiceUri = URI.create(new URIBuilder(searchServiceAdminAppUri)
                .addParameter("t", "/enterprise/admin/admin-searchservice")
                .addParameter(csrfCookie.getName(), URLDecoder.decode(csrfCookie.getValue(), StandardCharsets.US_ASCII))
                .toString());
        final HttpEntity changeSearchServiceFormEntity = MultipartEntityBuilder
                .create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addTextBody("Alfresco:Type=Configuration,Category=Search,id1=manager|sourceBeanName", implementation)
                .build();

        final HttpPost changeSearchServiceRequest = authenticate(new HttpPost(changeSearchServiceUri));
        changeSearchServiceRequest.setEntity(changeSearchServiceFormEntity);

        try (CloseableHttpResponse response = client.execute(changeSearchServiceRequest, httpCtx))
        {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_MOVED_PERMANENTLY)
            {
                throw new IllegalStateException("Couldn't switch to `" + implementation + "`.");
            }
        }
    }

    public void uploadLicense(File license) throws IOException
    {
        final HttpGet getCsrfToken = authenticate(new HttpGet(uploadLicenseAdminApiUri));
        final HttpClientContext httpCtx = HttpClientContext.create();
        httpCtx.setCookieStore(new BasicCookieStore());

        final Cookie csrfCookie;

        try (CloseableHttpResponse response = client.execute(getCsrfToken, httpCtx))
        {
            final Map<String, Cookie> cookies = httpCtx
                    .getCookieStore()
                    .getCookies()
                    .stream()
                    .collect(Collectors.toUnmodifiableMap(Cookie::getName, Function.identity()));

            csrfCookie = Objects.requireNonNull(cookies.get("alf-csrftoken"));
        }

        final URI uploadLicenseAdminCsrfApiUri = URI.create(new URIBuilder(uploadLicenseAdminApiUri)
                .addParameter(csrfCookie.getName(), URLDecoder.decode(csrfCookie.getValue(), StandardCharsets.US_ASCII))
                .toString());

        final HttpEntity uploadEntity = MultipartEntityBuilder
                .create()
                .setMode(HttpMultipartMode.STRICT)
                .addBinaryBody("license", license)
                .build();

        final HttpPost uploadRequest = authenticate(new HttpPost(uploadLicenseAdminCsrfApiUri));
        uploadRequest.setEntity(uploadEntity);

        var responseMap = executeAndGetResponseMap(uploadRequest, httpCtx);
        if (!Boolean.TRUE.equals(responseMap.get("success")))
        {
            throw new IOException("Failed to upload a licence. Server response error: " + responseMap.get("error"));
        }
    }

    public boolean isServerUp() throws IOException
    {
        final HttpGet healthCheckRequest = authenticate(new HttpGet(serverApiUri));
        try (CloseableHttpResponse response = client.execute(healthCheckRequest))
        {
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        }
    }

    public UUID uploadFile(URL contentUrl, String fileName) throws IOException
    {
        try (InputStream is = contentUrl.openStream())
        {
            final HttpEntity uploadEntity = MultipartEntityBuilder
                    .create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .addBinaryBody("filedata", is, ContentType.DEFAULT_BINARY, fileName)
                    .build();

            final HttpPost uploadRequest = authenticate(new HttpPost(fileUploadApiUri));
            uploadRequest.setEntity(uploadEntity);

            final Optional<Map<?, ?>> uploadResult = getJsonResponse(uploadRequest, HttpStatus.SC_CREATED);

            return uploadResult
                    .map(r -> r.get("entry"))
                    .filter(Map.class::isInstance).map(Map.class::cast)
                    .map(e -> e.get("id"))
                    .filter(String.class::isInstance).map(String.class::cast)
                    .map(UUID::fromString)
                    .orElseThrow();
        }
    }

    /**
     * ACS-12862: body extracted into {@link #doSearch}, which is now shared with
     * {@link #search} and {@link #searchAs}. Signature and behaviour are unchanged.
     */
    public Optional<Set<String>> searchForFiles(String term) throws IOException
    {
        return search("afts", term);
    }

    /** ACS-12862: runs a query in the given language ("afts" or "cmis") as admin. */
    public Optional<Set<String>> search(String language, String query) throws IOException
    {
        return doSearch(authenticate(new HttpPost(searchApiUri)), language, query);
    }

    /** ACS-12862: runs a query as the given user, for permission-filtering checks. */
    public Optional<Set<String>> searchAs(String user, String password, String language, String query) throws IOException
    {
        return doSearch(authenticateAs(new HttpPost(searchApiUri), user, password), language, query);
    }

    /** ACS-12862: creates a folder under the given parent (a node id, or an alias such as "-my-"). */
    public UUID createFolder(String parentId, String name) throws IOException
    {
        final String body = "{\"name\":\"" + name + "\",\"nodeType\":\"cm:folder\"}";
        final HttpPost request = authenticate(new HttpPost(childrenUriFor(parentId)));
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        return extractNodeId(getJsonResponse(request, HttpStatus.SC_CREATED));
    }

    /** ACS-12862: uploads a plain-text file with the given content under the given parent. */
    public UUID uploadTextFile(String parentId, String fileName, String content) throws IOException
    {
        final HttpEntity uploadEntity = MultipartEntityBuilder
                .create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addBinaryBody("filedata", content.getBytes(StandardCharsets.UTF_8),
                        ContentType.create("text/plain", StandardCharsets.UTF_8), fileName)
                .build();

        final HttpPost request = authenticate(new HttpPost(childrenUriFor(parentId)));
        request.setEntity(uploadEntity);
        return extractNodeId(getJsonResponse(request, HttpStatus.SC_CREATED));
    }

    /** ACS-12862: adds an existing node as a secondary child of the given folder. */
    public void addSecondaryChildAssociation(String parentFolderId, UUID childId) throws IOException
    {
        final String body = "{\"childId\":\"" + childId + "\",\"assocType\":\"cm:contains\"}";
        final HttpPost request = authenticate(new HttpPost(nodesApiUri.resolve(parentFolderId + "/secondary-children")));
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        if (getJsonResponse(request, HttpStatus.SC_CREATED).isEmpty())
        {
            throw new IOException("Failed to add secondary child association for " + childId);
        }
    }

    /** ACS-12862: tags a node. */
    public void addTag(UUID nodeId, String tag) throws IOException
    {
        final String body = "{\"tag\":\"" + tag + "\"}";
        final HttpPost request = authenticate(new HttpPost(nodesApiUri.resolve(nodeId + "/tags")));
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        if (getJsonResponse(request, HttpStatus.SC_CREATED).isEmpty())
        {
            throw new IOException("Failed to add tag `" + tag + "` to " + nodeId);
        }
    }

    /**
     * ACS-12862: creates a category under the root category and returns its node id.
     * Creating one keeps the fixture deterministic. Looking up an arbitrary existing
     * cm:category node is not: TYPE queries have no defined result order, and
     * cm:category has subtypes such as tags.
     */
    public String createCategory(String name) throws IOException
    {
        final String body = "{\"name\":\"" + name + "\"}";
        final HttpPost request = authenticate(new HttpPost(categoriesApiUri.resolve("-root-/subcategories")));
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        return getJsonResponse(request, HttpStatus.SC_CREATED)
                .map(r -> r.get("entry"))
                .filter(Map.class::isInstance).map(Map.class::cast)
                .map(e -> e.get("id"))
                .filter(String.class::isInstance).map(String.class::cast)
                .orElseThrow(() -> new IOException("Failed to create category `" + name + "`"));
    }

    /**
     * ACS-12862: adds the cm:generalclassifiable aspect and classifies the node into the
     * given category nodeRef (format: workspace://SpacesStore/&lt;id&gt;).
     */
    public void setCategory(UUID nodeId, String categoryNodeRef) throws IOException
    {
        final String body = "{\"aspectNames\":[\"cm:generalclassifiable\"],"
                + "\"properties\":{\"cm:categories\":[\"" + categoryNodeRef + "\"]}}";

        final HttpPut request = authenticate(new HttpPut(nodesApiUri.resolve(nodeId.toString())));
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        if (getJsonResponse(request, HttpStatus.SC_OK).isEmpty())
        {
            throw new IOException("Failed to set category on " + nodeId);
        }
    }

    /** ACS-12862: creates a non-admin user, used by the permission-filtering scenario. */
    public void createUser(String username, String password) throws IOException
    {
        final String body = "{\"id\":\"" + username + "\","
                + "\"firstName\":\"" + username + "\","
                + "\"email\":\"" + username + "@test.com\","
                + "\"password\":\"" + password + "\"}";

        final HttpPost request = authenticate(new HttpPost(peopleApiUri));
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        if (getJsonResponse(request, HttpStatus.SC_CREATED).isEmpty())
        {
            throw new IOException("Failed to create user `" + username + "`");
        }
    }

    /** ACS-12862: creates a group and returns its full authority id (GROUP_&lt;id&gt;). */
    public String createGroup(String groupId, String displayName) throws IOException
    {
        final String body = "{\"id\":\"" + groupId + "\",\"displayName\":\"" + displayName + "\"}";
        final HttpPost request = authenticate(new HttpPost(groupsApiUri));
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        return getJsonResponse(request, HttpStatus.SC_CREATED)
                .map(r -> r.get("entry"))
                .filter(Map.class::isInstance).map(Map.class::cast)
                .map(e -> e.get("id"))
                .filter(String.class::isInstance).map(String.class::cast)
                .orElseThrow(() -> new IOException("Failed to create group `" + groupId + "`"));
    }

    /**
     * ACS-12862: adds a user to an existing group. The literal "groups/" is required because
     * groupsApiUri has no trailing slash, so URI.resolve replaces its last segment.
     */
    public void addUserToGroup(String groupAuthorityId, String username) throws IOException
    {
        final String body = "{\"id\":\"" + username + "\",\"memberType\":\"PERSON\"}";
        final HttpPost request = authenticate(new HttpPost(
                groupsApiUri.resolve("groups/" + groupAuthorityId + "/members")));
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        if (getJsonResponse(request, HttpStatus.SC_CREATED).isEmpty())
        {
            throw new IOException("Failed to add `" + username + "` to group `" + groupAuthorityId + "`");
        }
    }

    /** ACS-12862: breaks inheritance on the node and grants exactly the given authority the given role. */
    public void setExclusivePermission(UUID nodeId, String authorityId, String role) throws IOException
    {
        final String body = "{\"permissions\":{"
                + "\"isInheritanceEnabled\":false,"
                + "\"locallySet\":[{\"authorityId\":\"" + authorityId + "\","
                + "\"name\":\"" + role + "\","
                + "\"accessStatus\":\"ALLOWED\"}]}}";

        final HttpPut request = authenticate(new HttpPut(nodesApiUri.resolve(nodeId.toString())));
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        if (getJsonResponse(request, HttpStatus.SC_OK).isEmpty())
        {
            throw new IOException("Failed to set permissions on " + nodeId);
        }
    }

    /** ACS-12862: moves a node to a new primary parent, so its indexed path has to be rebuilt. */
    public void moveNode(UUID nodeId, String targetParentId) throws IOException
    {
        final String body = "{\"targetParentId\":\"" + targetParentId + "\"}";
        final HttpPost request = authenticate(new HttpPost(nodesApiUri.resolve(nodeId + "/move")));
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        if (getJsonResponse(request, HttpStatus.SC_OK).isEmpty())
        {
            throw new IOException("Failed to move " + nodeId + " under " + targetParentId);
        }
    }

    /** ACS-12862: replaces a node's content, so the re-index has to pick up the current version. */
    public void updateTextFileContent(UUID nodeId, String content) throws IOException
    {
        final HttpPut request = authenticate(new HttpPut(nodesApiUri.resolve(nodeId + "/content")));
        request.setEntity(new StringEntity(content, ContentType.create("text/plain", StandardCharsets.UTF_8)));

        if (getJsonResponse(request, HttpStatus.SC_OK).isEmpty())
        {
            throw new IOException("Failed to update content of " + nodeId);
        }
    }

    /**
     * ACS-12862: deletes a node, moving it to the archive store. The v1 API answers 204 with no
     * body, so this cannot go through {@link #getJsonResponse}.
     */
    public void deleteNode(UUID nodeId) throws IOException
    {
        final HttpDelete request = authenticate(new HttpDelete(nodesApiUri.resolve(nodeId.toString())));

        try (CloseableHttpResponse response = client.execute(request))
        {
            final int status = response.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_NO_CONTENT)
            {
                throw new IOException("Failed to delete " + nodeId + ", status " + status);
            }
        }
    }

    /** ACS-12862: extracted from searchForFiles so all three search entry points share it. */
    private Optional<Set<String>> doSearch(HttpPost searchRequest, String language, String query) throws IOException
    {
        searchRequest.setEntity(new StringEntity(searchQuery(language, query), ContentType.APPLICATION_JSON));

        final Optional<Map<?, ?>> searchResult = getJsonResponse(searchRequest, HttpStatus.SC_OK);

        final Optional<Collection<?>> possibleEntries = searchResult
                .map(r -> r.get("list"))
                .filter(Map.class::isInstance).map(Map.class::cast)
                .map(m -> m.get("entries"))
                .filter(Collection.class::isInstance).map(c -> (Collection<?>) c);

        if (possibleEntries.isEmpty())
        {
            return Optional.empty();
        }

        final Collection<?> entries = possibleEntries.get();
        final Set<String> names = entries
                .stream()
                .filter(Map.class::isInstance).map(Map.class::cast)
                .map(m -> m.get("entry"))
                .filter(Map.class::isInstance).map(Map.class::cast)
                .map(m -> m.get("name"))
                .filter(String.class::isInstance).map(String.class::cast)
                .collect(Collectors.toUnmodifiableSet());

        return Optional.of(names);
    }

    /** ACS-12862: builds the children endpoint for a given parent node id or alias. */
    private URI childrenUriFor(String parentId)
    {
        return nodesApiUri.resolve(parentId + "/children");
    }

    /** ACS-12862: pulls the node id out of a v1 nodes API response. */
    private UUID extractNodeId(Optional<Map<?, ?>> response)
    {
        return response
                .map(r -> r.get("entry"))
                .filter(Map.class::isInstance).map(Map.class::cast)
                .map(e -> e.get("id"))
                .filter(String.class::isInstance).map(String.class::cast)
                .map(UUID::fromString)
                .orElseThrow();
    }

    private Map<?, ?> executeAndGetResponseMap(HttpPost httpPost, HttpClientContext httpCtx) throws IOException
    {
        try (CloseableHttpResponse response = client.execute(httpPost, httpCtx))
        {
            String responseBody = EntityUtils.toString(response.getEntity());
            return gson.fromJson(responseBody, Map.class);
        }
    }

    private <T extends HttpMessage> T authenticate(T msg)
    {
        msg.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=");
        return msg;
    }

    /** ACS-12862: basic auth as an arbitrary user, so searches can be run with their permissions. */
    private <T extends HttpMessage> T authenticateAs(T msg, String user, String password)
    {
        final String token = Base64.getEncoder()
                .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
        msg.setHeader("Authorization", "Basic " + token);
        return msg;
    }

    private Optional<Map<?, ?>> getJsonResponse(HttpUriRequest request, int requiredStatusCode) throws IOException
    {
        try (CloseableHttpResponse response = client.execute(request))
        {
            if (response.getStatusLine().getStatusCode() != requiredStatusCode)
            {
                return Optional.empty();
            }

            final ContentType contentType = ContentType.parse(response.getEntity().getContentType().getValue());
            if (!ContentType.APPLICATION_JSON.getMimeType().equals(contentType.getMimeType()))
            {
                return Optional.empty();
            }

            return Optional.of(gson.fromJson(EntityUtils.toString(response.getEntity()), Map.class));
        }
    }

    /** ACS-12862: language is now a parameter, and quotes are escaped for the category query. */
    private String searchQuery(String language, String query)
    {
        return "{\"query\":{\"language\":\"" + language + "\",\"query\":\"" + query.replace("\"", "\\\"") + "\"}}";
    }
}