package org.alfresco.elasticsearch.basicAuth;

import java.io.IOException;

import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.tas.SearchEngineType;
import org.testcontainers.containers.GenericContainer;

/**
 * ACS Stack Docker Compose initializer with Basic Authentication for Search Engine service (Opensearch or Elasticsearch).
 */
public class AlfrescoStackInitializerESBasicAuth extends AlfrescoStackInitializer
{

    // Default Elasticsearch credentials
    private static final String SEARCH_ENGINE_USERNAME = "elastic";
    private static final String SEARCH_ENGINE_PASSWORD = "bob123";
    private static final String OPENSEARCH_TEST_ROLE = "test_role";

    @Override
    public void configureSecuritySettings(GenericContainer searchEngineContainer)
    {
        SearchEngineType usedEngine = getImagesConfig().getSearchEngineType();

        if(SearchEngineType.OPENSEARCH_ENGINE.equals(usedEngine))
        {
            configureNewUser(searchEngineContainer);
        }
    }

    private void configureNewUser(GenericContainer opensearchContainer)
    {
        try
        {
            //Using password hash for setting up test only
            String passwordHash = hashPassword(opensearchContainer, SEARCH_ENGINE_PASSWORD);

            addNewUser(opensearchContainer, SEARCH_ENGINE_USERNAME, passwordHash);
            addNewRole(opensearchContainer, OPENSEARCH_TEST_ROLE, CUSTOM_ALFRESCO_INDEX);
            addNewRoleMapping(opensearchContainer, OPENSEARCH_TEST_ROLE, SEARCH_ENGINE_USERNAME);
            applyNewSecurityConfigs(opensearchContainer);
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private String hashPassword(GenericContainer opensearchContainer, String password) throws IOException, InterruptedException
    {
        return opensearchContainer.execInContainer("/usr/share/opensearch/plugins/opensearch-security/tools/hash.sh", "-p", password)
                .getStdout().strip();
    }

    private void applyNewSecurityConfigs(GenericContainer opensearchContainer) throws IOException, InterruptedException
    {
        opensearchContainer.execInContainer("sh", "-c", "/usr/share/opensearch/plugins/opensearch-security/tools/securityadmin.sh " +
                "-cd /usr/share/opensearch/plugins/opensearch-security/securityconfig " +
                "-icl -nhnv " +
                "-cert /usr/share/opensearch/config/kirk.pem " +
                "-cacert /usr/share/opensearch/config/root-ca.pem " +
                "-key /usr/share/opensearch/config/kirk-key.pem"
        );
    }

    private void addNewRoleMapping(GenericContainer opensearchContainer, String role, String username) throws IOException, InterruptedException
    {
        opensearchContainer.execInContainer(
                "sh", "-c", "echo '\n\n" + newOpensearchRoleMapping(role, username) +"' >> /usr/share/opensearch/plugins/opensearch-security/securityconfig/roles_mapping.yml"
        );
    }

    private void addNewRole(GenericContainer opensearchContainer, String role, String index) throws IOException, InterruptedException
    {
        opensearchContainer.execInContainer(
                "sh", "-c", "echo '\n\n" + newOpensearchRule(role, index) +"' >> /usr/share/opensearch/plugins/opensearch-security/securityconfig/roles.yml"
        );
    }

    private void addNewUser(GenericContainer opensearchContainer, String username, String passwordHash) throws IOException, InterruptedException
    {
        opensearchContainer.execInContainer(
                "sh", "-c", "echo '\n\n" + newOpensearchUser(username, passwordHash) +"' >> /usr/share/opensearch/plugins/opensearch-security/securityconfig/internal_users.yml"
        );
    }

    private String newOpensearchUser(String username, String passwordHash)
    {
        return username +":\n" +
                "  hash: \"" + passwordHash +"\"\n" +
                "  reserved: false\n" +
                "  backend_roles:\n" +
                "  - \"all_access\"\n" +
                "  description: \"New user for testing purposes\"";
    }

    private String newOpensearchRule(String role, String indexName)
    {
        return role + ":\n" +
                "  cluster_permissions:\n" +
                "    - cluster_all\n" +
                "  index_permissions:\n" +
                "    - index_patterns:\n" +
                "      - \"" + indexName + "\"\n" +
                "      allowed_actions:\n" +
                "        - \"*\"\n";
    }

    private String newOpensearchRoleMapping(String role, String user)
    {
        return role + ":\n" +
                "  reserved: true\n" +
                "  users:\n" +
                "  - \"" + user + "\"";
    }

    @Override
    protected GenericContainer createLiveIndexingContainer()
    {
        GenericContainer container = super.createLiveIndexingContainer();
        container.withEnv("SPRING_ELASTICSEARCH_REST_USERNAME", SEARCH_ENGINE_USERNAME);
        container.withEnv("SPRING_ELASTICSEARCH_REST_PASSWORD", SEARCH_ENGINE_PASSWORD);
        return container;
    }

    @Override
    protected GenericContainer createSearchEngineContainer()
    {
        SearchEngineType usedEngine = getImagesConfig().getSearchEngineType();

        if(SearchEngineType.OPENSEARCH_ENGINE.equals(usedEngine))
        {
            return super.createOpensearchContainer()
                    .withEnv("plugins.security.disabled", "false")
                    .withEnv("plugins.security.ssl.http.enabled", "false");
        }
        else
        {
            return super.createElasticContainer()
                    .withEnv("xpack.security.enabled", "true")
                    .withEnv("xpack.security.transport.ssl.enabled", "true")
                    .withEnv("xpack.security.http.ssl.enabled", "true")
                    .withEnv("ELASTIC_PASSWORD", SEARCH_ENGINE_PASSWORD);
        }
    }

    @Override
    protected GenericContainer createAlfrescoContainer()
    {
        GenericContainer container = super.createAlfrescoContainer();
        String javaOpts = (String) container.getEnvMap().get("JAVA_OPTS");
        javaOpts = javaOpts + " -Delasticsearch.user=" + SEARCH_ENGINE_USERNAME + " " +
                "-Delasticsearch.password=" + SEARCH_ENGINE_PASSWORD;
        container.getEnvMap().put("JAVA_OPTS", javaOpts);
        return container;
    }
}
