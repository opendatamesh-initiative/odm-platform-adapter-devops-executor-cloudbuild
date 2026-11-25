package org.opendatamesh.platform.up.executor.cloudbuild.clients.google;

import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.BadGatewayException;

/** A Google endpoint could not be reached at all (connection refused, DNS, timeout). */
public class GoogleApiUnreachableException extends BadGatewayException {

    private static final long serialVersionUID = 1L;

    public GoogleApiUnreachableException(String message, Throwable cause) {
        super(message, cause);
    }
}
