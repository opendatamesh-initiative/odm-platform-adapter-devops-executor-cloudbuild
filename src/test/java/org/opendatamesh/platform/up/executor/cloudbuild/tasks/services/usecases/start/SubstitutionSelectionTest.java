package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.start;

import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Trigger;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCaseException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Substitution rules of BDMD-5810. */
class SubstitutionSelectionTest {

    private static final Trigger TRIGGER = new Trigger("id", "dp-release", Map.of("_ENV", "lab", "_DATASET", "raw", "_DATA_SET", ""));

    /**
     * Feature: Pipeline parameters become substitutions
     *
     * Scenario: Parameters declared by the trigger are sent, the others are dropped
     *   Given a trigger declaring _ENV and _DATASET
     *   When the task parameters are env, dataset and typo
     *   Then the substitutions sent are _ENV and _DATASET
     *   And typo is reported as dropped
     */
    @Test
    void whenParametersDeclaredByTriggerThenSelectedAndOthersDropped() {
        SubstitutionSelection.Selection selection = new SubstitutionSelection()
                .select(TRIGGER, Map.of("env", "prd", "dataset", "curated", "typo", "x"));

        assertEquals(Map.of("_ENV", "prd", "_DATASET", "curated"), selection.substitutions());
        assertEquals(List.of("typo"), selection.dropped());
    }

    @Test
    void whenNameHasPrefixOrSymbolsThenNormalisedKey() {
        assertEquals("_ENV", SubstitutionSelection.substitutionKey("_env"));
        assertEquals("_DATA_SET", SubstitutionSelection.substitutionKey("data-set"));
        assertEquals("_A_B", SubstitutionSelection.substitutionKey("a.b"));
    }

    /**
     * Scenario: Two parameters with the same substitution key are refused
     *   When the task parameters are env and ENV
     *   Then the start fails with 400 naming both parameters
     */
    @Test
    void whenTwoParametersShareAKeyThenInvalid() {
        UseCaseException failure = assertThrows(UseCaseException.class,
                () -> new SubstitutionSelection().select(TRIGGER, Map.of("env", "a", "ENV", "b")));

        assertEquals(UseCaseException.Reason.INVALID, failure.getReason());
        assertTrue(failure.getMessage().contains("ENV and env"), failure.getMessage());
        assertTrue(failure.getMessage().contains("_ENV"), failure.getMessage());
    }

    /**
     * Scenario: A value longer than Cloud Build allows is refused
     *   When a parameter value is longer than 4000 bytes
     *   Then the start fails with 400 naming the substitution
     */
    @Test
    void whenValueTooLongThenInvalid() {
        UseCaseException failure = assertThrows(UseCaseException.class,
                () -> new SubstitutionSelection().select(TRIGGER, Map.of("env", "é".repeat(2001))));

        assertEquals(UseCaseException.Reason.INVALID, failure.getReason());
        assertTrue(failure.getMessage().contains("_ENV"), failure.getMessage());
    }

    @Test
    void whenUndeclaredParametersShareAKeyThenBothDroppedNotRefused() {
        SubstitutionSelection.Selection selection = new SubstitutionSelection()
                .select(TRIGGER, Map.of("other-key", "a", "other_key", "b", "env", "prd"));

        assertEquals(Map.of("_ENV", "prd"), selection.substitutions());
        assertEquals(List.of("other-key", "other_key"), selection.dropped());
    }

    @Test
    void whenDeclaredValueNullThenInvalid() {
        Map<String, String> parameters = new java.util.HashMap<>();
        parameters.put("env", null);

        UseCaseException failure = assertThrows(UseCaseException.class, () -> new SubstitutionSelection().select(TRIGGER, parameters));

        assertEquals(UseCaseException.Reason.INVALID, failure.getReason());
        assertTrue(failure.getMessage().contains("_ENV"), failure.getMessage());
    }

    @Test
    void whenUndeclaredValueTooLongThenDroppedNotRefused() {
        SubstitutionSelection.Selection selection = new SubstitutionSelection()
                .select(TRIGGER, Map.of("notes", "x".repeat(5000)));

        assertTrue(selection.substitutions().isEmpty());
        assertEquals(List.of("notes"), selection.dropped());
    }
}
