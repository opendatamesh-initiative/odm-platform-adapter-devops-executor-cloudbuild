package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildTarget;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.GitRef;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.RefType;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Trigger;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCase;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCaseException;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCaseException.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/**
 * Start of a build: run the trigger named in the task, in the configured project and location, at the task's branch
 * or tag, with the pipeline parameters the trigger declares as substitutions.
 */
class StartTask implements UseCase {

    private static final Logger logger = LoggerFactory.getLogger(StartTask.class);

    private final StartTaskCommand command;
    private final StartTaskPresenter presenter;
    private final StartTaskAccessOutboundPort accessPort;
    private final StartTaskCloudBuildOutboundPort cloudBuildPort;

    StartTask(StartTaskCommand command, StartTaskPresenter presenter,
              StartTaskAccessOutboundPort accessPort, StartTaskCloudBuildOutboundPort cloudBuildPort) {
        this.command = command;
        this.presenter = presenter;
        this.accessPort = accessPort;
        this.cloudBuildPort = cloudBuildPort;
    }

    @Override
    public void execute() {
        try {
            String triggerName = triggerName();
            GitRef ref = ref();
            CloudBuildAccess access = open();
            Trigger trigger = findTrigger(access, triggerName);
            SubstitutionSelection.Selection selection = selectSubstitutions(trigger);
            BuildName build = cloudBuildPort.runTrigger(access, trigger, ref, selection.substitutions());
            logger.info("Started Cloud Build build {} from trigger {} at {} {}", build.value(), trigger.name(), ref.type(), ref.name());
            presenter.presentStarted(build);
        } catch (Exception e) {
            presenter.presentException(e);
        }
    }

    private String triggerName() {
        if (isBlank(command.pipelineIdentifier())) {
            throw new UseCaseException(Reason.INVALID,
                    "executorParameters.pipelineIdentifier is required: it is the name of the Cloud Build trigger to run");
        }
        return command.pipelineIdentifier().trim();
    }

    private GitRef ref() {
        if (isBlank(command.refName())) {
            throw new UseCaseException(Reason.INVALID, "executorParameters.ref.name is required");
        }
        return new GitRef(command.refName().trim(), refType());
    }

    private RefType refType() {
        String type = command.refType() == null ? "" : command.refType().trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(RefType.values())
                .filter(value -> value.name().equals(type))
                .findFirst()
                .orElseThrow(() -> new UseCaseException(Reason.INVALID, "executorParameters.ref.type must be BRANCH or TAG"));
    }

    private CloudBuildAccess open() {
        return accessPort.open(accessPort.credentialPolicy().credentialFor(command.serviceAccountKey()));
    }

    private Trigger findTrigger(CloudBuildAccess access, String name) {
        CloudBuildTarget target = access.target();
        return cloudBuildPort.findTrigger(access, name)
                .orElseThrow(() -> new UseCaseException(Reason.NOT_FOUND, "No Cloud Build trigger named " + name
                        + " in project " + target.project() + ", location " + target.location()));
    }

    private SubstitutionSelection.Selection selectSubstitutions(Trigger trigger) {
        Map<String, String> parameters = command.pipelineParameters() == null ? Map.of() : command.pipelineParameters();
        SubstitutionSelection.Selection selection = new SubstitutionSelection().select(trigger, parameters);
        if (!selection.dropped().isEmpty()) {
            logger.info("Dropped pipeline parameters not declared as substitutions of trigger {}: {}",
                    trigger.name(), selection.dropped());
        }
        return selection;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
