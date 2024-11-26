package org.alfresco.elasticsearch.upgrade;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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

    RepoHttpClient(final URI repoBaseUri)
    {
        searchApiUri = repoBaseUri.resolve("/alfresco/api/-default-/public/search/versions/1/search");
        fileUploadApiUri = repoBaseUri.resolve("/alfresco/api/-default-/public/alfresco/versions/1/nodes/-my-/children");
        searchServiceAdminAppUri = repoBaseUri.resolve("/alfresco/s/enterprise/admin/admin-searchservice");
        uploadLicenseAdminApiUri = repoBaseUri.resolve("/alfresco/s/enterprise/admin/admin-license-upload");
        serverApiUri = repoBaseUri.resolve("/alfresco/service/api/server");
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

    public Optional<Set<String>> searchForFiles(String term) throws IOException
    {
        final HttpPost searchRequest = authenticate(new HttpPost(searchApiUri));
        searchRequest.setEntity(new StringEntity(searchQuery(term), ContentType.APPLICATION_JSON));

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

    private Map<?,?> executeAndGetResponseMap(HttpPost httpPost, HttpClientContext httpCtx) throws IOException
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

    private String searchQuery(String term)
    {
        return "{\"query\":{\"language\":\"afts\",\"query\":\"" + term + "\"}}";
    }
}
