package org.opendatamesh.platform.up.executor.cloudbuild.testsupport;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Starts one {@link GoogleStub} for the whole test run and points every Google endpoint of the executor at it. The
 * project, location and auth mode (workload identity, impersonating {@link GoogleStub#IMPERSONATED_SA}) come from the
 * test profile of application.yaml.
 */
public class GoogleStubResource implements QuarkusTestResourceLifecycleManager {

    private static volatile GoogleStub stub;

    public static GoogleStub stub() {
        GoogleStub current = stub;
        if (current == null) {
            throw new IllegalStateException("GoogleStubResource is not running");
        }
        return current;
    }

    @Override
    public Map<String, String> start() {
        try {
            stub = new GoogleStub();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot start the Google stub", e);
        }
        String url = stub.baseUrl();
        return Map.of(
                "executor.cloudbuild.endpoints.cloud-build", url,
                "executor.cloudbuild.endpoints.logging", url,
                "executor.cloudbuild.endpoints.storage", url,
                "executor.cloudbuild.endpoints.oauth-token", url + GoogleStub.OAUTH_TOKEN,
                "executor.cloudbuild.endpoints.iam-credentials", url,
                "executor.cloudbuild.endpoints.metadata", url);
    }

    @Override
    public void stop() {
        GoogleStub current = stub;
        stub = null;
        if (current != null) {
            current.close();
        }
    }
}
