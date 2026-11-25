package org.opendatamesh.platform.up.executor.cloudbuild.exceptions;

import jakarta.ws.rs.core.Response;

public class BadRequestException extends ExecutorCloudBuildApiException {

    private static final long serialVersionUID = 1L;

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorName() {
        return getClass().getSimpleName();
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.BAD_REQUEST;
    }
}
