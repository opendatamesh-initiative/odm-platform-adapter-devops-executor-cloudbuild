package org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.ExecutorApi;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.Fixtures;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.GoogleStub;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.GoogleStubResource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Log of a Cloud Build build (BDMD-5810). */
@QuarkusTest
@QuarkusTestResource(GoogleStubResource.class)
class ReadTaskLogsIT {

    private GoogleStub stub;

    @BeforeEach
    void resetStub() {
        stub = GoogleStubResource.stub();
        stub.reset();
    }

    /**
     * Feature: Return the build log
     *
     * Scenario: The log is read from the build's Cloud Storage bucket
     *   Given a build with a logs bucket and LEGACY logging
     *   When DevOps asks for the logs
     *   Then the content is the log object of the build
     */
    @Test
    void whenBucketLoggingThenReadsLogObject() {
        stub.reply("GET", Fixtures.BUILD_PATH, 200, Fixtures.build("SUCCESS", Fixtures.LOGS_BUCKET, "LEGACY"));
        stub.replyText("GET", GoogleStub.LOG_OBJECT + "?alt=media", 200, "Step #0: dbt build\nDONE\n");

        ExecutorApi.run("logs", Fixtures.BUILD_NAME).then().statusCode(200)
                .body("content", equalTo("Step #0: dbt build\nDONE\n")).body("generatedAt", notNullValue());

        assertEquals("Bearer " + GoogleStub.IMPERSONATED_TOKEN,
                stub.lastRequest("GET", GoogleStub.LOG_OBJECT).header("Authorization"));
        assertTrue(stub.requests("POST", GoogleStub.ENTRIES_LIST).isEmpty());
    }

    /**
     * Scenario: The log is read from Cloud Logging when the build logs only there
     *   Given a build with CLOUD_LOGGING_ONLY logging
     *   When DevOps asks for the logs
     *   Then the content is the build's log entries in order
     */
    @Test
    void whenCloudLoggingOnlyThenReadsEntriesInOrder() {
        stub.reply("GET", Fixtures.BUILD_PATH, 200, Fixtures.build("FAILURE", null, "CLOUD_LOGGING_ONLY"));
        stub.reply("POST", GoogleStub.ENTRIES_LIST, 200, entries("Step #0: dbt build", "ERROR: model failed"));

        ExecutorApi.run("logs", Fixtures.BUILD_NAME).then().statusCode(200)
                .body("content", equalTo("Step #0: dbt build\nERROR: model failed\n"));

        var query = stub.lastRequest("POST", GoogleStub.ENTRIES_LIST).json();
        assertEquals("projects/" + Fixtures.PROJECT, query.at("/resourceNames/0").asText());
        assertTrue(query.path("filter").asText().contains("resource.labels.build_id=\"" + Fixtures.BUILD_ID + "\""));
        assertEquals("timestamp asc", query.path("orderBy").asText());
    }

    /**
     * Scenario: Cloud Logging is used when the bucket cannot be read
     *   Given a build with a logs bucket and LEGACY logging, and Cloud Storage refuses the read
     *   When DevOps asks for the logs
     *   Then the content comes from Cloud Logging
     */
    @Test
    void whenStorageRefusedThenFallsBackToCloudLogging() {
        stub.reply("GET", Fixtures.BUILD_PATH, 200, Fixtures.build("SUCCESS", Fixtures.LOGS_BUCKET, "LEGACY"));
        stub.reply("GET", GoogleStub.LOG_OBJECT, 403, Fixtures.googleError(403, "PERMISSION_DENIED", "no storage.objects.get"));
        stub.reply("POST", GoogleStub.ENTRIES_LIST, 200, entries("from cloud logging"));

        ExecutorApi.run("logs", Fixtures.BUILD_NAME).then().statusCode(200)
                .body("content", equalTo("from cloud logging\n"));
    }

    /**
     * Scenario: No source holds the log: the block says why for each source
     */
    @Test
    void whenNoSourceHoldsTheLogThenNoteWithReasons() {
        stub.reply("GET", Fixtures.BUILD_PATH, 200, Fixtures.build("SUCCESS", Fixtures.LOGS_BUCKET, "LEGACY"));
        stub.reply("GET", GoogleStub.LOG_OBJECT, 403, Fixtures.googleError(403, "PERMISSION_DENIED", "denied"));
        stub.reply("POST", GoogleStub.ENTRIES_LIST, 403, Fixtures.googleError(403, "PERMISSION_DENIED", "denied"));

        ExecutorApi.run("logs", Fixtures.BUILD_NAME).then().statusCode(200)
                .body("content", containsString("roles/storage.objectViewer"))
                .body("content", containsString("roles/logging.viewer"));
    }

    /**
     * Scenario: A large log is truncated
     *   Given a log larger than the configured maximum size
     *   When DevOps asks for the logs
     *   Then the content is cut at the maximum size and marked as truncated
     */
    @Test
    void whenLogLargerThanMaxThenTruncatedAndMarked() {
        stub.reply("GET", Fixtures.BUILD_PATH, 200, Fixtures.build("SUCCESS", Fixtures.LOGS_BUCKET, "GCS_ONLY"));
        stub.replyText("GET", GoogleStub.LOG_OBJECT, 200, "x".repeat(6 * 1024 * 1024));

        String content = ExecutorApi.run("logs", Fixtures.BUILD_NAME).then().statusCode(200).extract().path("content");

        assertTrue(content.startsWith("x".repeat(1000)));
        assertTrue(content.endsWith("… log truncated at 5242880 bytes …\n"), content.substring(content.length() - 60));
        assertEquals(5 * 1024 * 1024, content.indexOf('\n'));
    }

    private static String entries(String... lines) {
        ObjectNode page = Fixtures.JSON.createObjectNode();
        var entries = page.putArray("entries");
        for (String line : lines) {
            entries.addObject().put("textPayload", line);
        }
        return page.toString();
    }
}
