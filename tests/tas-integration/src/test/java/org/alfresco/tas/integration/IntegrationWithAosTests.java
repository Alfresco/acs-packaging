package org.alfresco.tas.integration;

import static org.alfresco.utility.report.log.Step.STEP;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonObject;

import org.alfresco.rest.core.JsonBodyGenerator;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.model.RestHtmlResponse;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.report.Bug.Status;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

/**
 * @author Catalin Gornea
 */
public class IntegrationWithAosTests extends IntegrationTest
{
    @Bug(id = "REPO-2096", status = Status.FIXED)
    @Test(groups = { TestGroup.INTEGRATION, TestGroup.AOS, TestGroup.FULL, TestGroup.SSO })
    @TestRail(section = { TestGroup.INTEGRATION,
            TestGroup.AOS }, executionType = ExecutionType.REGRESSION, description = "Security check for AOS Protocol with External Authentification - MNT-17474")
    public void aosCSRFVulnerabilityInSSOEnvironment() throws Exception
    {
        STEP("1. Post call using RestAPI for specific AOS Security issue");
        UserModel adminUser = dataUser.getAdminUser();
        Map<String, String> headers = new HashMap<String, String>();
        String authCookie = String.format("%s:%s", adminUser.getUsername(), adminUser.getPassword());
        String authCookieEncoded = new String(Base64.encodeBase64(authCookie.getBytes()));
        headers.put("Authorization", String.format("Basic %s", authCookieEncoded));

        restAPI.configureRequestSpec().addHeaders(headers);
        JsonObject postBody = JsonBodyGenerator.defineJSON().add("method", "create url-directories:6.0.2.8164")
                .add("urldirs", "[[url=ExploitedMNT17474;meta_info=[]]]").build();
        restAPI.authenticateUser(adminUser).usingContentType(ContentType.URLENC).withAosAPI();
        RestRequest request = RestRequest.requestWithBody(HttpMethod.POST, postBody.toString(), "_vti_bin/_vti_aut/author.dll");
        restAPI.process(request);
        restAPI.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }
}
