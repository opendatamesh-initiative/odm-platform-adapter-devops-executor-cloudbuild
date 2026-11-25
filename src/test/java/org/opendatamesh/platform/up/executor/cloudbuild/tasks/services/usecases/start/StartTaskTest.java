package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.start;

import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.AccessToken;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.AuthMode;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildTarget;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CredentialPolicy;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.GcpCredential;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.GitRef;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.RefType;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Trigger;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCaseException;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Start use case of BDMD-5810, with fake ports. */
class StartTaskTest {

    private static final CloudBuildTarget TARGET = new CloudBuildTarget("demo-project", "europe-west8");
    private static final Trigger TRIGGER = new Trigger("trigger-id", "dp-release", Map.of("_ENV", "lab"));

    private final FakePorts ports = new FakePorts();
    private final CapturingPresenter presenter = new CapturingPresenter();

    /**
     * Feature: Start a build from a named trigger
     *
     * Scenario: A task without ref type is refused
     *   When DevOps starts a task whose ref has no type
     *   Then the answer is 400 saying ref.type must be BRANCH or TAG
     */
    @Test
    void whenRefTypeMissingThenInvalid() {
        execute(new StartTaskCommand("dp-release", "v1.2.0", null, Map.of(), null));

        UseCaseException failure = assertInstanceOf(UseCaseException.class, presenter.failure);
        assertEquals(UseCaseException.Reason.INVALID, failure.getReason());
        assertEquals("executorParameters.ref.type must be BRANCH or TAG", failure.getMessage());
        assertNull(ports.ranWith);
    }

    @Test
    void whenTagRefThenTriggerRunsWithDeclaredSubstitutionsOnly() {
        execute(new StartTaskCommand(" dp-release ", "v1.2.0", "tag", Map.of("env", "prd", "other", "x"), null));

        assertEquals(TARGET.buildName("b-1"), presenter.started);
        assertEquals(new GitRef("v1.2.0", RefType.TAG), ports.ranWith);
        assertEquals(Map.of("_ENV", "prd"), ports.substitutions);
    }

    @Test
    void whenTriggerUnknownThenNotFoundAndNothingRuns() {
        execute(new StartTaskCommand("missing", "main", "BRANCH", Map.of(), null));

        UseCaseException failure = assertInstanceOf(UseCaseException.class, presenter.failure);
        assertEquals(UseCaseException.Reason.NOT_FOUND, failure.getReason());
        assertTrue(failure.getMessage().contains("missing"));
        assertNull(ports.ranWith);
    }

    private void execute(StartTaskCommand command) {
        new StartTask(command, presenter, ports, ports).execute();
    }

    private static final class FakePorts implements StartTaskAccessOutboundPort, StartTaskCloudBuildOutboundPort {
        private GitRef ranWith;
        private Map<String, String> substitutions;

        @Override
        public CredentialPolicy credentialPolicy() {
            return new CredentialPolicy(AuthMode.WORKLOAD_IDENTITY, "x-odm-gcp-service-account-key", Optional.empty());
        }

        @Override
        public CloudBuildAccess open(GcpCredential credential) {
            return new CloudBuildAccess(TARGET, new AccessToken("token"));
        }

        @Override
        public Optional<Trigger> findTrigger(CloudBuildAccess access, String name) {
            return TRIGGER.name().equals(name) ? Optional.of(TRIGGER) : Optional.empty();
        }

        @Override
        public BuildName runTrigger(CloudBuildAccess access, Trigger trigger, GitRef ref, Map<String, String> substitutions) {
            this.ranWith = ref;
            this.substitutions = substitutions;
            return access.target().buildName("b-1");
        }
    }

    private static final class CapturingPresenter implements StartTaskPresenter {
        private BuildName started;
        private Exception failure;

        @Override
        public void presentStarted(BuildName build) {
            this.started = build;
        }

        @Override
        public void presentException(Exception exception) {
            this.failure = exception;
        }
    }
}
