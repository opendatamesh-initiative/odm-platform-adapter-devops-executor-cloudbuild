package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.cancel;

import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.auth.GoogleAccessTokenClient;
import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.cloudbuild.CloudBuildClient;
import org.opendatamesh.platform.up.executor.cloudbuild.config.CloudBuildProperties;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCase;

@ApplicationScoped
public class CancelTaskFactory {

    private final CloudBuildProperties properties;
    private final GoogleAccessTokenClient tokens;
    private final CloudBuildClient cloudBuild;

    public CancelTaskFactory(CloudBuildProperties properties, GoogleAccessTokenClient tokens, CloudBuildClient cloudBuild) {
        this.properties = properties;
        this.tokens = tokens;
        this.cloudBuild = cloudBuild;
    }

    public UseCase buildCancelTask(CancelTaskCommand command, CancelTaskPresenter presenter) {
        return new CancelTask(command, presenter,
                new CancelTaskAccessOutboundPortImpl(properties, tokens),
                new CancelTaskCloudBuildOutboundPortImpl(cloudBuild));
    }
}
