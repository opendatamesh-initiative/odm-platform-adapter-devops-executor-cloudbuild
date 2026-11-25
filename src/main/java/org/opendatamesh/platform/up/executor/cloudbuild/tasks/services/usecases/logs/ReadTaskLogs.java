package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Build;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildTarget;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LogContent;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LogDestination;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LogRead;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LogUnavailable;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LoggingMode;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCase;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCaseException;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCaseException.Reason;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Log of a build as one block. The destinations of the build are read in order (its Cloud Storage bucket, then Cloud
 * Logging) and the first one that holds the log wins; when none does, the block says why for each.
 */
class ReadTaskLogs implements UseCase {

    private final ReadTaskLogsCommand command;
    private final ReadTaskLogsPresenter presenter;
    private final ReadTaskLogsAccessOutboundPort accessPort;
    private final ReadTaskLogsCloudBuildOutboundPort cloudBuildPort;
    private final ReadTaskLogsBuildLogsOutboundPort logsPort;

    ReadTaskLogs(ReadTaskLogsCommand command, ReadTaskLogsPresenter presenter, ReadTaskLogsAccessOutboundPort accessPort,
                 ReadTaskLogsCloudBuildOutboundPort cloudBuildPort, ReadTaskLogsBuildLogsOutboundPort logsPort) {
        this.command = command;
        this.presenter = presenter;
        this.accessPort = accessPort;
        this.cloudBuildPort = cloudBuildPort;
        this.logsPort = logsPort;
    }

    @Override
    public void execute() {
        try {
            BuildName name = buildName();
            CloudBuildAccess access = open();
            Build build = findBuild(access, name);
            String content = logsOf(access, build);
            presenter.presentLogs(content, Instant.now());
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

    private String logsOf(CloudBuildAccess access, Build build) {
        List<LogDestination> destinations = build.logDestinations();
        if (destinations.isEmpty()) {
            return noLogsStored(build);
        }
        List<String> reasons = new ArrayList<>();
        for (LogDestination destination : destinations) {
            LogRead read = logsPort.read(access, build, destination);
            if (read instanceof LogContent content) {
                return rendered(content);
            }
            reasons.add(((LogUnavailable) read).reason());
        }
        return "No log available for build " + build.name().buildId() + " (" + build.rawStatus() + "): "
                + String.join("; ", reasons) + "\n";
    }

    private String rendered(LogContent content) {
        if (!content.truncated()) {
            return content.text();
        }
        return content.text() + "\n… log truncated at " + logsPort.maxLogBytes() + " bytes …\n";
    }

    private static String noLogsStored(Build build) {
        if (build.loggingMode() == LoggingMode.NONE) {
            return "Build " + build.name().buildId() + " stores no logs (options.logging = NONE)\n";
        }
        return "Build " + build.name().buildId() + " writes its log only to Cloud Storage but has no logs bucket\n";
    }
}
