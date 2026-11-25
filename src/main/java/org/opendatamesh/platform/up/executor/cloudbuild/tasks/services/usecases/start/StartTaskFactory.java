package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.start;

import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.auth.GoogleAccessTokenClient;
import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.cloudbuild.CloudBuildClient;
import org.opendatamesh.platform.up.executor.cloudbuild.config.CloudBuildProperties;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCase;

@ApplicationScoped
public class StartTaskFactory {

    private final CloudBuildProperties properties;
    private final GoogleAccessTokenClient tokens;
    private final CloudBuildClient cloudBuild;

    public StartTaskFactory(CloudBuildProperties properties, GoogleAccessTokenClient tokens, CloudBuildClient cloudBuild) {
        this.properties = properties;
        this.tokens = tokens;
        this.cloudBuild = cloudBuild;
    }

    public UseCase buildStartTask(StartTaskCommand command, StartTaskPresenter presenter) {
        return new StartTask(command, presenter,
                new StartTaskAccessOutboundPortImpl(properties, tokens),
                new StartTaskCloudBuildOutboundPortImpl(cloudBuild));
    }
}
