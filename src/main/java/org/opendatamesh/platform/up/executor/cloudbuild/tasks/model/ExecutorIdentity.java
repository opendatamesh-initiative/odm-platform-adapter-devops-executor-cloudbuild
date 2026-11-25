package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

import java.util.Optional;

/** The executor's own GCP identity (metadata server), optionally impersonating a service account. */
public record ExecutorIdentity(Optional<String> impersonate) implements GcpCredential {
}
