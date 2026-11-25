package org.opendatamesh.platform.up.executor.cloudbuild.utils.usecases;

import java.util.function.Function;

public interface TransactionalOutboundPort {

    void doInTransaction(Runnable runnable);

    <T, R> R doInTransactionWithResults(Function<T, R> function, T arg);
}
