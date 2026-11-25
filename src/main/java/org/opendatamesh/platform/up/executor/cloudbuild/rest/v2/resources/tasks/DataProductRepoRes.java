package org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.resources.tasks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Repository of the data product; ignored by the Cloud Build executor")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataProductRepoRes {

    @Schema(description = "Provider type of the repository")
    private String providerType;

    @Schema(description = "Base URL of the repository provider")
    private String providerBaseUrl;

    @Schema(description = "Provider identifier of the repository")
    private String externalIdentifier;

    @Schema(description = "Repository name")
    private String name;

    public DataProductRepoRes() {
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getProviderBaseUrl() {
        return providerBaseUrl;
    }

    public void setProviderBaseUrl(String providerBaseUrl) {
        this.providerBaseUrl = providerBaseUrl;
    }

    public String getExternalIdentifier() {
        return externalIdentifier;
    }

    public void setExternalIdentifier(String externalIdentifier) {
        this.externalIdentifier = externalIdentifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
