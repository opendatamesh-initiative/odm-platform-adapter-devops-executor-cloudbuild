package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

import java.util.Map;

/** A Cloud Build trigger: its service-generated id, its name, and the substitutions it declares with their defaults. */
public record Trigger(String id, String name, Map<String, String> substitutions) {

    public Trigger {
        substitutions = substitutions == null ? Map.of() : Map.copyOf(substitutions);
    }
}
