package org.alfresco.rest;

import org.alfresco.rest.core.RestProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:default.properties")
public class ClusterProperties extends RestProperties
{
    @Value("${alfresco.server1:localhost}")
    private String server1Host;

    @Value("${alfresco.server2:localhost}")
    private String server2Host;

    @Value("${alfresco.port1:8081}")
    private int port1;

    @Value("${alfresco.port2:8082}")
    private int port2;

    public String getServer1Host()
    {
        return server1Host;
    }

    public String getServer2Host()
    {
        return server2Host;
    }

    public int getPort1()
    {
        return port1;
    }

    public int getPort2()
    {
        return port2;
    }

    public String getServer1URI()
    {
        return String.format("%s://%s", envProperty().getScheme(), getServer1Host());
    }

    public String getServer2URI()
    {
        return String.format("%s://%s", envProperty().getScheme(), getServer2Host());
    }
}
