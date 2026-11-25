package org.opendatamesh.platform.up.executor.cloudbuild.clients.google.auth;

import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.AccessToken;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.GcpCredential;

/** Turns a credential into a short-lived Google access token for the {@code cloud-platform} scope. */
public interface GoogleAccessTokenClient {

    AccessToken tokenFor(GcpCredential credential);
}
