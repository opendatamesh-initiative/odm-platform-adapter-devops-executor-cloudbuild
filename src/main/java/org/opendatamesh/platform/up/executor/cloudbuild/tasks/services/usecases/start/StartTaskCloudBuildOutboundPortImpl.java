package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.cloudbuild.CloudBuildClient;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.GitRef;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Trigger;

import java.util.Map;
import java.util.Optional;

class StartTaskCloudBuildOutboundPortImpl implements StartTaskCloudBuildOutboundPort {

    private final CloudBuildClient client;

    StartTaskCloudBuildOutboundPortImpl(CloudBuildClient client) {
        this.client = client;
    }

    @Override
    public Optional<Trigger> findTrigger(CloudBuildAccess access, String name) {
        return client.findTrigger(access, name);
    }

    @Override
    public BuildName runTrigger(CloudBuildAccess access, Trigger trigger, GitRef ref, Map<String, String> substitutions) {
        return client.runTrigger(access, trigger, ref, substitutions);
    }
}
