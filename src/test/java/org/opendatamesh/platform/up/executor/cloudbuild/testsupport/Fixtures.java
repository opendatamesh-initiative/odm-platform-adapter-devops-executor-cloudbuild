package org.opendatamesh.platform.up.executor.cloudbuild.testsupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** JSON payloads as the Google APIs return them. */
public final class Fixtures {

    public static final ObjectMapper JSON = new ObjectMapper();
    public static final String PROJECT = "demo-project";
    public static final String LOCATION = "europe-west8";
    public static final String TRIGGER_NAME = "dp-release";
    public static final String TRIGGER_ID = "4f8e2c1a-5b6d-4e7f-8a9b-0c1d2e3f4a5b";
    public static final String BUILD_ID = "b1d2e3f4-0001";
    public static final String BUILD_NAME = "projects/" + PROJECT + "/locations/" + LOCATION + "/builds/" + BUILD_ID;
    public static final String LOCATION_PATH = "/v1/projects/" + PROJECT + "/locations/" + LOCATION;
    public static final String BUILD_PATH = LOCATION_PATH + "/builds/" + BUILD_ID;
    public static final String LOGS_BUCKET = "gs://demo-project-build-logs";

    private Fixtures() {
    }

    /** Trigger dp-release, declaring _ENV and _DATASET. */
    public static String trigger() {
        ObjectNode trigger = JSON.createObjectNode().put("id", TRIGGER_ID).put("name", TRIGGER_NAME)
                .put("resourceName", "projects/" + PROJECT + "/locations/" + LOCATION + "/triggers/" + TRIGGER_ID);
        trigger.putObject("substitutions").put("_ENV", "lab").put("_DATASET", "raw");
        return trigger.toString();
    }

    public static String triggerList(String... triggers) {
        return triggerPage(null, triggers);
    }

    public static String triggerPage(String nextPageToken, String... triggers) {
        ObjectNode list = JSON.createObjectNode();
        if (nextPageToken != null) {
            list.put("nextPageToken", nextPageToken);
        }
        var items = list.putArray("triggers");
        for (String trigger : triggers) {
            try {
                items.add(JSON.readTree(trigger));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return list.toString();
    }

    public static String otherTrigger() {
        return JSON.createObjectNode().put("id", "other-id").put("name", "other").toString();
    }

    /** The long-running operation returned by triggers:run. */
    public static String runOperation() {
        ObjectNode operation = JSON.createObjectNode().put("name", "projects/" + PROJECT + "/locations/" + LOCATION
                + "/operations/op-1");
        ObjectNode metadata = operation.putObject("metadata");
        metadata.put("@type", "type.googleapis.com/google.devtools.cloudbuild.v1.BuildOperationMetadata");
        metadata.putObject("build").put("id", BUILD_ID).put("status", "QUEUED");
        return operation.toString();
    }

    public static String build(String status, String logsBucket, String logging) {
        ObjectNode build = JSON.createObjectNode().put("id", BUILD_ID).put("projectId", PROJECT).put("status", status);
        if (logsBucket != null) {
            build.put("logsBucket", logsBucket);
        }
        if (logging != null) {
            build.putObject("options").put("logging", logging);
        }
        return build.toString();
    }

    public static String googleError(int code, String status, String message) {
        ObjectNode body = JSON.createObjectNode();
        body.putObject("error").put("code", code).put("status", status).put("message", message);
        return body.toString();
    }
}
