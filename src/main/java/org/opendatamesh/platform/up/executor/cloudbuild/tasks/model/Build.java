package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

import java.util.List;
import java.util.Optional;

/** A Cloud Build build: its status and where its log is stored. */
public record Build(BuildName name, CloudBuildStatus status, String rawStatus, Optional<String> logsBucket,
                    LoggingMode loggingMode) {

    /** Where to look for the log, in order: the bucket first when the build writes one, then Cloud Logging. */
    public List<LogDestination> logDestinations() {
        return switch (loggingMode) {
            case NONE -> List.of();
            case GCS_ONLY -> logsBucket.isPresent() ? List.of(LogDestination.STORAGE) : List.of();
            case CLOUD_LOGGING_ONLY -> List.of(LogDestination.CLOUD_LOGGING);
            case LEGACY, LOGGING_UNSPECIFIED -> logsBucket.isPresent()
                    ? List.of(LogDestination.STORAGE, LogDestination.CLOUD_LOGGING)
                    : List.of(LogDestination.CLOUD_LOGGING);
        };
    }
}
