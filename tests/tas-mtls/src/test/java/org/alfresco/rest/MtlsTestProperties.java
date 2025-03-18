package org.alfresco.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
@PropertySources({@PropertySource({"classpath:default.properties"})})
public class MtlsTestProperties
{
    @Value("${testClient.keystore.location}")
    private String keystoreLocation;
    @Value("${testClient.keystore.password}")
    private String keystorePassword;
    @Value("${testClient.keystore.type}")
    private String keystoreType;
    @Value("${testClient.truststore.location}")
    private String truststoreLocation;
    @Value("${testClient.truststore.password}")
    private String truststorePassword;
    @Value("${testClient.truststore.type}")
    private String truststoreType;
    @Value("${testClient.disableHostnameVerification}")
    private boolean disableHostnameVerification;

    public MtlsTestProperties()
    {}

    public String getKeystoreLocation()
    {
        return keystoreLocation;
    }

    public void setKeystoreLocation(String keystoreLocation)
    {
        this.keystoreLocation = keystoreLocation;
    }

    public String getKeystorePassword()
    {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword)
    {
        this.keystorePassword = keystorePassword;
    }

    public String getKeystoreType()
    {
        return keystoreType;
    }

    public void setKeystoreType(String keystoreType)
    {
        this.keystoreType = keystoreType;
    }

    public String getTruststoreLocation()
    {
        return truststoreLocation;
    }

    public void setTruststoreLocation(String truststoreLocation)
    {
        this.truststoreLocation = truststoreLocation;
    }

    public String getTruststorePassword()
    {
        return truststorePassword;
    }

    public void setTruststorePassword(String truststorePassword)
    {
        this.truststorePassword = truststorePassword;
    }

    public String getTruststoreType()
    {
        return truststoreType;
    }

    public void setTruststoreType(String truststoreType)
    {
        this.truststoreType = truststoreType;
    }

    public boolean isDisableHostnameVerification()
    {
        return disableHostnameVerification;
    }

    public void setDisableHostnameVerification(boolean disableHostnameVerification)
    {
        this.disableHostnameVerification = disableHostnameVerification;
    }
}
