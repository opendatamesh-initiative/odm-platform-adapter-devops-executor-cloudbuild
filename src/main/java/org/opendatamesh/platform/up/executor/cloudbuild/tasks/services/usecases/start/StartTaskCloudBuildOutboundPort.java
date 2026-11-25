package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.GitRef;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Trigger;

import java.util.Map;
import java.util.Optional;

interface StartTaskCloudBuildOutboundPort {

    Optional<Trigger> findTrigger(CloudBuildAccess access, String name);

    BuildName runTrigger(CloudBuildAccess access, Trigger trigger, GitRef ref, Map<String, String> substitutions);
}
