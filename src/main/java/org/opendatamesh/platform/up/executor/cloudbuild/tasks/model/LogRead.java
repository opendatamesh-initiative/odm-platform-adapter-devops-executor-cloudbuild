package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

/** The outcome of reading a build log from one destination. */
public sealed interface LogRead permits LogContent, LogUnavailable {
}
