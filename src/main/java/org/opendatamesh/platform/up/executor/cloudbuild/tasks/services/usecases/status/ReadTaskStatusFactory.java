package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.status;

import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.auth.GoogleAccessTokenClient;
import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.cloudbuild.CloudBuildClient;
import org.opendatamesh.platform.up.executor.cloudbuild.config.CloudBuildProperties;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCase;

@ApplicationScoped
public class ReadTaskStatusFactory {

    private final CloudBuildProperties properties;
    private final GoogleAccessTokenClient tokens;
    private final CloudBuildClient cloudBuild;

    public ReadTaskStatusFactory(CloudBuildProperties properties, GoogleAccessTokenClient tokens, CloudBuildClient cloudBuild) {
        this.properties = properties;
        this.tokens = tokens;
        this.cloudBuild = cloudBuild;
    }

    public UseCase buildReadTaskStatus(ReadTaskStatusCommand command, ReadTaskStatusPresenter presenter) {
        return new ReadTaskStatus(command, presenter,
                new ReadTaskStatusAccessOutboundPortImpl(properties, tokens),
                new ReadTaskStatusCloudBuildOutboundPortImpl(cloudBuild));
    }
}
