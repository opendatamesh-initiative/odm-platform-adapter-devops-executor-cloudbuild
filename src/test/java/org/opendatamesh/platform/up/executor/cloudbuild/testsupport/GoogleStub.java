package org.opendatamesh.platform.up.executor.cloudbuild.testsupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Scriptable fake of the Google endpoints the executor calls, served by the JDK HTTP server: the metadata server, IAM
 * Credentials, the OAuth token endpoint, Cloud Build, Cloud Storage and Cloud Logging, all on one base URL.
 *
 * <p>Every request is recorded with its headers. Replies are scripted by method and raw path (query included when the
 * script has one); an unscripted request gets 404 with a Google error body. {@link #reset()} installs the defaults:
 * the executor and impersonated tokens, trigger dp-release readable by name, its run, and a WORKING build.
 */
public final class GoogleStub implements AutoCloseable {

    public static final String EXECUTOR_TOKEN = "executor-token";
    public static final String IMPERSONATED_TOKEN = "impersonated-token";
    public static final String KEY_TOKEN = "key-exchanged-token";
    public static final String IMPERSONATED_SA = "builder@demo-project.iam.gserviceaccount.com";
    public static final String METADATA_TOKEN = "/computeMetadata/v1/instance/service-accounts/default/token";
    public static final String GENERATE_ACCESS_TOKEN = "/v1/projects/-/serviceAccounts/builder%40demo-project.iam.gserviceaccount.com:generateAccessToken";
    public static final String OAUTH_TOKEN = "/token";
    public static final String TRIGGER_BY_NAME = Fixtures.LOCATION_PATH + "/triggers/" + Fixtures.TRIGGER_NAME;
    public static final String TRIGGERS = Fixtures.LOCATION_PATH + "/triggers";
    public static final String RUN = Fixtures.LOCATION_PATH + "/triggers/" + Fixtures.TRIGGER_ID + ":run";
    public static final String CANCEL = Fixtures.BUILD_PATH + ":cancel";
    public static final String ENTRIES_LIST = "/v2/entries:list";
    public static final String LOG_OBJECT = "/storage/v1/b/demo-project-build-logs/o/log-" + Fixtures.BUILD_ID + ".txt";

    public record Recorded(String method, String pathWithQuery, String body, Map<String, String> headers) {

        public String header(String name) {
            return headers.get(name.toLowerCase());
        }

        public JsonNode json() {
            try {
                return Fixtures.JSON.readTree(body);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        public Map<String, String> form() {
            Map<String, String> form = new HashMap<>();
            for (String pair : body.split("&")) {
                int equals = pair.indexOf('=');
                form.put(URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8));
            }
            return form;
        }
    }

    private record Reply(int status, String contentType, String body) {
    }

    private final HttpServer server;
    private final Map<String, Reply> replies = new ConcurrentHashMap<>();
    private final List<Recorded> recorded = new CopyOnWriteArrayList<>();

    public GoogleStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        reset();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void reset() {
        replies.clear();
        recorded.clear();
        reply("GET", METADATA_TOKEN, 200,
                Fixtures.JSON.createObjectNode().put("access_token", EXECUTOR_TOKEN).put("expires_in", 3599).put("token_type", "Bearer").toString());
        reply("POST", GENERATE_ACCESS_TOKEN, 200,
                Fixtures.JSON.createObjectNode().put("accessToken", IMPERSONATED_TOKEN).put("expireTime", "2026-11-25T13:00:00Z").toString());
        reply("POST", OAUTH_TOKEN, 200,
                Fixtures.JSON.createObjectNode().put("access_token", KEY_TOKEN).put("expires_in", 3599).put("token_type", "Bearer").toString());
        reply("GET", TRIGGER_BY_NAME, 200, Fixtures.trigger());
        reply("POST", RUN, 200, Fixtures.runOperation());
        reply("GET", Fixtures.BUILD_PATH, 200, Fixtures.build("WORKING", Fixtures.LOGS_BUCKET, "LEGACY"));
        reply("POST", CANCEL, 200, Fixtures.build("CANCELLED", Fixtures.LOGS_BUCKET, "LEGACY"));
    }

    public void reply(String method, String pathWithQuery, int status, String body) {
        replies.put(method + " " + pathWithQuery, new Reply(status, "application/json", body));
    }

    public void replyText(String method, String pathWithQuery, int status, String body) {
        replies.put(method + " " + pathWithQuery, new Reply(status, "text/plain; charset=utf-8", body));
    }

    public List<Recorded> requests(String method, String path) {
        List<Recorded> matching = new ArrayList<>();
        for (Recorded request : recorded) {
            if (request.method().equals(method) && pathOf(request.pathWithQuery()).equals(path)) {
                matching.add(request);
            }
        }
        return matching;
    }

    public Recorded lastRequest(String method, String path) {
        List<Recorded> matching = requests(method, path);
        if (matching.isEmpty()) {
            throw new IllegalStateException("No " + method + " " + path + " received; got " + recorded.stream()
                    .map(r -> r.method() + " " + r.pathWithQuery()).toList());
        }
        return matching.get(matching.size() - 1);
    }

    public List<Recorded> all() {
        return List.copyOf(recorded);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String pathWithQuery = exchange.getRequestURI().getRawPath()
                + (exchange.getRequestURI().getRawQuery() == null ? "" : "?" + exchange.getRequestURI().getRawQuery());
        Map<String, String> headers = new ConcurrentHashMap<>();
        exchange.getRequestHeaders().forEach((name, values) -> headers.put(name.toLowerCase(), String.join(",", values)));
        recorded.add(new Recorded(exchange.getRequestMethod(), pathWithQuery, body, headers));
        Reply reply = replies.getOrDefault(exchange.getRequestMethod() + " " + pathWithQuery,
                replies.getOrDefault(exchange.getRequestMethod() + " " + pathOf(pathWithQuery),
                        new Reply(404, "application/json", Fixtures.googleError(404, "NOT_FOUND", "Requested entity was not found."))));
        byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", reply.contentType());
        exchange.sendResponseHeaders(reply.status(), bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private static String pathOf(String pathWithQuery) {
        int query = pathWithQuery.indexOf('?');
        return query < 0 ? pathWithQuery : pathWithQuery.substring(0, query);
    }
}
