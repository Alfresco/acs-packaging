package org.alfresco.elasticsearch;

import java.io.FileReader;
import java.util.Properties;

import org.testng.Assert;

/** A helper class with methods to access properties from Maven pom files. */
public class MavenPropertyHelper
{
    /** The location of the .env file (relative to the root of this maven submodule). */
    private static final String MAVEN_PROPERTIES_FILE = "target/classes/maven.properties";

    /** Private constructor for helper class. */
    private MavenPropertyHelper()
    {
    }

    /**
     * Load the value of a property from Maven.
     *
     * @param key The key to look up.
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
        try (FileReader reader = new FileReader(MAVEN_PROPERTIES_FILE))
        {
            properties.load(reader);
        }
        catch (Exception e)
        {
            Assert.fail("Unable to load maven.properties file ");
        }
        return properties;
    }
}
