package org.opendatamesh.platform.up.executor.cloudbuild.rest.v2.controllers;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.ExecutorCloudBuildApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The service's single error contract: the status of the exception and its message as plain text, which DevOps shows. */
@Provider
public class ExecutorCloudBuildApiExceptionMapper implements ExceptionMapper<ExecutorCloudBuildApiException> {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorCloudBuildApiExceptionMapper.class);

    @Override
    public Response toResponse(ExecutorCloudBuildApiException exception) {
        if (exception.getStatus().getFamily() == Response.Status.Family.SERVER_ERROR) {
            logger.warn("{}: {}", exception.getErrorName(), exception.getMessage());
        }
        return Response.status(exception.getStatus())
                .entity(exception.getMessage())
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}
