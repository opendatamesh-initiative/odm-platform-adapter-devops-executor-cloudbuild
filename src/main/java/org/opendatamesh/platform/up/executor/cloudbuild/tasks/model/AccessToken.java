package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

/** A short-lived Google OAuth access token, used for the calls of one request and then dropped. */
public record AccessToken(String value) {

    @Override
    public String toString() {
        return "AccessToken[***]";
    }
}
