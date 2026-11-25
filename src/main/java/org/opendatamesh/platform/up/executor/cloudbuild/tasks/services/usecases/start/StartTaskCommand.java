package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.start;

import java.util.Map;

/**
 * Start of a build: the trigger's name, the ref and the pipeline parameters of the task. {@code serviceAccountKey} is
 * the value of the configured key header, or null; it is masked in {@link #toString()}.
 */
public record StartTaskCommand(String pipelineIdentifier, String refName, String refType,
                               Map<String, String> pipelineParameters, String serviceAccountKey) {

    @Override
    public String toString() {
        return "StartTaskCommand[pipelineIdentifier=" + pipelineIdentifier + ", refName=" + refName + ", refType=" + refType
                + ", pipelineParameters=" + (pipelineParameters == null ? "{}" : pipelineParameters.keySet())
                + ", serviceAccountKey=" + (serviceAccountKey == null ? "absent" : "***") + "]";
    }
}
