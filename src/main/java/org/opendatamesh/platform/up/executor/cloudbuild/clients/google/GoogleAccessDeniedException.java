package org.opendatamesh.platform.up.executor.cloudbuild.clients.google;

import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.BadGatewayException;

/** A Google API refused the credentials (HTTP 401 or 403); the message quotes Google's reason. */
public class GoogleAccessDeniedException extends BadGatewayException {

    private static final long serialVersionUID = 1L;

    public GoogleAccessDeniedException(String message) {
        super(message);
    }
}
