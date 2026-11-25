package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.auth.GoogleAccessTokenClient;
import org.opendatamesh.platform.up.executor.cloudbuild.config.CloudBuildProperties;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CredentialPolicy;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.GcpCredential;

class StartTaskAccessOutboundPortImpl implements StartTaskAccessOutboundPort {

    private final CloudBuildProperties properties;
    private final GoogleAccessTokenClient tokens;

    StartTaskAccessOutboundPortImpl(CloudBuildProperties properties, GoogleAccessTokenClient tokens) {
        this.properties = properties;
        this.tokens = tokens;
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
