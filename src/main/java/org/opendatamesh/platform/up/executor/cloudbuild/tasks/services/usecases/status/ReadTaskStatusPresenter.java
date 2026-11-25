package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCasePresenter;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.RunStatus;

public interface ReadTaskStatusPresenter extends UseCasePresenter {

    void presentStatus(BuildName build, RunStatus status);
}
