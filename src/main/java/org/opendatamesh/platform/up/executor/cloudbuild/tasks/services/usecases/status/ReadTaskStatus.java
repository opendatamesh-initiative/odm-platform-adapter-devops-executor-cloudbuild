package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Build;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildTarget;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCase;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCaseException;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCaseException.Reason;

/** Status of a build in the DevOps vocabulary. A run id outside the configured project and location is refused. */
class ReadTaskStatus implements UseCase {

    private final ReadTaskStatusCommand command;
    private final ReadTaskStatusPresenter presenter;
    private final ReadTaskStatusAccessOutboundPort accessPort;
    private final ReadTaskStatusCloudBuildOutboundPort cloudBuildPort;

    ReadTaskStatus(ReadTaskStatusCommand command, ReadTaskStatusPresenter presenter,
                   ReadTaskStatusAccessOutboundPort accessPort, ReadTaskStatusCloudBuildOutboundPort cloudBuildPort) {
        this.command = command;
        this.presenter = presenter;
        this.accessPort = accessPort;
        this.cloudBuildPort = cloudBuildPort;
    }

    @Override
    public void execute() {
        try {
            BuildName name = buildName();
            CloudBuildAccess access = open();
            Build build = findBuild(access, name);
            presenter.presentStatus(name, build.status().runStatus());
        } catch (Exception e) {
            presenter.presentException(e);
        }
    }

    private BuildName buildName() {
        BuildName name = BuildName.parse(command.providerRunId())
                .orElseThrow(() -> new UseCaseException(Reason.INVALID,
                        "providerRunId must be projects/{project}/locations/{location}/builds/{id}"));
        CloudBuildTarget target = accessPort.target();
        if (!target.owns(name)) {
            throw new UseCaseException(Reason.INVALID, "providerRunId " + name.value() + " is not in the configured project "
                    + target.project() + " and location " + target.location());
        }
        return name;
    }

    private CloudBuildAccess open() {
        return accessPort.open(accessPort.credentialPolicy().credentialFor(command.serviceAccountKey()));
    }

    private Build findBuild(CloudBuildAccess access, BuildName name) {
        return cloudBuildPort.findBuild(access, name)
                .orElseThrow(() -> new UseCaseException(Reason.NOT_FOUND, "No Cloud Build build " + name.buildId()
                        + " in project " + name.project() + ", location " + name.location()));
    }
}
