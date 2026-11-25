package org.opendatamesh.platform.up.executor.cloudbuild.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.TestConfigs;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Start-up validation of BDMD-5810. */
class ExecutorCloudBuildConfigValidatorTest {

    @Test
    void whenValidThenPasses() {
        assertDoesNotThrow(() -> validate(TestConfigs.valid("http://stub:1")));
    }

    /**
     * Feature: Validate the configuration at start-up
     *
     * Scenario: An invalid key secret type stops the executor
     *   Given auth.key-secret-type "GCP_Key"
     *   Then start-up fails naming executor.cloudbuild.auth.key-secret-type
     */
    @ParameterizedTest
    @ValueSource(strings = {"GCP_Key", "gcp--key", "-key", "my-executor-secret-key"})
    void whenKeySecretTypeInvalidThenFailsNamingKey(String secretType) {
        Map<String, String> properties = TestConfigs.valid("http://stub:1");
        properties.put("executor.cloudbuild.auth.key-secret-type", secretType);

        assertFailsNaming("executor.cloudbuild.auth.key-secret-type", properties);
    }

    /**
     * Scenario: Impersonation in key mode stops the executor
     *   Given auth.mode service-account-key and an impersonated service account
     *   Then start-up fails naming executor.cloudbuild.auth.impersonate-service-account
     */
    @Test
    void whenImpersonationInKeyModeThenFailsNamingKey() {
        Map<String, String> properties = TestConfigs.valid("http://stub:1");
        properties.put("executor.cloudbuild.auth.mode", "service-account-key");
        properties.put("executor.cloudbuild.auth.impersonate-service-account", "sa@demo-project.iam.gserviceaccount.com");

        assertFailsNaming("executor.cloudbuild.auth.impersonate-service-account", properties);
    }

    @Test
    void whenProjectNotAGcpProjectIdThenFailsNamingKey() {
        Map<String, String> properties = TestConfigs.valid("http://stub:1");
        properties.put("executor.cloudbuild.project", "Demo Project");

        assertFailsNaming("executor.cloudbuild.project", properties);
    }

    @Test
    void whenEndpointNotAnHttpUrlThenFailsNamingKey() {
        Map<String, String> properties = TestConfigs.valid("http://stub:1");
        properties.put("executor.cloudbuild.endpoints.metadata", "metadata.google.internal");

        assertFailsNaming("executor.cloudbuild.endpoints.metadata", properties);
    }

    private static void assertFailsNaming(String key, Map<String, String> properties) {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> validate(properties));
        assertTrue(failure.getMessage().startsWith(key + ":"), failure.getMessage());
    }

    private static void validate(Map<String, String> properties) {
        new ExecutorCloudBuildConfigValidator(TestConfigs.cloudBuild(properties)).validate();
    }
}
