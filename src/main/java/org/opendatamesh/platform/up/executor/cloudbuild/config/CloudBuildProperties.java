package org.opendatamesh.platform.up.executor.cloudbuild.config;

import io.quarkus.runtime.configuration.MemorySize;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.AuthMode;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildTarget;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CredentialPolicy;

import java.time.Duration;
import java.util.Optional;

/** Configuration of the Cloud Build executor ({@code executor.cloudbuild}). */
@ConfigMapping(prefix = "executor.cloudbuild")
public interface CloudBuildProperties {

    String KEY_HEADER_PREFIX = "x-odm-";

    /** GCP project of the triggers. */
    String project();

    /** Cloud Build location of the triggers: {@code global} or a region. */
    String location();

    Auth auth();

    Http http();

    Logs logs();

    Endpoints endpoints();

    default CloudBuildTarget target() {
        return new CloudBuildTarget(project(), location());
    }

    default String keyHeader() {
        return KEY_HEADER_PREFIX + auth().keySecretType();
    }

    default CredentialPolicy credentialPolicy() {
        return new CredentialPolicy(auth().mode(), keyHeader(), auth().impersonateServiceAccount().filter(s -> !s.isBlank()));
    }

    interface Auth {

        /** {@code workload-identity} or {@code service-account-key}. */
        AuthMode mode();

        /** Service account the executor's identity impersonates (workload-identity mode only). */
        Optional<String> impersonateServiceAccount();

        /** Secret type of the key: the key arrives in header {@code x-odm-<key-secret-type>}. */
        @WithDefault("gcp-service-account-key")
        String keySecretType();
    }

    interface Http {

        @WithDefault("5s")
        Duration connectTimeout();

        @WithDefault("30s")
        Duration requestTimeout();

        /** Retries of idempotent calls only; never the trigger run or the cancel. */
        @WithDefault("2")
        int readRetries();

        @WithDefault("500ms")
        Duration retryBackoff();
    }

    interface Logs {

        @WithDefault("5M")
        MemorySize maxSize();

        @WithDefault("1000")
        int cloudLoggingPageSize();
    }

    interface Endpoints {

        @WithDefault("https://cloudbuild.googleapis.com")
        String cloudBuild();

        @WithDefault("https://logging.googleapis.com")
        String logging();

        @WithDefault("https://storage.googleapis.com")
        String storage();

        @WithDefault("https://oauth2.googleapis.com/token")
        String oauthToken();

        @WithDefault("https://iamcredentials.googleapis.com")
        String iamCredentials();

        @WithDefault("http://metadata.google.internal")
        String metadata();
    }
}
