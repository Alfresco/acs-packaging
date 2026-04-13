package org.alfresco.rest.mtls.solr;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.alfresco.rest.MtlsRestTest;

@ContextConfiguration("classpath:alfresco-mtls-solr-context.xml")
public class MtlsSolrTest extends MtlsRestTest
{
    @Test
    public void checkIfMtlsIsEnabledForSearchEngine()
    {
        CloseableHttpClient client = HttpClients.createMinimal();
        Assert.assertThrows(SSLHandshakeException.class,
                () -> client.execute(new HttpGet(mtlsTestProperties.getSearchEngineMtlsUrl())));
    }
}

