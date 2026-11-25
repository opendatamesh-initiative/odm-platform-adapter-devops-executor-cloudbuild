package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.auth.GoogleAccessTokenClient;
import org.opendatamesh.platform.up.executor.cloudbuild.config.CloudBuildProperties;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildTarget;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CredentialPolicy;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.GcpCredential;

class ReadTaskStatusAccessOutboundPortImpl implements ReadTaskStatusAccessOutboundPort {

    private final CloudBuildProperties properties;
    private final GoogleAccessTokenClient tokens;

    ReadTaskStatusAccessOutboundPortImpl(CloudBuildProperties properties, GoogleAccessTokenClient tokens) {
        this.properties = properties;
        this.tokens = tokens;
    }

    @Override
    public CloudBuildTarget target() {
        return properties.target();
    }

    @Override
    public CredentialPolicy credentialPolicy() {
        return properties.credentialPolicy();
    }

    @Override
    public CloudBuildAccess open(GcpCredential credential) {
        return new CloudBuildAccess(properties.target(), tokens.tokenFor(credential));
    }
}
