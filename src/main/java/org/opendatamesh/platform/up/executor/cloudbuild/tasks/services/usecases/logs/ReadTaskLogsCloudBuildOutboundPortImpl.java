package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.cloudbuild.CloudBuildClient;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Build;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;

import java.util.Optional;

class ReadTaskLogsCloudBuildOutboundPortImpl implements ReadTaskLogsCloudBuildOutboundPort {

    private final CloudBuildClient client;

    ReadTaskLogsCloudBuildOutboundPortImpl(CloudBuildClient client) {
        this.client = client;
    }

    @Override
    public Optional<Build> findBuild(CloudBuildAccess access, BuildName build) {
        return client.findBuild(access, build);
    }
}
