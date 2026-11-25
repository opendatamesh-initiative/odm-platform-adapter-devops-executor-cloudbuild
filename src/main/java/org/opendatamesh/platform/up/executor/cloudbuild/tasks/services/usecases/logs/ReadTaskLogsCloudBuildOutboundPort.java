package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Build;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;

import java.util.Optional;

interface ReadTaskLogsCloudBuildOutboundPort {

    Optional<Build> findBuild(CloudBuildAccess access, BuildName build);
}
