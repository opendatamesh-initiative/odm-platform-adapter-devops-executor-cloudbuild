package org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks.usecases.status;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Status of a run")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskStatusRes {

    @Schema(description = "Run id")
    private String providerRunId;

    @Schema(description = "RUNNING, SUCCEEDED, FAILED or CANCELED")
    private String status;

    public TaskStatusRes() {
    }

    public String getProviderRunId() {
        return providerRunId;
    }

    public void setProviderRunId(String providerRunId) {
        this.providerRunId = providerRunId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
