package org.alfresco.rest.mtls.elasticsearch;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.alfresco.rest.MtlsRestTest;

@ContextConfiguration("classpath:alfresco-mtls-elasticsearch-context.xml")
public class MtlsElasticsearchTest extends MtlsRestTest
{
    @Test
    public void checkIfMtlsIsEnabledForSearchEngine()
    {
        CloseableHttpClient client = HttpClients.createMinimal();
        Assert.assertThrows(HttpHostConnectException.class,
                () -> client.execute(new HttpGet(mtlsTestProperties.getSearchEngineMtlsUrl())));
    }
}

