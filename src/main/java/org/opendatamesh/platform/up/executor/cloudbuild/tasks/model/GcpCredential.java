package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

/** What the executor authenticates to Google with, for one request. */
public sealed interface GcpCredential permits ServiceAccountKey, ExecutorIdentity {
}
