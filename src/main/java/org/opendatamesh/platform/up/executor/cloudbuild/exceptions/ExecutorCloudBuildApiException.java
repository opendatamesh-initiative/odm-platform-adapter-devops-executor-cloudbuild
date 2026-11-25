package org.opendatamesh.platform.up.executor.cloudbuild.exceptions;

import jakarta.ws.rs.core.Response;

public abstract class ExecutorCloudBuildApiException extends RuntimeException {

    private static final long serialVersionUID = 3876573329263306459L;

    public ExecutorCloudBuildApiException() {
        super();
    }

    public ExecutorCloudBuildApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ExecutorCloudBuildApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExecutorCloudBuildApiException(String message) {
        super(message);
    }

    public ExecutorCloudBuildApiException(Throwable cause) {
        super(cause);
    }

    public abstract String getErrorName();

    public abstract Response.Status getStatus();
}
