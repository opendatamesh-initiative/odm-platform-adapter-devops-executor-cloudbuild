package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildNameTest {

    @Test
    void whenFullResourceNameThenParsedAndRoundTrips() {
        String value = "projects/demo-project/locations/europe-west8/builds/b-1";

        BuildName name = BuildName.parse(value).orElseThrow();

        assertEquals(new BuildName("demo-project", "europe-west8", "b-1"), name);
        assertEquals(value, name.value());
        assertTrue(new CloudBuildTarget("demo-project", "europe-west8").owns(name));
        assertFalse(new CloudBuildTarget("demo-project", "global").owns(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "b-1", "projects/p/locations/l/builds/", "projects/p/locations/l/builds/b/extra",
            "projects/p/builds/b", "projects/p q/locations/l/builds/b"})
    void whenNotABuildResourceNameThenEmpty(String value) {
        assertTrue(BuildName.parse(value).isEmpty());
    }
}
