package org.opendatamesh.platform.up.executor.cloudbuild.clients.google.cloudbuild;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.GoogleApiTransport;
import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.GoogleRequest;
import org.opendatamesh.platform.up.executor.cloudbuild.config.CloudBuildProperties;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.BadGatewayException;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.BadRequestException;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Build;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildTarget;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.GitRef;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Trigger;

import java.util.Map;
import java.util.Optional;

/**
 * Cloud Build REST v1 on the regional paths {@code projects/{p}/locations/{l}}. A trigger is read by name first; when
 * Cloud Build does not resolve the name, the location's triggers are listed and matched on {@code name}. The run and the
 * cancel are never retried.
 */
@ApplicationScoped
public class CloudBuildClientImpl implements CloudBuildClient {

    private static final String API = "Cloud Build";
    private static final int TRIGGER_PAGE_SIZE = 100;

    private final GoogleApiTransport transport;
    private final CloudBuildProperties.Endpoints endpoints;
    private final CloudBuildJsonMapper mapper;

    public CloudBuildClientImpl(GoogleApiTransport transport, CloudBuildProperties properties) {
        this.transport = transport;
        this.endpoints = properties.endpoints();
        this.mapper = new CloudBuildJsonMapper(transport.objectMapper());
    }

    @Override
    public Optional<Trigger> findTrigger(CloudBuildAccess access, String name) {
        return readTriggerByName(access, name)
                .or(() -> listTriggersForName(access, name))
                .filter(trigger -> !trigger.id().isBlank());
    }

    @Override
    public BuildName runTrigger(CloudBuildAccess access, Trigger trigger, GitRef ref, Map<String, String> substitutions) {
        CloudBuildTarget target = access.target();
        GoogleRequest request = new GoogleRequest(API, "run trigger " + trigger.name(),
                locationUrl(target) + "/triggers/" + GoogleApiTransport.encodePathSegment(trigger.id()) + ":run");
        JsonNode operation = transport.writeJson(request, access.token(),
                mapper.runBody(target.project(), trigger, ref, substitutions));
        mapper.operationError(operation).ifPresent(message -> {
            throw new BadRequestException(API + " refused to run trigger " + trigger.name() + ": " + message);
        });
        return mapper.buildIdOf(operation)
                .map(target::buildName)
                .orElseThrow(() -> new BadGatewayException(
                        API + " accepted the run of trigger " + trigger.name() + " but returned no build id"));
    }

    @Override
    public Optional<Build> findBuild(CloudBuildAccess access, BuildName build) {
        GoogleRequest request = new GoogleRequest(API, "read build " + build.buildId(), buildUrl(build));
        return transport.readJson(request, access.token()).map(json -> mapper.toBuild(build, json));
    }

    @Override
    public void cancelBuild(CloudBuildAccess access, BuildName build) {
        GoogleRequest request = new GoogleRequest(API, "cancel build " + build.buildId(), buildUrl(build) + ":cancel");
        transport.writeJson(request, access.token(), mapper.cancelBody(build));
    }

    private Optional<Trigger> readTriggerByName(CloudBuildAccess access, String name) {
        GoogleRequest request = new GoogleRequest(API, "read trigger " + name,
                locationUrl(access.target()) + "/triggers/" + GoogleApiTransport.encodePathSegment(name));
        try {
            return transport.readJson(request, access.token()).map(mapper::toTrigger)
                    .filter(trigger -> name.equals(trigger.name()) || name.equals(trigger.id()));
        } catch (BadRequestException notAnId) {
            return Optional.empty();
        }
    }

    private Optional<Trigger> listTriggersForName(CloudBuildAccess access, String name) {
        String pageToken = "";
        do {
            JsonNode page = readTriggerPage(access, pageToken);
            for (JsonNode trigger : page.path("triggers")) {
                if (name.equals(trigger.path("name").asText())) {
                    return Optional.of(mapper.toTrigger(trigger));
                }
            }
            pageToken = page.path("nextPageToken").asText("");
        } while (!pageToken.isBlank());
        return Optional.empty();
    }

    private JsonNode readTriggerPage(CloudBuildAccess access, String pageToken) {
        String url = locationUrl(access.target()) + "/triggers?pageSize=" + TRIGGER_PAGE_SIZE
                + (pageToken.isBlank() ? "" : "&pageToken=" + GoogleApiTransport.encodePathSegment(pageToken));
        GoogleRequest request = new GoogleRequest(API, "list the triggers of " + access.target().location(), url);
        return transport.readJson(request, access.token()).orElseGet(() -> transport.objectMapper().createObjectNode());
    }

    private String locationUrl(CloudBuildTarget target) {
        return endpoints.cloudBuild() + "/v1/projects/" + GoogleApiTransport.encodePathSegment(target.project())
                + "/locations/" + GoogleApiTransport.encodePathSegment(target.location());
    }

    private String buildUrl(BuildName build) {
        return endpoints.cloudBuild() + "/v1/projects/" + GoogleApiTransport.encodePathSegment(build.project())
                + "/locations/" + GoogleApiTransport.encodePathSegment(build.location())
                + "/builds/" + GoogleApiTransport.encodePathSegment(build.buildId());
    }
}
