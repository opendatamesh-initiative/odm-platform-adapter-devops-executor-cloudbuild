package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCaseException;
import org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases.UseCaseException.Reason;

import java.util.Optional;

/**
 * Which credential a request is served with. In workload-identity mode the executor uses its own identity and ignores
 * any secret header; in service-account-key mode the key header is required.
 */
public record CredentialPolicy(AuthMode mode, String keyHeader, Optional<String> impersonate) {

    public GcpCredential credentialFor(String presentedKey) {
        if (mode == AuthMode.WORKLOAD_IDENTITY) {
            return new ExecutorIdentity(impersonate);
        }
        if (presentedKey == null || presentedKey.isBlank()) {
            throw new UseCaseException(Reason.INVALID,
                    "Header " + keyHeader + " is required: the executor runs in service-account-key mode");
        }
        return new ServiceAccountKey(presentedKey.trim());
    }
}
