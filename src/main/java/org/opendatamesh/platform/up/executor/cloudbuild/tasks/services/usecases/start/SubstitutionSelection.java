package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Trigger;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCaseException;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCaseException.Reason;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Turns the pipeline parameters of a task into the substitutions of a build: each parameter name becomes a
 * substitution key ({@code env} → {@code _ENV}), and only the keys the trigger declares are sent. The trigger's default
 * applies to a declared key with no parameter.
 */
final class SubstitutionSelection {

    static final int MAX_SUBSTITUTIONS = 200;
    static final int MAX_KEY_BYTES = 100;
    static final int MAX_VALUE_BYTES = 4000;

    record Selection(SortedMap<String, String> substitutions, List<String> dropped) {
    }

    Selection select(Trigger trigger, Map<String, String> parameters) {
        Map<String, List<String>> parametersByKey = groupByKey(parameters);
        rejectCollisions(trigger, parametersByKey);
        SortedMap<String, String> substitutions = new TreeMap<>();
        List<String> dropped = new ArrayList<>();
        parametersByKey.forEach((key, names) -> {
            if (declaredByTrigger(trigger, key)) {
                substitutions.put(checkedKey(key), checkedValue(key, parameters.get(names.get(0))));
            } else {
                dropped.addAll(names);
            }
        });
        rejectTooMany(substitutions);
        return new Selection(substitutions, dropped.stream().sorted().toList());
    }

    private Map<String, List<String>> groupByKey(Map<String, String> parameters) {
        Map<String, List<String>> byKey = new TreeMap<>();
        parameters.keySet().stream().sorted()
                .forEach(name -> byKey.computeIfAbsent(substitutionKey(name), key -> new ArrayList<>()).add(name));
        return byKey;
    }

    /** {@code _} followed by the name in upper case, any character outside {@code [A-Z0-9_]} replaced by {@code _}. */
    static String substitutionKey(String parameterName) {
        String withoutPrefix = parameterName.startsWith("_") ? parameterName.substring(1) : parameterName;
        return "_" + withoutPrefix.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
    }

    /** Only keys the trigger declares can collide: parameters that would be dropped anyway never block the start. */
    private static void rejectCollisions(Trigger trigger, Map<String, List<String>> parametersByKey) {
        parametersByKey.forEach((key, names) -> {
            if (names.size() > 1 && declaredByTrigger(trigger, key)) {
                throw new UseCaseException(Reason.INVALID, "Pipeline parameters " + String.join(" and ", names)
                        + " all become substitution " + key + ": rename them so that each key is unique");
            }
        });
    }

    private static boolean declaredByTrigger(Trigger trigger, String key) {
        return trigger.substitutions().containsKey(key);
    }

    private static String checkedKey(String key) {
        if (bytes(key) > MAX_KEY_BYTES) {
            throw new UseCaseException(Reason.INVALID,
                    "Substitution " + key + " is longer than " + MAX_KEY_BYTES + " bytes, the Cloud Build limit");
        }
        return key;
    }

    private static String checkedValue(String key, String value) {
        if (value == null) {
            throw new UseCaseException(Reason.INVALID, "Substitution " + key + " has no value: pipeline parameters cannot be null");
        }
        if (bytes(value) > MAX_VALUE_BYTES) {
            throw new UseCaseException(Reason.INVALID, "The value of substitution " + key + " is longer than "
                    + MAX_VALUE_BYTES + " bytes, the Cloud Build limit");
        }
        return value;
    }

    private static void rejectTooMany(Map<String, String> substitutions) {
        if (substitutions.size() > MAX_SUBSTITUTIONS) {
            throw new UseCaseException(Reason.INVALID, "The task sends " + substitutions.size()
                    + " substitutions; Cloud Build accepts at most " + MAX_SUBSTITUTIONS);
        }
    }

    private static int bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }
}
