package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services;

import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.RunStatus;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.cancel.CancelTaskCommand;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.cancel.CancelTaskFactory;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.logs.ReadTaskLogsCommand;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.logs.ReadTaskLogsFactory;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.logs.ReadTaskLogsPresenter;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.start.StartTaskCommand;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.start.StartTaskFactory;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.start.StartTaskPresenter;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.status.ReadTaskStatusCommand;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.status.ReadTaskStatusFactory;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.status.ReadTaskStatusPresenter;

import java.time.Instant;

/** Runs the executor task use cases on domain types; version-independent. */
@ApplicationScoped
public class ExecutorTaskUseCasesService {

    private final StartTaskFactory startTaskFactory;
    private final ReadTaskStatusFactory readTaskStatusFactory;
    private final ReadTaskLogsFactory readTaskLogsFactory;
    private final CancelTaskFactory cancelTaskFactory;

    public ExecutorTaskUseCasesService(StartTaskFactory startTaskFactory, ReadTaskStatusFactory readTaskStatusFactory,
                                       ReadTaskLogsFactory readTaskLogsFactory, CancelTaskFactory cancelTaskFactory) {
        this.startTaskFactory = startTaskFactory;
        this.readTaskStatusFactory = readTaskStatusFactory;
        this.readTaskLogsFactory = readTaskLogsFactory;
        this.cancelTaskFactory = cancelTaskFactory;
    }

    public BuildName start(StartTaskCommand command) {
        StartTaskResultHolder holder = new StartTaskResultHolder();
        startTaskFactory.buildStartTask(command, holder).execute();
        return holder.build;
    }

    public TaskStatus status(ReadTaskStatusCommand command) {
        ReadTaskStatusResultHolder holder = new ReadTaskStatusResultHolder();
        readTaskStatusFactory.buildReadTaskStatus(command, holder).execute();
        return new TaskStatus(holder.build, holder.status);
    }

    public TaskLogs logs(ReadTaskLogsCommand command) {
        ReadTaskLogsResultHolder holder = new ReadTaskLogsResultHolder();
        readTaskLogsFactory.buildReadTaskLogs(command, holder).execute();
        return new TaskLogs(holder.content, holder.generatedAt);
    }

    public void cancel(CancelTaskCommand command) {
        cancelTaskFactory.buildCancelTask(command, build -> { }).execute();
    }

    public record TaskStatus(BuildName build, RunStatus status) {
    }

    public record TaskLogs(String content, Instant generatedAt) {
    }

    private static final class StartTaskResultHolder implements StartTaskPresenter {
        private BuildName build;

        @Override
        public void presentStarted(BuildName build) {
            this.build = build;
        }
    }

    private static final class ReadTaskStatusResultHolder implements ReadTaskStatusPresenter {
        private BuildName build;
        private RunStatus status;

        @Override
        public void presentStatus(BuildName build, RunStatus status) {
            this.build = build;
            this.status = status;
        }
    }

    private static final class ReadTaskLogsResultHolder implements ReadTaskLogsPresenter {
        private String content;
        private Instant generatedAt;

        @Override
        public void presentLogs(String content, Instant generatedAt) {
            this.content = content;
            this.generatedAt = generatedAt;
        }
    }
}
