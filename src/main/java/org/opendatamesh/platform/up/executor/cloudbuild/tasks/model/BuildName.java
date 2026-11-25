package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Full resource name of a build, {@code projects/{project}/locations/{location}/builds/{id}}. It is the provider run
 * id returned to DevOps, so status, logs and cancel need nothing else to find the build.
 */
public record BuildName(String project, String location, String buildId) {

    private static final Pattern FORMAT = Pattern.compile("projects/([^/\\s]+)/locations/([^/\\s]+)/builds/([^/\\s]+)");

    public static Optional<BuildName> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        Matcher matcher = FORMAT.matcher(value);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new BuildName(matcher.group(1), matcher.group(2), matcher.group(3)));
    }

    public String value() {
        return "projects/" + project + "/locations/" + location + "/builds/" + buildId;
    }
}
