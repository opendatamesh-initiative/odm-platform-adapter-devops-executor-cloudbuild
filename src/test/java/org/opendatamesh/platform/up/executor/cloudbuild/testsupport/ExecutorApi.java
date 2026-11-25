package org.opendatamesh.platform.up.executor.cloudbuild.testsupport;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;

/** Calls of the executor API as DevOps makes them. */
public final class ExecutorApi {

    private ExecutorApi() {
    }

    public static Response start(String triggerName, String refName, String refType, Map<String, String> parameters) {
        ObjectNode body = Fixtures.JSON.createObjectNode();
        ObjectNode executorParameters = body.putObject("executorParameters");
        executorParameters.putObject("dataProductRepo").put("providerType", "GITLAB").put("externalIdentifier", "42");
        ObjectNode ref = executorParameters.putObject("ref").put("name", refName);
        if (refType != null) {
            ref.put("type", refType);
        }
        if (triggerName != null) {
            executorParameters.put("pipelineIdentifier", triggerName);
        }
        ObjectNode pipelineParameters = body.putObject("pipelineParameters");
        parameters.forEach(pipelineParameters::put);
        return post("start", body);
    }

    public static Response run(String verb, String providerRunId) {
        ObjectNode body = Fixtures.JSON.createObjectNode();
        body.putObject("executorParameters").put("pipelineIdentifier", Fixtures.TRIGGER_NAME)
                .putObject("ref").put("name", "v1.2.0").put("type", "TAG");
        body.put("providerRunId", providerRunId);
        return post(verb, body);
    }

    private static Response post(String verb, ObjectNode body) {
        return given().contentType(ContentType.JSON).body(body.toString())
                .when().post("/api/v2/up/executor/tasks/" + verb);
    }
}
