package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

/** A destination that holds no readable log for the build, and why. */
public record LogUnavailable(String reason) implements LogRead {
}
