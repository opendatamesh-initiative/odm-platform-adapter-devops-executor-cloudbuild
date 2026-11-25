package org.opendatamesh.platform.up.executor.cloudbuild.clients.google.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.GoogleApiTransport;
import org.opendatamesh.platform.up.executor.cloudbuild.config.CloudBuildProperties;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.BadRequestException;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.AccessToken;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.ServiceAccountKey;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.GoogleStub;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.TestConfigs;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Service-account-key mode of BDMD-5810: the client against the Google stub, without the application. */
class GoogleAccessTokenClientImplTest {

    private static final String CLIENT_EMAIL = "deployer@demo-project.iam.gserviceaccount.com";

    private GoogleStub stub;
    private GoogleAccessTokenClientImpl client;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        stub = new GoogleStub();
        Map<String, String> config = TestConfigs.valid(stub.baseUrl());
        config.put("executor.cloudbuild.auth.mode", "service-account-key");
        CloudBuildProperties properties = TestConfigs.cloudBuild(config);
        client = new GoogleAccessTokenClientImpl(new GoogleApiTransport(properties, new ObjectMapper()), properties);
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @AfterEach
    void tearDown() {
        stub.close();
    }

    /**
     * Feature: Authenticate to Google
     *
     * Scenario: A service-account key is exchanged for a token
     *   Given the executor runs in service-account-key mode
     *   When DevOps starts a task with a base64 service-account key in the key header
     *   Then the executor sends a JWT signed with that key to the token endpoint
     *   And calls Cloud Build with the token it receives
     */
    @Test
    void whenKeyHeaderThenSignedJwtExchangedAtConfiguredEndpoint() {
        AccessToken token = client.tokenFor(new ServiceAccountKey(base64(serviceAccountKey())));

        assertEquals(GoogleStub.KEY_TOKEN, token.value());
        Map<String, String> form = stub.lastRequest("POST", GoogleStub.OAUTH_TOKEN).form();
        assertEquals("urn:ietf:params:oauth:grant-type:jwt-bearer", form.get("grant_type"));
        DecodedJWT assertion = JWT.require(Algorithm.RSA256((RSAPublicKey) keyPair.getPublic(), null))
                .withIssuer(CLIENT_EMAIL)
                .withAudience(stub.baseUrl() + GoogleStub.OAUTH_TOKEN)
                .build()
                .verify(form.get("assertion"));
        assertEquals("https://www.googleapis.com/auth/cloud-platform", assertion.getClaim("scope").asString());
        assertEquals("kid-1", assertion.getKeyId());
        assertTrue(stub.requests("GET", GoogleStub.METADATA_TOKEN).isEmpty());
    }

    @Test
    void whenRawJsonKeyThenAcceptedToo() {
        assertEquals(GoogleStub.KEY_TOKEN, client.tokenFor(new ServiceAccountKey(serviceAccountKey())).value());
    }

    /**
     * Scenario: A malformed key is refused without echoing it
     *   Given the executor runs in service-account-key mode
     *   When the key header holds something that is not a service-account key
     *   Then the answer is 400 and the message does not contain the header value
     */
    @Test
    void whenKeyMalformedThenBadRequestWithoutEchoingIt() {
        String notAKey = base64("{\"type\":\"authorized_user\",\"refresh_token\":\"very-secret-refresh-token\"}");

        BadRequestException failure = assertThrows(BadRequestException.class, () -> client.tokenFor(new ServiceAccountKey(notAKey)));

        assertTrue(failure.getMessage().contains("x-odm-gcp-service-account-key"), failure.getMessage());
        assertFalse(failure.getMessage().contains("very-secret-refresh-token"));
        assertFalse(failure.getMessage().contains(notAKey));
        assertTrue(stub.all().isEmpty());
    }

    @Test
    void whenGoogleRefusesTheKeyThenBadGatewayQuotingGoogle() {
        stub.reply("POST", GoogleStub.OAUTH_TOKEN, 400, "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid JWT Signature.\"}");

        Exception failure = assertThrows(Exception.class, () -> client.tokenFor(new ServiceAccountKey(base64(serviceAccountKey()))));

        assertTrue(failure.getMessage().contains("invalid_grant"), failure.getMessage());
        assertTrue(failure.getMessage().contains(CLIENT_EMAIL), failure.getMessage());
    }

    private String serviceAccountKey() {
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        ObjectNode key = new ObjectMapper().createObjectNode()
                .put("type", "service_account")
                .put("project_id", "demo-project")
                .put("private_key_id", "kid-1")
                .put("private_key", pem)
                .put("client_email", CLIENT_EMAIL)
                .put("token_uri", "https://attacker.example/token");
        return key.toString();
    }

    private static String base64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }
}
