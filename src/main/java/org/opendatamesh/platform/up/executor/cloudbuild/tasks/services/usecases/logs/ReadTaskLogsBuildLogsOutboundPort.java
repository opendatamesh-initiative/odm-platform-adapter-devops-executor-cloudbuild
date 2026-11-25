package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Build;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LogDestination;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LogRead;

interface ReadTaskLogsBuildLogsOutboundPort {

    LogRead read(CloudBuildAccess access, Build build, LogDestination destination);

    long maxLogBytes();
}
