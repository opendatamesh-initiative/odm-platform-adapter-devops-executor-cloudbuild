package org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.controllers;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.ExecutorApi;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.Fixtures;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.GoogleStub;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.GoogleStubResource;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cancel of a Cloud Build build (BDMD-5810). */
@QuarkusTest
@QuarkusTestResource(GoogleStubResource.class)
class CancelTaskIT {

    private GoogleStub stub;

    @BeforeEach
    void resetStub() {
        stub = GoogleStubResource.stub();
        stub.reset();
    }

    /**
     * Feature: Cancel a build
     *
     * Scenario: A running build is canceled
     *   Given a build in status WORKING
     *   When DevOps cancels it
     *   Then Cloud Build is asked to cancel the build and the answer is 200
     */
    @Test
    void whenBuildRunningThenCanceled() {
        ExecutorApi.run("cancel", Fixtures.BUILD_NAME).then().statusCode(200);

        var cancel = stub.lastRequest("POST", GoogleStub.CANCEL);
        assertEquals(Fixtures.BUILD_ID, cancel.json().path("id").asText());
        assertEquals("Bearer " + GoogleStub.IMPERSONATED_TOKEN, cancel.header("Authorization"));
    }

    /**
     * Scenario: An ended build is not canceled
     *   Given a build in status SUCCESS
     *   When DevOps cancels it
     *   Then the answer is 409 and Cloud Build is not asked to cancel
     */
    @Test
    void whenBuildEndedThen409AndNoCancel() {
        stub.reply("GET", Fixtures.BUILD_PATH, 200, Fixtures.build("SUCCESS", Fixtures.LOGS_BUCKET, "LEGACY"));

        ExecutorApi.run("cancel", Fixtures.BUILD_NAME).then().statusCode(409).body(containsString("already ended (SUCCESS)"));

        assertTrue(stub.requests("POST", GoogleStub.CANCEL).isEmpty());
    }

    /**
     * Scenario: An unknown build cannot be canceled
     *   When DevOps cancels a build Cloud Build does not know
     *   Then the answer is 404
     */
    @Test
    void whenBuildUnknownThen404() {
        stub.reply("GET", Fixtures.BUILD_PATH, 404, Fixtures.googleError(404, "NOT_FOUND", "build not found"));

        ExecutorApi.run("cancel", Fixtures.BUILD_NAME).then().statusCode(404);

        assertTrue(stub.requests("POST", GoogleStub.CANCEL).isEmpty());
    }
}
