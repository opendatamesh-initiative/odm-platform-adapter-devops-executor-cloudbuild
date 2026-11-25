package org.opendatamesh.platform.up.executor.cloudbuild.clients.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.cloudbuild.config.CloudBuildProperties;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.BadGatewayException;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.InternalException;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.client.ClientException;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.client.ClientResourceMappingException;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.AccessToken;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.client.RestUtils;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.client.RestUtilsFactory;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.client.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * HTTP mechanics shared by the Google clients: one {@link HttpClient} (connect timeout, no redirects), JSON calls
 * through the template's {@link RestUtils}, form posts and bounded text downloads on the same client, bounded retries
 * of idempotent calls, and translation of Google errors into service exceptions. It makes no business decision.
 */
@ApplicationScoped
public class GoogleApiTransport {

    private static final Logger logger = LoggerFactory.getLogger(GoogleApiTransport.class);
    private static final Set<Integer> RETRYABLE_CODES = Set.of(429, 500, 502, 503, 504);
    private static final int NOT_FOUND = 404;
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final RestUtils rest;
    private final CloudBuildProperties.Http http;

    public GoogleApiTransport(CloudBuildProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.http = properties.http();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(http.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.rest = RestUtilsFactory.getRestUtils(httpClient, objectMapper);
    }

    /** GET of a JSON resource with a bearer token; a missing resource (404) is empty. Retried. */
    public Optional<JsonNode> readJson(GoogleRequest request, AccessToken token) {
        return withRetries(request, true, () -> {
            try {
                JsonNode json = rest.genericGet(request.url(), bearer(token), null, JsonNode.class);
                if (json == null) {
                    throw new BadGatewayException(request.api() + " returned an empty response when trying to " + request.action());
                }
                return Optional.of(json);
            } catch (ClientException e) {
                if (e.getCode() == NOT_FOUND) {
                    return Optional.empty();
                }
                throw e;
            } catch (ClientResourceMappingException e) {
                throw unreadable(request, e);
            }
        });
    }

    /** POST of a JSON body that only reads (a list, a token); retried. */
    public JsonNode queryJson(GoogleRequest request, AccessToken token, JsonNode body) {
        return withRetries(request, true, () -> postJsonOnce(request, token, body));
    }

    /** POST of a JSON body that changes something (run, cancel); never retried. */
    public JsonNode writeJson(GoogleRequest request, AccessToken token, JsonNode body) {
        return withRetries(request, false, () -> postJsonOnce(request, token, body));
    }

    /** POST of an {@code application/x-www-form-urlencoded} body without a bearer token (the OAuth token endpoint). */
    public JsonNode postForm(GoogleRequest request, Map<String, String> form) {
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(request.url()))
                .timeout(http.requestTimeout())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(formBody(form)))
                .build();
        return withRetries(request, true, () -> readJsonBody(request, sendForText(httpRequest)));
    }

    /** GET of a JSON resource with plain headers, outside {@link RestUtils} so an unreachable host is told apart. */
    public JsonNode readJsonWithHeaders(GoogleRequest request, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(request.url()))
                .timeout(http.requestTimeout())
                .header("Accept", "application/json")
                .GET();
        headers.forEach(builder::header);
        HttpRequest httpRequest = builder.build();
        return withRetries(request, true, () -> readJsonBody(request, sendForText(httpRequest)));
    }

    /** GET of a text resource with a bearer token, read up to {@code maxBytes}; a missing resource (404) is empty. */
    public Optional<BoundedText> readText(GoogleRequest request, AccessToken token, long maxBytes) {
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(request.url()))
                .timeout(http.requestTimeout())
                .header(AUTHORIZATION, BEARER + token.value())
                .GET()
                .build();
        return withRetries(request, true, () -> {
            try {
                return Optional.of(sendForBoundedText(httpRequest, maxBytes));
            } catch (ClientException e) {
                if (e.getCode() == NOT_FOUND) {
                    return Optional.empty();
                }
                throw e;
            }
        });
    }

    public static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public ObjectMapper objectMapper() {
        return objectMapper;
    }

    private JsonNode postJsonOnce(GoogleRequest request, AccessToken token, JsonNode body) {
        try {
            JsonNode response = rest.genericPost(request.url(), bearer(token), body, JsonNode.class);
            return response == null ? objectMapper.createObjectNode() : response;
        } catch (ClientResourceMappingException e) {
            throw unreadable(request, e);
        }
    }

    private <T> T withRetries(GoogleRequest request, boolean retryable, Supplier<T> call) {
        int attempt = 0;
        while (true) {
            try {
                return call.get();
            } catch (ClientException e) {
                if (!retryable || attempt >= http.readRetries() || !RETRYABLE_CODES.contains(e.getCode())) {
                    throw GoogleApiError.from(e.getCode(), e.getResponseBody(), objectMapper).toException(request);
                }
            } catch (UncheckedIOException e) {
                if (!retryable || attempt >= http.readRetries()) {
                    if (isResponseTimeout(e)) {
                        throw new BadGatewayException(request.api() + " did not answer within "
                                + http.requestTimeout() + " when trying to " + request.action(), e);
                    }
                    throw new GoogleApiUnreachableException(request.api() + " is not reachable when trying to "
                            + request.action() + " (" + e.getCause().getClass().getSimpleName() + ")", e);
                }
            }
            attempt++;
            logger.warn("{} failed to {}, retry {} of {}", request.api(), request.action(), attempt, http.readRetries());
            pause();
        }
    }

    private String sendForText(HttpRequest httpRequest) {
        try {
            HttpResponse<byte[]> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            String body = response.body() == null ? "" : new String(response.body(), StandardCharsets.UTF_8);
            if (response.statusCode() >= 400) {
                throw new ClientException(response.statusCode(), body);
            }
            return body;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalException("Interrupted while calling a Google API", e);
        }
    }

    private BoundedText sendForBoundedText(HttpRequest httpRequest, long maxBytes) {
        try {
            HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() >= 400) {
                    throw new ClientException(response.statusCode(), new String(body.readNBytes(64 * 1024), StandardCharsets.UTF_8));
                }
                byte[] bytes = body.readNBytes((int) Math.min(Integer.MAX_VALUE - 8L, maxBytes + 1));
                boolean truncated = bytes.length > maxBytes;
                int length = truncated ? completeCharacters(bytes, (int) maxBytes) : bytes.length;
                return new BoundedText(new String(bytes, 0, length, StandardCharsets.UTF_8), truncated);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalException("Interrupted while calling a Google API", e);
        }
    }

    /** A response timeout means the host answered too slowly; a connect timeout still means it was not reached. */
    private static boolean isResponseTimeout(UncheckedIOException e) {
        return e.getCause() instanceof HttpTimeoutException && !(e.getCause() instanceof HttpConnectTimeoutException);
    }

    /** The cut, moved back so that it never splits a multi-byte UTF-8 character. */
    private static int completeCharacters(byte[] bytes, int cut) {
        int length = cut;
        while (length > 0 && (bytes[length] & 0xC0) == 0x80) {
            length--;
        }
        return length;
    }

    private JsonNode readJsonBody(GoogleRequest request, String body) {
        try {
            return objectMapper.readTree(body);
        } catch (IOException e) {
            throw unreadable(request, e);
        }
    }

    private void pause() {
        try {
            Thread.sleep(http.retryBackoff().toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalException("Interrupted while waiting to retry a Google API call", e);
        }
    }

    private static BadGatewayException unreadable(GoogleRequest request, Exception cause) {
        return new BadGatewayException(request.api() + " returned an unreadable response when trying to " + request.action(), cause);
    }

    private static List<HttpHeader> bearer(AccessToken token) {
        return List.of(new HttpHeader(AUTHORIZATION, BEARER + token.value()));
    }

    private static String formBody(Map<String, String> form) {
        return form.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
