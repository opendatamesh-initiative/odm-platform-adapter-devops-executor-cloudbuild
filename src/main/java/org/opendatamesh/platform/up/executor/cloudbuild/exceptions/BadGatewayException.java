package org.opendatamesh.platform.up.executor.cloudbuild.exceptions;

import jakarta.ws.rs.core.Response;

/** A Google API refused the credentials, failed, or answered something the executor cannot read. */
public class BadGatewayException extends ExecutorCloudBuildApiException {

    private static final long serialVersionUID = 1L;

    public BadGatewayException(String message) {
        super(message);
    }

    public BadGatewayException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorName() {
        return getClass().getSimpleName();
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.BAD_GATEWAY;
    }
}
