package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildTest {

    @ParameterizedTest
    @CsvSource(value = {
            "LEGACY,gs://b,STORAGE CLOUD_LOGGING", "LEGACY,,CLOUD_LOGGING", "LOGGING_UNSPECIFIED,gs://b,STORAGE CLOUD_LOGGING",
            "GCS_ONLY,gs://b,STORAGE", "GCS_ONLY,,", "CLOUD_LOGGING_ONLY,gs://b,CLOUD_LOGGING", "NONE,gs://b,"},
            emptyValue = "")
    void whenLoggingModeThenDestinationsInOrder(LoggingMode mode, String bucket, String expected) {
        Build build = new Build(new BuildName("p", "l", "b"), CloudBuildStatus.SUCCESS, "SUCCESS",
                bucket == null || bucket.isEmpty() ? Optional.empty() : Optional.of(bucket), mode);

        List<LogDestination> destinations = expected == null || expected.isEmpty() ? List.of()
                : Arrays.stream(expected.split(" ")).map(LogDestination::valueOf).toList();
        assertEquals(destinations, build.logDestinations());
    }
}
