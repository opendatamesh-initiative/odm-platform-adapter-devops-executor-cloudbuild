package org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.controllers;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.ExecutorApi;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.Fixtures;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.GoogleStub;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.GoogleStubResource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Status of a Cloud Build build (BDMD-5810). */
@QuarkusTest
@QuarkusTestResource(GoogleStubResource.class)
class ReadTaskStatusIT {

    private GoogleStub stub;

    @BeforeEach
    void resetStub() {
        stub = GoogleStubResource.stub();
        stub.reset();
    }

    /**
     * Feature: Report the build status
     *
     * Scenario: Cloud Build statuses map to the DevOps vocabulary (end to end, one row per status)
     */
    @ParameterizedTest
    @CsvSource({"QUEUED,RUNNING", "WORKING,RUNNING", "SUCCESS,SUCCEEDED", "TIMEOUT,FAILED", "CANCELLED,CANCELED"})
    void whenBuildStatusThenReportedInDevOpsVocabulary(String cloudBuildStatus, String runStatus) {
        stub.reply("GET", Fixtures.BUILD_PATH, 200, Fixtures.build(cloudBuildStatus, Fixtures.LOGS_BUCKET, "LEGACY"));

        ExecutorApi.run("status", Fixtures.BUILD_NAME).then().statusCode(200)
                .body("providerRunId", equalTo(Fixtures.BUILD_NAME)).body("status", equalTo(runStatus));
    }

    /**
     * Scenario: An unknown build is not found
     *   When DevOps asks for the status of a build Cloud Build does not know
     *   Then the answer is 404
     */
    @Test
    void whenBuildUnknownThen404() {
        stub.reply("GET", Fixtures.BUILD_PATH, 404, Fixtures.googleError(404, "NOT_FOUND", "build not found"));

        ExecutorApi.run("status", Fixtures.BUILD_NAME).then().statusCode(404).body(containsString(Fixtures.BUILD_ID));
    }

    /**
     * Scenario: A run id from another project is refused
     *   When DevOps asks for the status of a build in another project
     *   Then the answer is 400 and Cloud Build is not called
     */
    @Test
    void whenRunIdInAnotherProjectThen400WithoutCallingCloudBuild() {
        ExecutorApi.run("status", "projects/other-project/locations/" + Fixtures.LOCATION + "/builds/" + Fixtures.BUILD_ID)
                .then().statusCode(400).body(containsString("not in the configured project"));

        assertTrue(stub.all().isEmpty());
    }

    /**
     * Scenario: A malformed run id is refused
     *   When the providerRunId is not a build resource name
     *   Then the answer is 400
     */
    @Test
    void whenRunIdMalformedThen400() {
        ExecutorApi.run("status", "12345").then().statusCode(400)
                .body(containsString("projects/{project}/locations/{location}/builds/{id}"));
    }
}
