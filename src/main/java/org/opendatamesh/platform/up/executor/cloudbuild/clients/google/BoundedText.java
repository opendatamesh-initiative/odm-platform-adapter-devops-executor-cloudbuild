package org.opendatamesh.platform.up.executor.cloudbuild.clients.google;

/** Text read up to a size bound; {@code truncated} when the source held more. */
public record BoundedText(String text, boolean truncated) {
}
