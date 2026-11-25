package org.opendatamesh.platform.up.executor.cloudbuild.tasks.services.usecases.logs;

import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.AccessToken;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.AuthMode;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.Build;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.BuildName;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildAccess;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildStatus;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CloudBuildTarget;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.CredentialPolicy;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.GcpCredential;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LogContent;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LogDestination;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LogRead;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LogUnavailable;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.LoggingMode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Logs use case of BDMD-5810, with fake ports. */
class ReadTaskLogsTest {

    private static final CloudBuildTarget TARGET = new CloudBuildTarget("demo-project", "europe-west8");
    private static final BuildName NAME = TARGET.buildName("b-1");

    private final List<LogDestination> read = new ArrayList<>();
    private String content;
    private Exception failure;

    /**
     * Feature: Return the build log
     *
     * Scenario: A build that stores no logs gets a note
     *   Given a build with NONE logging
     *   When DevOps asks for the logs
     *   Then the content says that the build stores no logs
     */
    @Test
    void whenBuildStoresNoLogsThenNote() {
        execute(build(LoggingMode.NONE, Optional.of("gs://bucket")), Map.of());

        assertEquals("Build b-1 stores no logs (options.logging = NONE)\n", content);
        assertTrue(read.isEmpty());
        assertNull(failure);
    }

    @Test
    void whenBucketUnreadableThenCloudLoggingRead() {
        execute(build(LoggingMode.LEGACY, Optional.of("gs://bucket")), Map.of(
                LogDestination.STORAGE, new LogUnavailable("denied"),
                LogDestination.CLOUD_LOGGING, new LogContent("line\n", false)));

        assertEquals("line\n", content);
        assertEquals(List.of(LogDestination.STORAGE, LogDestination.CLOUD_LOGGING), read);
    }

    @Test
    void whenNoDestinationHoldsTheLogThenReasonsListed() {
        execute(build(LoggingMode.LEGACY, Optional.of("gs://bucket")), Map.of(
                LogDestination.STORAGE, new LogUnavailable("storage denied"),
                LogDestination.CLOUD_LOGGING, new LogUnavailable("logging denied")));

        assertEquals("No log available for build b-1 (SUCCESS): storage denied; logging denied\n", content);
    }

    @Test
    void whenTruncatedThenMarkedWithTheBound() {
        execute(build(LoggingMode.GCS_ONLY, Optional.of("gs://bucket")), Map.of(
                LogDestination.STORAGE, new LogContent("abc", true)));

        assertEquals("abc\n… log truncated at 3 bytes …\n", content);
    }

    @Test
    void whenRunIdInAnotherLocationThenInvalidBeforeAnyRead() {
        new ReadTaskLogs(new ReadTaskLogsCommand("projects/demo-project/locations/global/builds/b-1", null), presenter(),
                access(), (access, name) -> Optional.empty(), logs(Map.of())).execute();

        assertTrue(failure.getMessage().contains("not in the configured project"));
        assertTrue(read.isEmpty());
    }

    private void execute(Build build, Map<LogDestination, LogRead> reads) {
        new ReadTaskLogs(new ReadTaskLogsCommand(NAME.value(), null), presenter(), access(),
                (access, name) -> Optional.of(build), logs(reads)).execute();
    }

    private ReadTaskLogsPresenter presenter() {
        return new ReadTaskLogsPresenter() {
            @Override
            public void presentLogs(String logs, Instant generatedAt) {
                content = logs;
            }

            @Override
            public void presentException(Exception exception) {
                failure = exception;
            }
        };
    }

    private static ReadTaskLogsAccessOutboundPort access() {
        return new ReadTaskLogsAccessOutboundPort() {
            @Override
            public CloudBuildTarget target() {
                return TARGET;
            }

            @Override
            public CredentialPolicy credentialPolicy() {
                return new CredentialPolicy(AuthMode.WORKLOAD_IDENTITY, "x-odm-key", Optional.empty());
            }

            @Override
            public CloudBuildAccess open(GcpCredential credential) {
                return new CloudBuildAccess(TARGET, new AccessToken("token"));
            }
        };
    }

    private ReadTaskLogsBuildLogsOutboundPort logs(Map<LogDestination, LogRead> reads) {
        return new ReadTaskLogsBuildLogsOutboundPort() {
            @Override
            public LogRead read(CloudBuildAccess access, Build build, LogDestination destination) {
                read.add(destination);
                return reads.get(destination);
            }

            @Override
            public long maxLogBytes() {
                return 3;
            }
        };
    }

    private static Build build(LoggingMode mode, Optional<String> bucket) {
        return new Build(NAME, CloudBuildStatus.SUCCESS, "SUCCESS", bucket, mode);
    }
}
