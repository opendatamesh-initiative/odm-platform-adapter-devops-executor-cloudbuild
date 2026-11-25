package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCasePresenter;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;

public interface StartTaskPresenter extends UseCasePresenter {

    void presentStarted(BuildName build);
}
