package org.opendatamesh.platform.up.executor.cloudbuild.clients.google.logs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.GoogleAccessDeniedException;
import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.GoogleApiTransport;
import org.opendatamesh.platform.up.executor.cloudbuild.clients.google.GoogleRequest;
import org.opendatamesh.platform.up.executor.cloudbuild.config.CloudBuildProperties;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.BadGatewayException;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.BadRequestException;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.NotFoundException;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Build;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LogContent;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LogRead;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LogUnavailable;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Cloud Storage JSON API v1 (media download of {@code log-<id>.txt} from the build's logs bucket) and Cloud Logging v2
 * ({@code entries:list} on the build's entries, oldest first). A source that cannot be read becomes a
 * {@link LogUnavailable} with the reason, naming the role to grant when Google refused the credentials.
 */
@ApplicationScoped
public class BuildLogsClientImpl implements BuildLogsClient {

    private static final String STORAGE_API = "Cloud Storage";
    private static final String LOGGING_API = "Cloud Logging";
    private static final String GCS_SCHEME = "gs://";

    private final GoogleApiTransport transport;
    private final CloudBuildProperties.Endpoints endpoints;
    private final int pageSize;

    public BuildLogsClientImpl(GoogleApiTransport transport, CloudBuildProperties properties) {
        this.transport = transport;
        this.endpoints = properties.endpoints();
        this.pageSize = properties.logs().cloudLoggingPageSize();
    }

    @Override
    public LogRead readFromStorage(CloudBuildAccess access, Build build, long maxBytes) {
        Optional<String> bucketUrl = build.logsBucket().filter(bucket -> bucket.startsWith(GCS_SCHEME));
        if (bucketUrl.isEmpty()) {
            return new LogUnavailable("the build has no Cloud Storage logs bucket");
        }
        LogObject object = LogObject.of(bucketUrl.get(), build.name().buildId());
        GoogleRequest request = new GoogleRequest(STORAGE_API, "read " + object.uri(), endpoints.storage()
                + "/storage/v1/b/" + GoogleApiTransport.encodePathSegment(object.bucket())
                + "/o/" + GoogleApiTransport.encodePathSegment(object.name()) + "?alt=media");
        try {
            return transport.readText(request, access.token(), maxBytes)
                    .<LogRead>map(text -> new LogContent(text.text(), text.truncated()))
                    .orElseGet(() -> new LogUnavailable("no log object " + object.uri()));
        } catch (GoogleAccessDeniedException e) {
            return new LogUnavailable("Cloud Storage refused to read " + object.uri()
                    + ": grant roles/storage.objectViewer on the bucket (and roles/logging.viewAccessor for a user-owned bucket)");
        } catch (BadGatewayException | BadRequestException | NotFoundException e) {
            return new LogUnavailable(e.getMessage());
        }
    }

    @Override
    public LogRead readFromCloudLogging(CloudBuildAccess access, Build build, long maxBytes) {
        String buildId = build.name().buildId();
        GoogleRequest request = new GoogleRequest(LOGGING_API, "list the log entries of build " + buildId,
                endpoints.logging() + "/v2/entries:list");
        try {
            return collectEntries(access, build, request, maxBytes);
        } catch (GoogleAccessDeniedException e) {
            return new LogUnavailable("Cloud Logging refused to list the entries of build " + buildId
                    + ": grant roles/logging.viewer on project " + build.name().project());
        } catch (BadGatewayException | BadRequestException | NotFoundException e) {
            return new LogUnavailable(e.getMessage());
        }
    }

    private LogRead collectEntries(CloudBuildAccess access, Build build, GoogleRequest request, long maxBytes) {
        StringBuilder text = new StringBuilder();
        long bytes = 0;
        boolean any = false;
        String pageToken = "";
        do {
            JsonNode page = transport.queryJson(request, access.token(), entriesQuery(build, pageToken));
            for (JsonNode entry : page.path("entries")) {
                any = true;
                String line = lineOf(entry) + "\n";
                long lineBytes = line.getBytes(StandardCharsets.UTF_8).length;
                if (bytes + lineBytes > maxBytes) {
                    return new LogContent(text.toString(), true);
                }
                text.append(line);
                bytes += lineBytes;
            }
            pageToken = page.path("nextPageToken").asText("");
        } while (!pageToken.isBlank());
        return any ? new LogContent(text.toString(), false)
                : new LogUnavailable("Cloud Logging has no entries for build " + build.name().buildId());
    }

    private ObjectNode entriesQuery(Build build, String pageToken) {
        ObjectNode query = transport.objectMapper().createObjectNode();
        query.putArray("resourceNames").add("projects/" + build.name().project());
        query.put("filter", "resource.type=\"build\" AND resource.labels.build_id=\"" + build.name().buildId() + "\"");
        query.put("orderBy", "timestamp asc");
        query.put("pageSize", pageSize);
        if (!pageToken.isBlank()) {
            query.put("pageToken", pageToken);
        }
        return query;
    }

    private static String lineOf(JsonNode entry) {
        if (entry.hasNonNull("textPayload")) {
            return entry.get("textPayload").asText();
        }
        JsonNode message = entry.path("jsonPayload").path("message");
        return message.isMissingNode() ? entry.path("jsonPayload").toString() : message.asText();
    }

    /** The log object of a build in a logs bucket given as {@code gs://<bucket>[/<prefix>]}. */
    private record LogObject(String bucket, String name) {

        static LogObject of(String bucketUrl, String buildId) {
            String path = bucketUrl.substring(GCS_SCHEME.length());
            int slash = path.indexOf('/');
            String bucket = slash < 0 ? path : path.substring(0, slash);
            String prefix = slash < 0 ? "" : path.substring(slash + 1).replaceAll("/+$", "");
            return new LogObject(bucket, (prefix.isEmpty() ? "" : prefix + "/") + "log-" + buildId + ".txt");
        }

        String uri() {
            return GCS_SCHEME + bucket + "/" + name;
        }
    }
}
