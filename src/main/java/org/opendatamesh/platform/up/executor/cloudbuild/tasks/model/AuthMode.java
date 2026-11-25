package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

/** How the executor authenticates to Google. Chosen once, in the executor configuration. */
public enum AuthMode {
    WORKLOAD_IDENTITY,
    SERVICE_ACCOUNT_KEY
}
