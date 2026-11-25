package org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Executor parameters of the task")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutorParametersRes {

    @Schema(description = "Key of the repository in the DevOps platform, ignored (the trigger binds the repository)")
    private String repositoryKey;

    @Schema(description = "Repository of the data product, ignored (the trigger binds the repository)")
    private DataProductRepoRes dataProductRepo;

    @Schema(description = "Branch or tag the trigger runs at")
    private GitRefRes ref;

    @Schema(description = "Name of the Cloud Build trigger to run, in the project and location of the executor configuration (required)")
    private String pipelineIdentifier;

    public ExecutorParametersRes() {
    }

    public String getRepositoryKey() {
        return repositoryKey;
    }

    public void setRepositoryKey(String repositoryKey) {
        this.repositoryKey = repositoryKey;
    }

    public DataProductRepoRes getDataProductRepo() {
        return dataProductRepo;
    }

    public void setDataProductRepo(DataProductRepoRes dataProductRepo) {
        this.dataProductRepo = dataProductRepo;
    }

    public GitRefRes getRef() {
        return ref;
    }

    public void setRef(GitRefRes ref) {
        this.ref = ref;
    }

    public String getPipelineIdentifier() {
        return pipelineIdentifier;
    }

    public void setPipelineIdentifier(String pipelineIdentifier) {
        this.pipelineIdentifier = pipelineIdentifier;
    }
}
