package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.cancel;

import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCasePresenter;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;

public interface CancelTaskPresenter extends UseCasePresenter {

    void presentCanceled(BuildName build);
}
