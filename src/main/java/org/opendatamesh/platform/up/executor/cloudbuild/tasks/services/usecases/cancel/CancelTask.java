package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.cancel;

import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Build;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildTarget;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCase;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCaseException;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCaseException.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Cancel of a running build. Unknown build: not found; ended build: conflict, and Cloud Build is not called. */
class CancelTask implements UseCase {

    private static final Logger logger = LoggerFactory.getLogger(CancelTask.class);

    private final CancelTaskCommand command;
    private final CancelTaskPresenter presenter;
    private final CancelTaskAccessOutboundPort accessPort;
    private final CancelTaskCloudBuildOutboundPort cloudBuildPort;

    CancelTask(CancelTaskCommand command, CancelTaskPresenter presenter,
               CancelTaskAccessOutboundPort accessPort, CancelTaskCloudBuildOutboundPort cloudBuildPort) {
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
            rejectEnded(build);
            cloudBuildPort.cancelBuild(access, name);
            logger.info("Cancel of Cloud Build build {} requested", name.value());
            presenter.presentCanceled(name);
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

    private static void rejectEnded(Build build) {
        if (build.status().hasEnded()) {
            throw new UseCaseException(Reason.CONFLICT,
                    "Build " + build.name().buildId() + " has already ended (" + build.rawStatus() + ")");
        }
    }
}
