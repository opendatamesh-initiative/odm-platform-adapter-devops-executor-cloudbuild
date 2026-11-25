package org.opendatamesh.platform.up.executor.cloudbuild.clients.google.cloudbuild;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Build;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildStatus;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.GitRef;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LoggingMode;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.RefType;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Trigger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Maps Cloud Build JSON to domain types and builds request bodies. It filters nothing and decides nothing. */
final class CloudBuildJsonMapper {

    private final ObjectMapper objectMapper;

    CloudBuildJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    Trigger toTrigger(JsonNode json) {
        Map<String, String> substitutions = new LinkedHashMap<>();
        json.path("substitutions").fields().forEachRemaining(entry -> substitutions.put(entry.getKey(), entry.getValue().asText()));
        return new Trigger(json.path("id").asText(""), json.path("name").asText(""), substitutions);
    }

    Build toBuild(BuildName name, JsonNode json) {
        String rawStatus = json.path("status").asText("STATUS_UNKNOWN");
        String bucket = json.path("logsBucket").asText("");
        return new Build(name, CloudBuildStatus.fromApi(rawStatus), rawStatus,
                bucket.isBlank() ? Optional.empty() : Optional.of(bucket),
                LoggingMode.fromApi(json.path("options").path("logging").asText(null)));
    }

    ObjectNode runBody(String project, Trigger trigger, GitRef ref, Map<String, String> substitutions) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("projectId", project);
        body.put("triggerId", trigger.id());
        ObjectNode source = body.putObject("source");
        source.put("projectId", project);
        source.put(ref.type() == RefType.TAG ? "tagName" : "branchName", ref.name());
        if (!substitutions.isEmpty()) {
            ObjectNode values = source.putObject("substitutions");
            substitutions.forEach(values::put);
        }
        return body;
    }

    ObjectNode cancelBody(BuildName build) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("projectId", build.project());
        body.put("id", build.buildId());
        return body;
    }

    /** The build id of a run operation: {@code metadata.build.id}, else the last segment of {@code metadata.build.name}. */
    Optional<String> buildIdOf(JsonNode operation) {
        JsonNode build = operation.path("metadata").path("build");
        String id = build.path("id").asText("");
        if (!id.isBlank()) {
            return Optional.of(id);
        }
        String name = build.path("name").asText("");
        return name.isBlank() ? Optional.empty() : Optional.of(name.substring(name.lastIndexOf('/') + 1));
    }

    Optional<String> operationError(JsonNode operation) {
        JsonNode error = operation.path("error");
        return error.isMissingNode() || error.isNull() ? Optional.empty() : Optional.of(error.path("message").asText(error.toString()));
    }
}
