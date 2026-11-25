package org.opendatamesh.platform.up.executor.cloudbuild.testsupport;

import io.quarkus.runtime.configuration.DurationConverter;
import io.quarkus.runtime.configuration.MemorySize;
import io.quarkus.runtime.configuration.MemorySizeConverter;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.opendatamesh.platform.up.executor.cloudbuild.config.CloudBuildProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** Builds the typed configuration from a property map, without starting the application. */
public final class TestConfigs {

    private TestConfigs() {
    }

    /** A valid configuration in workload-identity mode, every endpoint on {@code baseUrl}. */
    public static Map<String, String> valid(String baseUrl) {
        Map<String, String> properties = new HashMap<>();
        properties.put("executor.cloudbuild.project", Fixtures.PROJECT);
        properties.put("executor.cloudbuild.location", Fixtures.LOCATION);
        properties.put("executor.cloudbuild.auth.mode", "workload-identity");
        properties.put("executor.cloudbuild.http.read-retries", "0");
        properties.put("executor.cloudbuild.endpoints.cloud-build", baseUrl);
        properties.put("executor.cloudbuild.endpoints.logging", baseUrl);
        properties.put("executor.cloudbuild.endpoints.storage", baseUrl);
        properties.put("executor.cloudbuild.endpoints.oauth-token", baseUrl + GoogleStub.OAUTH_TOKEN);
        properties.put("executor.cloudbuild.endpoints.iam-credentials", baseUrl);
        properties.put("executor.cloudbuild.endpoints.metadata", baseUrl);
        return properties;
    }

    public static CloudBuildProperties cloudBuild(Map<String, String> properties) {
        return new SmallRyeConfigBuilder()
                .withConverter(Duration.class, 200, new DurationConverter())
                .withConverter(MemorySize.class, 200, new MemorySizeConverter())
                .withMapping(CloudBuildProperties.class)
                .withSources(new PropertiesConfigSource(properties, "test", 500))
                .build()
                .getConfigMapping(CloudBuildProperties.class);
    }
}
