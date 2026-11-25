package org.opendatamesh.platform.up.executor.cloudbuild.clients.google.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.GoogleApiTransport;
import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.GoogleApiUnreachableException;
import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.GoogleRequest;
import org.opendatamesh.platform.up.executor.cloudbuild.config.CloudBuildProperties;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.BadGatewayException;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.BadRequestException;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.NotFoundException;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.AccessToken;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.ExecutorIdentity;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.GcpCredential;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.ServiceAccountKey;

import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Obtains a Google access token for one request. A service-account key is exchanged at the configured OAuth token
 * endpoint (never at the key's own {@code token_uri}); the executor's identity comes from the metadata server and is
 * exchanged through IAM Credentials when a service account is impersonated. Tokens are never cached or logged.
 */
@ApplicationScoped
public class GoogleAccessTokenClientImpl implements GoogleAccessTokenClient {

    private static final String OAUTH_API = "Google OAuth";
    private static final String METADATA_API = "GCP metadata server";
    private static final String IAM_CREDENTIALS_API = "IAM Credentials";
    private static final String JWT_BEARER_GRANT = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    private static final String METADATA_TOKEN_PATH = "/computeMetadata/v1/instance/service-accounts/default/token";
    private static final String IMPERSONATED_TOKEN_LIFETIME = "3600s";

    private final GoogleApiTransport transport;
    private final CloudBuildProperties.Endpoints endpoints;
    private final ServiceAccountKeyReader keyReader;

    public GoogleAccessTokenClientImpl(GoogleApiTransport transport, CloudBuildProperties properties) {
        this.transport = transport;
        this.endpoints = properties.endpoints();
        this.keyReader = new ServiceAccountKeyReader(transport.objectMapper(), properties.keyHeader());
    }

    @Override
    public AccessToken tokenFor(GcpCredential credential) {
        return switch (credential) {
            case ServiceAccountKey key -> exchangeKey(key);
            case ExecutorIdentity identity -> identity.impersonate()
                    .map(serviceAccount -> impersonate(executorToken(), serviceAccount))
                    .orElseGet(this::executorToken);
        };
    }

    private AccessToken exchangeKey(ServiceAccountKey key) {
        ServiceAccountKeyReader.ParsedKey parsed = keyReader.read(key);
        String assertion = keyReader.signAssertion(parsed, endpoints.oauthToken(), Instant.now());
        GoogleRequest request = new GoogleRequest(OAUTH_API,
                "exchange the key of service account " + parsed.clientEmail() + " for a token", endpoints.oauthToken());
        JsonNode response = asGatewayFailure(() -> transport.postForm(request, Map.of("grant_type", JWT_BEARER_GRANT, "assertion", assertion)));
        return token(response, "access_token", request);
    }

    private AccessToken executorToken() {
        GoogleRequest request = new GoogleRequest(METADATA_API, "read the executor's token",
                endpoints.metadata() + METADATA_TOKEN_PATH);
        try {
            return token(asGatewayFailure(() -> transport.readJsonWithHeaders(request, Map.of("Metadata-Flavor", "Google"))),
                    "access_token", request);
        } catch (GoogleApiUnreachableException e) {
            throw new BadGatewayException(e.getMessage() + ": workload-identity mode needs the executor to run on GCP "
                    + "(Cloud Run, or GKE with Workload Identity); outside GCP use service-account-key mode", e);
        }
    }

    private AccessToken impersonate(AccessToken executorToken, String serviceAccount) {
        GoogleRequest request = new GoogleRequest(IAM_CREDENTIALS_API,
                "obtain a token for service account " + serviceAccount + " (needs roles/iam.serviceAccountTokenCreator)",
                endpoints.iamCredentials() + "/v1/projects/-/serviceAccounts/"
                        + GoogleApiTransport.encodePathSegment(serviceAccount) + ":generateAccessToken");
        ObjectNode body = transport.objectMapper().createObjectNode();
        body.putArray("scope").add(ServiceAccountKeyReader.CLOUD_PLATFORM_SCOPE);
        body.put("lifetime", IMPERSONATED_TOKEN_LIFETIME);
        return token(asGatewayFailure(() -> transport.queryJson(request, executorToken, body)), "accessToken", request);
    }

    /**
     * A 400 or 404 from an auth endpoint (revoked key, unknown impersonated account) is a credential or configuration
     * failure, not a bad task request: it must reach DevOps as 502, never as a 4xx that DevOps reads as "run gone".
     */
    private static JsonNode asGatewayFailure(Supplier<JsonNode> call) {
        try {
            return call.get();
        } catch (BadRequestException | NotFoundException e) {
            throw new BadGatewayException(e.getMessage(), e);
        }
    }

    private static AccessToken token(JsonNode response, String field, GoogleRequest request) {
        String value = response == null ? "" : response.path(field).asText("");
        if (value.isBlank()) {
            throw new BadGatewayException(request.api() + " returned no access token when trying to " + request.action());
        }
        return new AccessToken(value);
    }
}
