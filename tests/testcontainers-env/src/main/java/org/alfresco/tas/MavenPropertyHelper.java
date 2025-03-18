package org.alfresco.tas;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

import org.testng.Assert;

/** A helper class with methods to access properties from Maven pom files. */
public class MavenPropertyHelper
{
    /** The relative path to the maven property file resource. */
    private static final String MAVEN_PROPERTIES_FILE = "maven.properties";

    /** Private constructor for helper class. */
    private MavenPropertyHelper()
    {}

    /**
     * Load the value of a property from Maven.
     *
     * @param key
     *            The key to look up.
     * @return The value as a string.
     */
    public static String getMavenProperty(String key)
    {
        return loadMavenProperties().getProperty(key);
    }

    /**
     * Load all the properties from Maven.
     *
     * @return The properties.
     */
    public static Properties loadMavenProperties()
    {
        Properties properties = new Properties();
        try (InputStream inputStream = MavenPropertyHelper.class.getClassLoader().getResourceAsStream(MAVEN_PROPERTIES_FILE))
        {
            Reader reader = new InputStreamReader(inputStream);
            properties.load(reader);
        }
        catch (Exception e)
        {
            Assert.fail("Unable to load maven.properties file ");
        }
        return properties;
    }
}
