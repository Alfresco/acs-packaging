package org.alfresco.elasticsearch;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

/** TODO Decide whether this test class needs to be in a maven submodule of it's own. */
@ContextConfiguration ("classpath:alfresco-elasticsearch-context.xml")
public class ReindexingTests extends AbstractTestNGSpringContextTests
{
    @Test
    public void testReindexerIndexesSystemDocuments()
    {
        // GIVEN
        // Check a particular system document is not indexed.

        // WHEN
        // Run reindexer.

        // THEN
        // Check system document is indexed.
    }

    @Test
    public void testReindexerFixesBrokenIndex()
    {
        // GIVEN
        // Stop Elasticsearch.
        // Create document.
        // Start Elasticsearch.
        // Check document not indexed.

        // WHEN
        // Run reindexer.

        // THEN
        // Check document indexed.
    }

    @Test
    public void testRecreateIndex()
    {
        // GIVEN
        // Create document.
        // Stop ElasticsearchConnector.
        // Stop Alfresco.
        // Delete index.
        // Start Alfresco.

        // WHEN
        // Run reindexer.

        // THEN
        // Check document indexed.

        // TIDY
        // Restart ElasticsearchConnector.
    }
}
