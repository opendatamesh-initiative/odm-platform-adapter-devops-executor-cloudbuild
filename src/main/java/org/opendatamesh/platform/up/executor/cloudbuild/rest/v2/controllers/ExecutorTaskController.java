package org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks.TaskResourceMapper;
import org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks.TaskRunCommandRes;
import org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks.usecases.logs.TaskLogsRes;
import org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks.usecases.start.TaskStartCommandRes;
import org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks.usecases.start.TaskStartResultRes;
import org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks.usecases.status.TaskStatusRes;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.ExecutorTaskUseCasesService;

/** DevOps executor API v2 for Google Cloud Build. */
@Path("/api/v2/up/executor/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Executor tasks")
public class ExecutorTaskController {

    private final ExecutorTaskUseCasesService service;
    private final TaskResourceMapper mapper;

    public ExecutorTaskController(ExecutorTaskUseCasesService service, TaskResourceMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @POST
    @Path("/start")
    @Operation(summary = "Start a Cloud Build build",
            description = "Runs the trigger named in executorParameters.pipelineIdentifier, in the configured project and "
                    + "location, at the ref's branch or tag. Pipeline parameters the trigger declares are sent as substitutions.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Build started; providerRunId is the build resource name"),
            @APIResponse(responseCode = "400", description = "Missing pipelineIdentifier, ref name or type, missing or invalid "
                    + "service-account key, invalid substitutions, or Cloud Build rejected the run"),
            @APIResponse(responseCode = "404", description = "No trigger with that name in the configured project and location"),
            @APIResponse(responseCode = "502", description = "Google refused the credentials or failed")
    })
    public TaskStartResultRes start(TaskStartCommandRes body, @Context HttpHeaders headers) {
        return mapper.toStartResult(service.start(mapper.toStartCommand(body, headers)));
    }

    @POST
    @Path("/status")
    @Operation(summary = "Read the status of a build")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Run status"),
            @APIResponse(responseCode = "400", description = "Malformed providerRunId, or a build outside the configured "
                    + "project and location, or a missing service-account key"),
            @APIResponse(responseCode = "404", description = "Unknown build"),
            @APIResponse(responseCode = "502", description = "Google refused the credentials or failed")
    })
    public TaskStatusRes status(TaskRunCommandRes body, @Context HttpHeaders headers) {
        return mapper.toStatusRes(service.status(mapper.toStatusCommand(body, headers)));
    }

    @POST
    @Path("/logs")
    @Operation(summary = "Read the log of a build as one block",
            description = "From the build's Cloud Storage bucket, else from Cloud Logging; a note says why when neither holds it.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Build log"),
            @APIResponse(responseCode = "400", description = "Malformed providerRunId, or a build outside the configured "
                    + "project and location, or a missing service-account key"),
            @APIResponse(responseCode = "404", description = "Unknown build"),
            @APIResponse(responseCode = "502", description = "Google refused the credentials or failed")
    })
    public TaskLogsRes logs(TaskRunCommandRes body, @Context HttpHeaders headers) {
        return mapper.toLogsRes(service.logs(mapper.toLogsCommand(body, headers)));
    }

    @POST
    @Path("/cancel")
    @Operation(summary = "Cancel a running build")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Cancel requested"),
            @APIResponse(responseCode = "400", description = "Malformed providerRunId, or a build outside the configured "
                    + "project and location, or a missing service-account key"),
            @APIResponse(responseCode = "404", description = "Unknown build"),
            @APIResponse(responseCode = "409", description = "The build has already ended"),
            @APIResponse(responseCode = "502", description = "Google refused the credentials or failed")
    })
    public Response cancel(TaskRunCommandRes body, @Context HttpHeaders headers) {
        service.cancel(mapper.toCancelCommand(body, headers));
        return Response.ok().build();
    }
}
