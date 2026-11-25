package org.opendatamesh.platform.up.executor.cloudbuild.clients.google;

/** One call to a Google API: which API, what the executor is trying to do (for error messages), and the URL. */
public record GoogleRequest(String api, String action, String url) {
}
