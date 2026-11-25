package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCaseException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialPolicyTest {

    private static final String HEADER = "x-odm-gcp-service-account-key";

    /**
     * Feature: Authenticate to Google
     *
     * Scenario: Key mode without the key header is refused
     *   Given the executor runs in service-account-key mode
     *   When DevOps starts a task without the key header
     *   Then the answer is 400 naming the header
     */
    @Test
    void whenKeyModeWithoutHeaderThenInvalidNamingHeader() {
        CredentialPolicy policy = new CredentialPolicy(AuthMode.SERVICE_ACCOUNT_KEY, HEADER, Optional.empty());

        UseCaseException failure = assertThrows(UseCaseException.class, () -> policy.credentialFor(" "));

        assertEquals(UseCaseException.Reason.INVALID, failure.getReason());
        assertTrue(failure.getMessage().contains(HEADER));
    }

    @Test
    void whenKeyModeWithHeaderThenServiceAccountKeyMaskedInToString() {
        CredentialPolicy policy = new CredentialPolicy(AuthMode.SERVICE_ACCOUNT_KEY, HEADER, Optional.empty());

        GcpCredential credential = policy.credentialFor("c2VjcmV0");

        assertEquals(new ServiceAccountKey("c2VjcmV0"), credential);
        assertFalse(credential.toString().contains("c2VjcmV0"));
    }

    @Test
    void whenWorkloadIdentityThenHeaderIgnoredAndImpersonationKept() {
        CredentialPolicy policy = new CredentialPolicy(AuthMode.WORKLOAD_IDENTITY, HEADER, Optional.of("sa@p.iam.gserviceaccount.com"));

        assertEquals(new ExecutorIdentity(Optional.of("sa@p.iam.gserviceaccount.com")), policy.credentialFor("ignored"));
        assertEquals(new ExecutorIdentity(Optional.of("sa@p.iam.gserviceaccount.com")), policy.credentialFor(null));
    }
}
