package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.cancel;

import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.cloudbuild.CloudBuildClient;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Build;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;

import java.util.Optional;

class CancelTaskCloudBuildOutboundPortImpl implements CancelTaskCloudBuildOutboundPort {

    private final CloudBuildClient client;

    CancelTaskCloudBuildOutboundPortImpl(CloudBuildClient client) {
        this.client = client;
    }

    @Override
    public Optional<Build> findBuild(CloudBuildAccess access, BuildName build) {
        return client.findBuild(access, build);
    }

    @Override
    public void cancelBuild(CloudBuildAccess access, BuildName build) {
        client.cancelBuild(access, build);
    }
}
