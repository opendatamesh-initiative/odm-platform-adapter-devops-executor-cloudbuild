package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildTarget;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CredentialPolicy;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.GcpCredential;

interface ReadTaskStatusAccessOutboundPort {

    CloudBuildTarget target();

    CredentialPolicy credentialPolicy();

    CloudBuildAccess open(GcpCredential credential);
}
