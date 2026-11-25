package org.opendatamesh.platform.up.executor.cloudbuild.clients.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.BadGatewayException;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.BadRequestException;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.ExecutorCloudBuildApiException;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.NotFoundException;

/**
 * An error answered by a Google API, read from its body: the Google API shape {@code {"error":{"code","status",
 * "message"}}} or the OAuth shape {@code {"error","error_description"}}. Bodies that are not JSON are kept as text.
 */
public record GoogleApiError(int code, String status, String message) {

    private static final int MAX_MESSAGE_CHARS = 300;
    private static final int BAD_REQUEST = 400;
    private static final int UNAUTHORIZED = 401;
    private static final int FORBIDDEN = 403;
    private static final int NOT_FOUND = 404;

    public static GoogleApiError from(int code, String body, ObjectMapper objectMapper) {
        JsonNode json = parse(body, objectMapper);
        JsonNode error = json == null ? null : json.get("error");
        if (error != null && error.isObject()) {
            return new GoogleApiError(code, error.path("status").asText(""), cut(error.path("message").asText("")));
        }
        if (error != null && error.isTextual()) {
            return new GoogleApiError(code, error.asText(), cut(json.path("error_description").asText("")));
        }
        return new GoogleApiError(code, "", cut(body == null ? "" : body.strip()));
    }

    public boolean isAuthorizationFailure() {
        return code == UNAUTHORIZED || code == FORBIDDEN;
    }

    /** The service exception for this error. Messages name the API and the action, never a credential. */
    public ExecutorCloudBuildApiException toException(GoogleRequest request) {
        String detail = detail();
        if (isAuthorizationFailure()) {
            return new GoogleAccessDeniedException(request.api() + " refused the credentials when trying to "
                    + request.action() + " (HTTP " + code + detail + ")");
        }
        if (code == BAD_REQUEST) {
            return new BadRequestException(request.api() + " rejected the request to " + request.action()
                    + " (HTTP " + code + detail + ")");
        }
        if (code == NOT_FOUND) {
            return new NotFoundException(request.api() + " found nothing when trying to " + request.action()
                    + " (HTTP " + code + detail + ")");
        }
        return new BadGatewayException(request.api() + " failed to " + request.action() + " (HTTP " + code + detail + ")");
    }

    private String detail() {
        StringBuilder detail = new StringBuilder();
        if (!status.isBlank()) {
            detail.append(' ').append(status);
        }
        if (!message.isBlank()) {
            detail.append(": ").append(message);
        }
        return detail.toString();
    }

    private static JsonNode parse(String body, ObjectMapper objectMapper) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            return null;
        }
    }

    private static String cut(String text) {
        return text.length() <= MAX_MESSAGE_CHARS ? text : text.substring(0, MAX_MESSAGE_CHARS) + "…";
    }
}
