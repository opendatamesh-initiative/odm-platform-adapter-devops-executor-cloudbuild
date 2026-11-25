package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

/** The branch or tag a build runs at. */
public record GitRef(String name, RefType type) {
}
