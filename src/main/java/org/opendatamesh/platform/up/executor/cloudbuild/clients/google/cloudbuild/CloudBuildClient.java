package org.opendatamesh.platform.up.executor.cloudbuild.clients.google.cloudbuild;

import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Build;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.GitRef;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Trigger;

import java.util.Map;
import java.util.Optional;

/** Cloud Build REST v1, in the project and location of the access. */
public interface CloudBuildClient {

    Optional<Trigger> findTrigger(CloudBuildAccess access, String name);

    BuildName runTrigger(CloudBuildAccess access, Trigger trigger, GitRef ref, Map<String, String> substitutions);

    Optional<Build> findBuild(CloudBuildAccess access, BuildName build);

    void cancelBuild(CloudBuildAccess access, BuildName build);
}
