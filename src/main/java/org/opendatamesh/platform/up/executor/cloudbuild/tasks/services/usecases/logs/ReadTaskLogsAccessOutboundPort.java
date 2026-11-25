package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildTarget;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CredentialPolicy;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.GcpCredential;

interface ReadTaskLogsAccessOutboundPort {

    CloudBuildTarget target();

    CredentialPolicy credentialPolicy();

    CloudBuildAccess open(GcpCredential credential);
}
