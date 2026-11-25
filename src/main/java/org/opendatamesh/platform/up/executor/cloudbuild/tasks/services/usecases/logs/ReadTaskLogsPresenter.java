package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCasePresenter;
import java.time.Instant;

public interface ReadTaskLogsPresenter extends UseCasePresenter {

    void presentLogs(String content, Instant generatedAt);
}
