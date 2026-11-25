package org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Request of the status, logs and cancel calls")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskRunCommandRes {

    @Schema(description = "Executor parameters of the task")
    private ExecutorParametersRes executorParameters;

    @Schema(description = "Run id returned by start: projects/{project}/locations/{location}/builds/{id}")
    private String providerRunId;

    public TaskRunCommandRes() {
    }

    public ExecutorParametersRes getExecutorParameters() {
        return executorParameters;
    }

    public void setExecutorParameters(ExecutorParametersRes executorParameters) {
        this.executorParameters = executorParameters;
    }

    public String getProviderRunId() {
        return providerRunId;
    }

    public void setProviderRunId(String providerRunId) {
        this.providerRunId = providerRunId;
    }
}
