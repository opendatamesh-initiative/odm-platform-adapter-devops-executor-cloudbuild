package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Status mapping of BDMD-5810. */
class CloudBuildStatusTest {

    /**
     * Feature: Report the build status
     *
     * Scenario: Cloud Build statuses map to the DevOps vocabulary
     *   Given a build in status WORKING, SUCCESS, TIMEOUT or CANCELLED
     *   When DevOps asks for its status
     *   Then the status is RUNNING, SUCCEEDED, FAILED or CANCELED
     */
    @ParameterizedTest
    @CsvSource({
            "STATUS_UNKNOWN,RUNNING,false", "PENDING,RUNNING,false", "QUEUED,RUNNING,false", "WORKING,RUNNING,false",
            "SUCCESS,SUCCEEDED,true", "FAILURE,FAILED,true", "INTERNAL_ERROR,FAILED,true", "TIMEOUT,FAILED,true",
            "EXPIRED,FAILED,true", "CANCELLED,CANCELED,true", "SOMETHING_NEW,RUNNING,false"})
    void whenCloudBuildStatusThenMapsToRunStatus(String apiValue, RunStatus expected, boolean ended) {
        CloudBuildStatus status = CloudBuildStatus.fromApi(apiValue);

        assertEquals(expected, status.runStatus());
        assertEquals(ended, status.hasEnded());
    }
}
