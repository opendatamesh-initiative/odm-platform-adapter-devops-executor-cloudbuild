package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

/** The GCP project and location the executor runs triggers in. It comes from the executor configuration. */
public record CloudBuildTarget(String project, String location) {

    public boolean owns(BuildName build) {
        return project.equals(build.project()) && location.equals(build.location());
    }

    public BuildName buildName(String buildId) {
        return new BuildName(project, location, buildId);
    }
}
