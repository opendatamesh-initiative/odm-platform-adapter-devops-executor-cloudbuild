package org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.ExecutorApi;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.Fixtures;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.GoogleStub;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.GoogleStubResource;

import java.util.Map;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Start of a Cloud Build build (BDMD-5810), with the application running and Google replaced by {@link GoogleStub}. */
@QuarkusTest
@QuarkusTestResource(GoogleStubResource.class)
class StartTaskIT {

    private GoogleStub stub;

    @BeforeEach
    void resetStub() {
        stub = GoogleStubResource.stub();
        stub.reset();
    }

    /**
     * Feature: Start a build from a named trigger
     *
     * Scenario: The named trigger runs at the version's tag
     *   Given a trigger "dp-release" in the configured project and location
     *   When DevOps starts a task with pipelineIdentifier "dp-release" and ref TAG "v1.2.0"
     *   Then Cloud Build runs that trigger by its id with tagName "v1.2.0"
     *   And the providerRunId is the full build resource name
     */
    @Test
    void whenTriggerNamedThenRunsItAtTheTagAndReturnsBuildName() {
        ExecutorApi.start(Fixtures.TRIGGER_NAME, "v1.2.0", "TAG", Map.of("env", "prd"))
                .then().statusCode(200).body("providerRunId", equalTo(Fixtures.BUILD_NAME));

        JsonNode run = stub.lastRequest("POST", GoogleStub.RUN).json();
        assertEquals(Fixtures.PROJECT, run.path("projectId").asText());
        assertEquals(Fixtures.TRIGGER_ID, run.path("triggerId").asText());
        assertEquals("v1.2.0", run.at("/source/tagName").asText());
        assertTrue(run.at("/source/branchName").isMissingNode());
        assertEquals("prd", run.at("/source/substitutions/_ENV").asText());
    }

    /**
     * Scenario: A branch ref runs the trigger on that branch
     *   Given a trigger "dp-release"
     *   When DevOps starts a task with ref BRANCH "main"
     *   Then Cloud Build runs the trigger with branchName "main"
     */
    @Test
    void whenBranchRefThenRunsTriggerOnBranch() {
        ExecutorApi.start(Fixtures.TRIGGER_NAME, "main", "BRANCH", Map.of()).then().statusCode(200);

        JsonNode run = stub.lastRequest("POST", GoogleStub.RUN).json();
        assertEquals("main", run.at("/source/branchName").asText());
        assertTrue(run.at("/source/tagName").isMissingNode());
        assertTrue(run.at("/source/substitutions").isMissingNode());
    }

    /**
     * Scenario: The trigger name is resolved from the trigger list when Cloud Build does not read it by name
     *   Given Cloud Build answers 404 to reading the trigger by name and lists it among the triggers
     *   When DevOps starts a task with pipelineIdentifier "dp-release"
     *   Then the trigger found in the list is run
     */
    @Test
    void whenTriggerNotReadableByNameThenFoundInList() {
        stub.reply("GET", GoogleStub.TRIGGER_BY_NAME, 404, Fixtures.googleError(404, "NOT_FOUND", "trigger not found"));
        stub.reply("GET", GoogleStub.TRIGGERS + "?pageSize=100", 200, Fixtures.triggerPage("p2", Fixtures.otherTrigger()));
        stub.reply("GET", GoogleStub.TRIGGERS + "?pageSize=100&pageToken=p2", 200, Fixtures.triggerList(Fixtures.trigger()));

        ExecutorApi.start(Fixtures.TRIGGER_NAME, "v1.2.0", "TAG", Map.of())
                .then().statusCode(200).body("providerRunId", equalTo(Fixtures.BUILD_NAME));

        assertEquals(1, stub.requests("POST", GoogleStub.RUN).size());
    }

    /**
     * Scenario: An unknown trigger name starts nothing
     *   When DevOps starts a task with pipelineIdentifier "missing"
     *   Then the answer is 404 naming the trigger, the project and the location
     *   And no run is requested
     */
    @Test
    void whenTriggerUnknownThen404AndNoRun() {
        ExecutorApi.start("missing", "v1.2.0", "TAG", Map.of())
                .then().statusCode(404)
                .body(allOf(containsString("missing"), containsString(Fixtures.PROJECT), containsString(Fixtures.LOCATION)));

        assertTrue(stub.requests("POST", GoogleStub.RUN).isEmpty());
    }

    /**
     * Scenario: A task without pipelineIdentifier is refused
     *   When DevOps starts a task without pipelineIdentifier
     *   Then the answer is 400 explaining that it is the trigger's name
     */
    @Test
    void whenPipelineIdentifierMissingThen400() {
        ExecutorApi.start(null, "v1.2.0", "TAG", Map.of())
                .then().statusCode(400).body(containsString("name of the Cloud Build trigger"));

        assertTrue(stub.all().isEmpty());
    }

    /**
     * Feature: Authenticate to Google
     *
     * Scenario: Workload identity with impersonation calls Cloud Build with the impersonated token
     *   Given the executor runs in workload-identity mode impersonating a service account
     *   When DevOps starts a task
     *   Then the executor takes its token from the metadata server
     *   And exchanges it for the impersonated account's token
     *   And calls Cloud Build with the impersonated token
     */
    @Test
    void whenWorkloadIdentityThenCloudBuildCalledWithImpersonatedToken() {
        ExecutorApi.start(Fixtures.TRIGGER_NAME, "v1.2.0", "TAG", Map.of()).then().statusCode(200);

        assertEquals("Google", stub.lastRequest("GET", GoogleStub.METADATA_TOKEN).header("Metadata-Flavor"));
        GoogleStub.Recorded impersonation = stub.lastRequest("POST", GoogleStub.GENERATE_ACCESS_TOKEN);
        assertEquals("Bearer " + GoogleStub.EXECUTOR_TOKEN, impersonation.header("Authorization"));
        assertEquals("https://www.googleapis.com/auth/cloud-platform", impersonation.json().at("/scope/0").asText());
        assertEquals("Bearer " + GoogleStub.IMPERSONATED_TOKEN, stub.lastRequest("GET", GoogleStub.TRIGGER_BY_NAME).header("Authorization"));
        assertEquals("Bearer " + GoogleStub.IMPERSONATED_TOKEN, stub.lastRequest("POST", GoogleStub.RUN).header("Authorization"));
        assertTrue(stub.requests("POST", GoogleStub.OAUTH_TOKEN).isEmpty());
    }

    /**
     * Scenario: Google refuses the credential
     *   Given Cloud Build answers 403 PERMISSION_DENIED to the trigger read
     *   When DevOps starts a task
     *   Then the answer is 502 quoting Google's message
     */
    @Test
    void whenCloudBuildDeniesPermissionThen502WithGoogleMessage() {
        stub.reply("GET", GoogleStub.TRIGGER_BY_NAME, 403,
                Fixtures.googleError(403, "PERMISSION_DENIED", "The caller does not have permission cloudbuild.builds.get"));

        String message = ExecutorApi.start(Fixtures.TRIGGER_NAME, "v1.2.0", "TAG", Map.of())
                .then().statusCode(502).extract().asString();

        assertTrue(message.contains("Cloud Build refused the credentials"), message);
        assertTrue(message.contains("cloudbuild.builds.get"), message);
        assertFalse(message.contains(GoogleStub.IMPERSONATED_TOKEN), message);
    }
}
