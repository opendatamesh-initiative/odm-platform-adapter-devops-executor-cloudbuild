package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

import java.util.Arrays;

/** Where a build stores its log ({@code options.logging} of the build). */
public enum LoggingMode {
    LOGGING_UNSPECIFIED,
    LEGACY,
    GCS_ONLY,
    CLOUD_LOGGING_ONLY,
    NONE;

    private static final String DEPRECATED_CLOUD_LOGGING_ONLY = "STACKDRIVER_ONLY";

    public static LoggingMode fromApi(String value) {
        if (DEPRECATED_CLOUD_LOGGING_ONLY.equals(value)) {
            return CLOUD_LOGGING_ONLY;
        }
        return Arrays.stream(values())
                .filter(mode -> mode.name().equals(value))
                .findFirst()
                .orElse(LOGGING_UNSPECIFIED);
    }
}
