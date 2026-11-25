package org.opendatamesh.platform.up.executor.cloudbuild.tasks.model;

/** A service-account JSON key sent by DevOps in the key header, base64 on one line (or raw JSON). */
public record ServiceAccountKey(String encodedKey) implements GcpCredential {

    @Override
    public String toString() {
        return "ServiceAccountKey[***]";
    }
}
