package org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks.usecases.logs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.util.Date;

@Schema(description = "Logs of a run, as one block")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskLogsRes {

    @Schema(description = "Build log, from the build's Cloud Storage bucket or from Cloud Logging")
    private String content;

    @Schema(description = "Time the logs were generated")
    private Date generatedAt;

    public TaskLogsRes() {
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Date generatedAt) {
        this.generatedAt = generatedAt;
    }
}
