package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

/** Status of a task run as the DevOps executor API reports it. */
public enum RunStatus {
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELED;

    public boolean isTerminal() {
        return this != RUNNING;
    }
}
