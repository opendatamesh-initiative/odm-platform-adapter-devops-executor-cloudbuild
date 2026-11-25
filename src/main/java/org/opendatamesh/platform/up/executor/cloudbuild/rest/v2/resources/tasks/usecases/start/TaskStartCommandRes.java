package org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks.usecases.start;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.util.Map;
import org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks.ExecutorParametersRes;

@Schema(description = "Start of a Cloud Build build")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskStartCommandRes {

    @Schema(description = "Executor parameters of the task")
    private ExecutorParametersRes executorParameters;

    @Schema(description = "Pipeline parameters: the ones the trigger declares as substitutions are sent (name env becomes _ENV)")
    private Map<String, String> pipelineParameters;

    public TaskStartCommandRes() {
    }

    public ExecutorParametersRes getExecutorParameters() {
        return executorParameters;
    }

    public void setExecutorParameters(ExecutorParametersRes executorParameters) {
        this.executorParameters = executorParameters;
    }

    public Map<String, String> getPipelineParameters() {
        return pipelineParameters;
    }

    public void setPipelineParameters(Map<String, String> pipelineParameters) {
        this.pipelineParameters = pipelineParameters;
    }
}
