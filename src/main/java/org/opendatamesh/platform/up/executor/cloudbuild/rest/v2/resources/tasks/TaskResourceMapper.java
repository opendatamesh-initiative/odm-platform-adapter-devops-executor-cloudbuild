package org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import org.opendatamesh.platform.up.executor.cloudbuild.config.CloudBuildProperties;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.BadRequestException;
import org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks.usecases.logs.TaskLogsRes;
import org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks.usecases.start.TaskStartCommandRes;
import org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks.usecases.start.TaskStartResultRes;
import org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks.usecases.status.TaskStatusRes;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.ExecutorTaskUseCasesService.TaskLogs;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.ExecutorTaskUseCasesService.TaskStatus;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.cancel.CancelTaskCommand;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.logs.ReadTaskLogsCommand;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.start.StartTaskCommand;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.status.ReadTaskStatusCommand;

import java.util.Date;
import java.util.Map;

/**
 * Maps the DevOps executor API v2 resources to the use case commands and back. The service-account key is read from
 * the configured header ({@code x-odm-<auth.key-secret-type>}) and never logged.
 */
@ApplicationScoped
public class TaskResourceMapper {

    private static final String EXECUTOR_PARAMETERS_REQUIRED = "executorParameters is required";
    private static final String PROVIDER_RUN_ID_REQUIRED = "providerRunId is required";

    private final String keyHeader;

    public TaskResourceMapper(CloudBuildProperties properties) {
        this.keyHeader = properties.keyHeader();
    }

    public StartTaskCommand toStartCommand(TaskStartCommandRes body, HttpHeaders headers) {
        if (body == null || body.getExecutorParameters() == null) {
            throw new BadRequestException(EXECUTOR_PARAMETERS_REQUIRED);
        }
        ExecutorParametersRes parameters = body.getExecutorParameters();
        GitRefRes ref = parameters.getRef();
        Map<String, String> pipelineParameters = body.getPipelineParameters() == null ? Map.of() : body.getPipelineParameters();
        return new StartTaskCommand(parameters.getPipelineIdentifier(), ref == null ? null : ref.getName(),
                ref == null ? null : ref.getType(), pipelineParameters, serviceAccountKey(headers));
    }

    public ReadTaskStatusCommand toStatusCommand(TaskRunCommandRes body, HttpHeaders headers) {
        return new ReadTaskStatusCommand(requireRunId(body), serviceAccountKey(headers));
    }

    public ReadTaskLogsCommand toLogsCommand(TaskRunCommandRes body, HttpHeaders headers) {
        return new ReadTaskLogsCommand(requireRunId(body), serviceAccountKey(headers));
    }

    public CancelTaskCommand toCancelCommand(TaskRunCommandRes body, HttpHeaders headers) {
        return new CancelTaskCommand(requireRunId(body), serviceAccountKey(headers));
    }

    public TaskStartResultRes toStartResult(BuildName build) {
        TaskStartResultRes res = new TaskStartResultRes();
        res.setProviderRunId(build.value());
        return res;
    }

    public TaskStatusRes toStatusRes(TaskStatus status) {
        TaskStatusRes res = new TaskStatusRes();
        res.setProviderRunId(status.build().value());
        res.setStatus(status.status().name());
        return res;
    }

    public TaskLogsRes toLogsRes(TaskLogs logs) {
        TaskLogsRes res = new TaskLogsRes();
        res.setContent(logs.content());
        res.setGeneratedAt(Date.from(logs.generatedAt()));
        return res;
    }

    private String serviceAccountKey(HttpHeaders headers) {
        return headers == null ? null : headers.getHeaderString(keyHeader);
    }

    private static String requireRunId(TaskRunCommandRes body) {
        if (body == null || body.getExecutorParameters() == null) {
            throw new BadRequestException(EXECUTOR_PARAMETERS_REQUIRED);
        }
        if (body.getProviderRunId() == null || body.getProviderRunId().isBlank()) {
            throw new BadRequestException(PROVIDER_RUN_ID_REQUIRED);
        }
        return body.getProviderRunId();
    }
}
