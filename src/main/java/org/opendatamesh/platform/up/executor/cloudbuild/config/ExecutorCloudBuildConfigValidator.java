package org.opendatamesh.platform.up.executor.cloudbuild.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.AuthMode;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Stops the service at boot on invalid configuration, naming the offending key. */
@ApplicationScoped
public class ExecutorCloudBuildConfigValidator {

    private static final String PREFIX = "executor.cloudbuild.";
    private static final Pattern PROJECT = Pattern.compile("[a-z][a-z0-9-]{4,28}[a-z0-9]");
    private static final Pattern LOCATION = Pattern.compile("[a-z0-9-]+");
    private static final Pattern SECRET_TYPE = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final String SECRET_HEADER_SEPARATOR = "executor-secret";
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final CloudBuildProperties properties;

    public ExecutorCloudBuildConfigValidator(CloudBuildProperties properties) {
        this.properties = properties;
    }

    void onStart(@Observes StartupEvent event) {
        validate();
    }

    public void validate() {
        validateTarget();
        validateAuth();
        validateHttp();
        validateLogs();
        validateEndpoints();
    }

    private void validateTarget() {
        require(PROJECT.matcher(properties.project()).matches(), "project",
                "must be a GCP project id (6 to 30 lowercase letters, digits or hyphens, starting with a letter)");
        require(LOCATION.matcher(properties.location()).matches(), "location",
                "must be global or a Cloud Build region, for example europe-west8");
    }

    private void validateAuth() {
        String secretType = properties.auth().keySecretType();
        require(SECRET_TYPE.matcher(secretType).matches(), "auth.key-secret-type",
                "must be lowercase letters and digits separated by single hyphens, for example gcp-service-account-key");
        require(!secretType.contains(SECRET_HEADER_SEPARATOR), "auth.key-secret-type",
                "must not contain '" + SECRET_HEADER_SEPARATOR + "', which DevOps and the agent use to split secret headers");
        Optional<String> impersonate = properties.auth().impersonateServiceAccount().filter(value -> !value.isBlank());
        if (impersonate.isPresent()) {
            require(properties.auth().mode() == AuthMode.WORKLOAD_IDENTITY, "auth.impersonate-service-account",
                    "is only used in workload-identity mode; remove it or set auth.mode to workload-identity");
            require(EMAIL.matcher(impersonate.get()).matches(), "auth.impersonate-service-account",
                    "must be the email of a service account");
        }
    }

    private void validateHttp() {
        requirePositive(properties.http().connectTimeout(), "http.connect-timeout");
        requirePositive(properties.http().requestTimeout(), "http.request-timeout");
        require(properties.http().readRetries() >= 0, "http.read-retries", "must be zero or more");
        require(!properties.http().retryBackoff().isNegative(), "http.retry-backoff", "must not be negative");
    }

    private void validateLogs() {
        require(properties.logs().maxSize().asLongValue() > 0, "logs.max-size", "must be positive");
        require(properties.logs().cloudLoggingPageSize() > 0, "logs.cloud-logging-page-size", "must be positive");
    }

    private void validateEndpoints() {
        CloudBuildProperties.Endpoints endpoints = properties.endpoints();
        Map.of("endpoints.cloud-build", endpoints.cloudBuild(),
                        "endpoints.logging", endpoints.logging(),
                        "endpoints.storage", endpoints.storage(),
                        "endpoints.oauth-token", endpoints.oauthToken(),
                        "endpoints.iam-credentials", endpoints.iamCredentials(),
                        "endpoints.metadata", endpoints.metadata())
                .forEach(this::requireHttpUrl);
    }

    private void requireHttpUrl(String key, String value) {
        require(isHttpUrl(value), key, "must be an absolute http or https URL");
    }

    private static boolean isHttpUrl(String value) {
        try {
            URI uri = URI.create(value);
            return uri.isAbsolute() && uri.getHost() != null
                    && ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void requirePositive(Duration value, String key) {
        require(value != null && !value.isNegative() && !value.isZero(), key, "must be positive");
    }

    private static void require(boolean condition, String key, String rule) {
        if (!condition) {
            throw new IllegalStateException(PREFIX + key + ": " + rule);
        }
    }
}
