package org.alfresco.mtls;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;


import com.google.common.util.concurrent.RateLimiter;
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
import org.testcontainers.containers.DockerComposeContainer;
import org.testng.annotations.Test;

//TODO: remove it?
public class TransformServiceWithMTLSIntegrationTest
{
    final int HTTP_TIMEOUT_MS = 5_000;
    final CloseableHttpClient client = HttpClientBuilder
            .create()
            .setDefaultRequestConfig(
                    RequestConfig.copy(RequestConfig.DEFAULT)
                                 .setConnectionRequestTimeout(HTTP_TIMEOUT_MS)
                                 .setSocketTimeout(HTTP_TIMEOUT_MS)
                                 .setConnectionRequestTimeout(HTTP_TIMEOUT_MS)
                                 .setRedirectsEnabled(false)
                                 .build())
            .build();

    final Gson gson = new Gson();

    private final URI searchApiUri = URI.create("localhost:8080/alfresco/api/-default-/public/search/versions/1/search");
    private final URI fileUploadApiUri = URI.create("localhost:8080/alfresco/api/-default-/public/alfresco/versions/1/nodes/-my-/children");
    private final URI searchServiceAdminAppUri = URI.create("localhost:8080/alfresco/s/enterprise/admin/admin-searchservice");
    private final URI uploadLicenseAdminApiUri = URI.create("localhost:8080/alfresco/s/enterprise/admin/admin-license-upload");
    private final URI serverApiUri = URI.create("localhost:8080/alfresco/service/api/server");

    @Test
    public void testRenditionWithMTLSEnabledTest() throws IOException, InterruptedException
    {
        DockerComposeContainer env = new DockerComposeContainer(new File("/Users/Kacper.Magdziarz@hyland.com/IdeaProjects/acs-packaging/tests/environment/test2-docker-compose.yml"));
        env.start();

        waitUntilServerIsUp(Duration.ofMinutes(5));

        UUID uuid = uploadFile(getClass().getResource("testFile.txt"), "testFile.txt");
        System.out.println(uuid);

        wait(Duration.ofMinutes(10).toMillis());
    }

    private <T extends HttpMessage> T authenticate(T msg)
    {
        msg.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=");
        return msg;
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

    public String getRendition(UUID id)
    {
        return "";
    }

    private void waitUntilServerIsUp(Duration timeout)
    {
        waitFor("Reaching the point where server is up and running.", timeout, () -> {
            try
            {
                return isServerUp();
            } catch (IOException e)
            {
                return false;
            }
        });
    }

    public static void waitFor(String description, final Duration timeout, final BooleanSupplier condition)
    {
        final RateLimiter rateLimiter = RateLimiter.create(5);
        final long numberOfIterations = Math.max(1L, timeout.getSeconds() * (long) rateLimiter.getRate());
        for (long i = 0; i < numberOfIterations; i++)
        {
            rateLimiter.acquire();
            if (condition.getAsBoolean())
            {
                return;
            }
        }
        throw new RuntimeException("Failed to wait for " + description + ".");
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
}
