/*
 * Copyright (c) 2016-2018 Contributors to the Eclipse Foundation
 *
 *  See the NOTICE file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.eclipse.microprofile.jwt.tck.config;

import org.eclipse.microprofile.jwt.tck.container.jaxrs.TCKApplication;
import org.eclipse.microprofile.jwt.tck.util.MpJwtTestVersion;
import org.eclipse.microprofile.jwt.tck.util.TokenUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.HashMap;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.microprofile.jwt.tck.TCKConstants.TEST_GROUP_CONFIG;

/**
 * Validate that if the token does not have an exp claim that an expiry date is not enforced
 */
public class NoValidationNoExpTest extends Arquillian {
    /**
     * The base URL for the container under test
     */
    @ArquillianResource
    private URL baseURL;

    /**
     * The token used by the test
     */
    private static String token;

    /**
     * Create a CDI aware base web application archive that includes an embedded PEM public key
     * that is included as the mp.jwt.verify.publickey property.
     * The root url is /
     * @return the base base web application archive
     * @throws Exception - on resource failure
     */
    @Deployment()
    public static WebArchive createDeployment() throws Exception {
        PrivateKey privateKey = TokenUtils.readPrivateKey("/privateKey4k.pem");
        String kid = "/privateKey4k.pem";
        HashMap<String, Long> timeClaims = new HashMap<>();
        token = TokenUtils.generateTokenString(privateKey, kid, "/TokenNoExp.json", Collections.singleton(TokenUtils.InvalidClaims.EXP), timeClaims);

        URL config = PublicKeyAsPEMTest.class.getResource("/META-INF/microprofile-config.properties");

        WebArchive webArchive = ShrinkWrap
            .create(WebArchive.class, "NoValidationNoExpTest.war")
            .addAsManifestResource(new StringAsset(MpJwtTestVersion.MPJWT_V_1_1.name()), MpJwtTestVersion.MANIFEST_NAME)
            // Include the token for inspection by ApplicationArchiveProcessor
            .add(new StringAsset(token), "MP-JWT")
            .addClass(PublicKeyEndpoint.class)
            .addClass(TCKApplication.class)
            .addClass(SimpleTokenUtils.class)
            .addAsWebInfResource("beans.xml", "beans.xml")
            .addAsManifestResource(config, "microprofile-config.properties");
        System.out.printf("WebArchive: %s\n", webArchive.toString(true));
        return webArchive;
    }

    @RunAsClient
    @Test(groups = TEST_GROUP_CONFIG,
        description = "Validate that JWK without exp returns HTTP_OK")
    public void testNotRequiredExpMissingIgnored() throws Exception {
        Reporter.log("testNotRequiredExpMissingIgnored, expect HTTP_OK");

        String uri = baseURL.toExternalForm() + "endp/verifyMissingExpIsOk";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
            .target(uri)
            ;
        Response response = echoEndpointTarget.request(APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer "+token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

}
