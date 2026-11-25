package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

/** What every Cloud Build call of one request needs: where, and with which token. */
public record CloudBuildAccess(CloudBuildTarget target, AccessToken token) {
}
