package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

import java.util.Arrays;

/** Status of a Cloud Build build, and how DevOps sees it. */
public enum CloudBuildStatus {
    STATUS_UNKNOWN(RunStatus.RUNNING),
    PENDING(RunStatus.RUNNING),
    QUEUED(RunStatus.RUNNING),
    WORKING(RunStatus.RUNNING),
    SUCCESS(RunStatus.SUCCEEDED),
    FAILURE(RunStatus.FAILED),
    INTERNAL_ERROR(RunStatus.FAILED),
    TIMEOUT(RunStatus.FAILED),
    CANCELLED(RunStatus.CANCELED),
    EXPIRED(RunStatus.FAILED);

    private final RunStatus runStatus;

    CloudBuildStatus(RunStatus runStatus) {
        this.runStatus = runStatus;
    }

    /** A missing or unknown value is {@link #STATUS_UNKNOWN}, so DevOps keeps polling until its own cap. */
    public static CloudBuildStatus fromApi(String value) {
        return Arrays.stream(values())
                .filter(status -> status.name().equals(value))
                .findFirst()
                .orElse(STATUS_UNKNOWN);
    }

    public RunStatus runStatus() {
        return runStatus;
    }

    public boolean hasEnded() {
        return runStatus.isTerminal();
    }
}
