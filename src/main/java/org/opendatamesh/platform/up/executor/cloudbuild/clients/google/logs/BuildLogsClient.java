package org.opendatamesh.platform.up.executor.cloudbuild.clients.google.logs;

import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Build;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LogRead;

/** Reads the log of a build from its Cloud Storage bucket or from Cloud Logging, up to a size bound. */
public interface BuildLogsClient {

    LogRead readFromStorage(CloudBuildAccess access, Build build, long maxBytes);

    LogRead readFromCloudLogging(CloudBuildAccess access, Build build, long maxBytes);
}
