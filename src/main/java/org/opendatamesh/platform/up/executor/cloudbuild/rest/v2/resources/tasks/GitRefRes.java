package org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Git ref of the task")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitRefRes {

    @Schema(description = "Name of the branch or tag (required)")
    private String name;

    @Schema(description = "BRANCH or TAG (required): the trigger runs with branchName or tagName")
    private String type;

    public GitRefRes() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
