package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.logs.BuildLogsClient;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Build;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LogDestination;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LogRead;

class ReadTaskLogsBuildLogsOutboundPortImpl implements ReadTaskLogsBuildLogsOutboundPort {

    private final BuildLogsClient client;
    private final long maxLogBytes;

    ReadTaskLogsBuildLogsOutboundPortImpl(BuildLogsClient client, long maxLogBytes) {
        this.client = client;
        this.maxLogBytes = maxLogBytes;
    }

    @Override
    public LogRead read(CloudBuildAccess access, Build build, LogDestination destination) {
        return switch (destination) {
            case STORAGE -> client.readFromStorage(access, build, maxLogBytes);
            case CLOUD_LOGGING -> client.readFromCloudLogging(access, build, maxLogBytes);
        };
    }

    @Override
    public long maxLogBytes() {
        return maxLogBytes;
    }
}
