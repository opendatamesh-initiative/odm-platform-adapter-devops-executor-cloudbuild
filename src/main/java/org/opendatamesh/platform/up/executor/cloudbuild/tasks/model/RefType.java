package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

/** Whether a Git ref is a branch or a tag; Cloud Build runs a trigger with one or the other. */
public enum RefType {
    BRANCH,
    TAG
}
