package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.logs;

/**
 * Log of a build, by its provider run id.
 * {@code serviceAccountKey} is the value of the configured key header, or null; it is masked in {@link #toString()}.
 */
public record ReadTaskLogsCommand(String providerRunId, String serviceAccountKey) {

    @Override
    public String toString() {
        return "ReadTaskLogsCommand[providerRunId=" + providerRunId + ", serviceAccountKey=" + (serviceAccountKey == null ? "absent" : "***") + "]";
    }
}
