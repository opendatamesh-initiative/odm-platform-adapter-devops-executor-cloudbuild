package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

/** Log text read from one destination; {@code truncated} when it was cut at the configured maximum size. */
public record LogContent(String text, boolean truncated) implements LogRead {
}
