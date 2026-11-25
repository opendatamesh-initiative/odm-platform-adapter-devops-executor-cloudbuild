package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.logs;

import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.auth.GoogleAccessTokenClient;
import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.cloudbuild.CloudBuildClient;
import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.logs.BuildLogsClient;
import org.opendatamesh.platform.up.executor.cloudbuild.config.CloudBuildProperties;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCase;

@ApplicationScoped
public class ReadTaskLogsFactory {

    private final CloudBuildProperties properties;
    private final GoogleAccessTokenClient tokens;
    private final CloudBuildClient cloudBuild;
    private final BuildLogsClient logs;

    public ReadTaskLogsFactory(CloudBuildProperties properties, GoogleAccessTokenClient tokens, CloudBuildClient cloudBuild,
                               BuildLogsClient logs) {
        this.properties = properties;
        this.tokens = tokens;
        this.cloudBuild = cloudBuild;
        this.logs = logs;
    }

    public UseCase buildReadTaskLogs(ReadTaskLogsCommand command, ReadTaskLogsPresenter presenter) {
        return new ReadTaskLogs(command, presenter,
                new ReadTaskLogsAccessOutboundPortImpl(properties, tokens),
                new ReadTaskLogsCloudBuildOutboundPortImpl(cloudBuild),
                new ReadTaskLogsBuildLogsOutboundPortImpl(logs, properties.logs().maxSize().asLongValue()));
    }
}
