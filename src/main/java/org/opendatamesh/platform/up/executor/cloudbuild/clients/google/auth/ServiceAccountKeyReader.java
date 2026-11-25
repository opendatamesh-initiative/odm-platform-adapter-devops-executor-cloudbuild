package org.opendatamesh.platform.up.executor.cloudbuild.clients.google.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.BadRequestException;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.ServiceAccountKey;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Reads a service-account JSON key and signs the JWT assertion of the OAuth 2.0 JWT bearer flow. Any malformed key is
 * one {@link BadRequestException} whose message names the header only: neither the key nor a parser message (which
 * could quote it) is ever put in the error.
 */
final class ServiceAccountKeyReader {

    static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
    private static final Duration ASSERTION_LIFETIME = Duration.ofHours(1);
    private static final String SERVICE_ACCOUNT_TYPE = "service_account";
    private static final String PEM_BEGIN = "-----BEGIN PRIVATE KEY-----";
    private static final String PEM_END = "-----END PRIVATE KEY-----";

    record ParsedKey(String clientEmail, String privateKeyId, RSAPrivateKey privateKey) {

        @Override
        public String toString() {
            return "ParsedKey[clientEmail=" + clientEmail + ", privateKeyId=" + privateKeyId + ", privateKey=***]";
        }
    }

    private final ObjectMapper objectMapper;
    private final String keyHeader;

    ServiceAccountKeyReader(ObjectMapper objectMapper, String keyHeader) {
        this.objectMapper = objectMapper;
        this.keyHeader = keyHeader;
    }

    ParsedKey read(ServiceAccountKey key) {
        JsonNode json = parseJson(decode(key.encodedKey()));
        requireServiceAccount(json);
        return new ParsedKey(json.get("client_email").asText(), json.path("private_key_id").asText(""),
                privateKey(json.get("private_key").asText()));
    }

    String signAssertion(ParsedKey key, String audience, Instant now) {
        var builder = JWT.create()
                .withIssuer(key.clientEmail())
                .withAudience(audience)
                .withClaim("scope", CLOUD_PLATFORM_SCOPE)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(ASSERTION_LIFETIME)));
        if (!key.privateKeyId().isBlank()) {
            builder.withKeyId(key.privateKeyId());
        }
        return builder.sign(Algorithm.RSA256(null, key.privateKey()));
    }

    private String decode(String encodedKey) {
        String trimmed = encodedKey.trim();
        if (trimmed.startsWith("{")) {
            return trimmed;
        }
        try {
            return new String(Base64.getMimeDecoder().decode(trimmed), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw invalidKey();
        }
    }

    private JsonNode parseJson(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node == null || !node.isObject()) {
                throw invalidKey();
            }
            return node;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw invalidKey();
        }
    }

    private void requireServiceAccount(JsonNode json) {
        if (!SERVICE_ACCOUNT_TYPE.equals(json.path("type").asText())
                || json.path("client_email").asText("").isBlank()
                || json.path("private_key").asText("").isBlank()) {
            throw invalidKey();
        }
    }

    private RSAPrivateKey privateKey(String pem) {
        try {
            String base64 = pem.replace(PEM_BEGIN, "").replace(PEM_END, "").replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(base64);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw invalidKey();
        }
    }

    private BadRequestException invalidKey() {
        return new BadRequestException("The service-account key in header " + keyHeader
                + " is not a valid JSON key of a GCP service account (base64 on one line, or raw JSON)");
    }
}
