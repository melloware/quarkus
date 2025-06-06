package io.quarkus.agroal.runtime;

import java.sql.Connection;

import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSourceListener;

final class AgroalEventLoggingListener implements AgroalDataSourceListener {

    private static final Logger log = Logger.getLogger("io.agroal.pool");

    private static final String CONN_WITHOUT_TX = "Connection acquired without transaction.";

    private final String datasourceName;
    private final boolean transactionRequirementWarningActive;

    public AgroalEventLoggingListener(String name, boolean transactionRequirementWarningActive) {
        this.datasourceName = "Datasource '" + name + "'";
        this.transactionRequirementWarningActive = transactionRequirementWarningActive;
    }

    @Override
    public void beforeConnectionLeak(Connection connection) {
        log.tracev("{0}: Leak test on connection {1}", datasourceName, connection);
    }

    @Override
    public void beforeConnectionReap(Connection connection) {
        log.tracev("{0}: Reap test on connection {1}", datasourceName, connection);
    }

    @Override
    public void beforeConnectionValidation(Connection connection) {
        log.tracev("{0}: Validation test on connection {1}", datasourceName, connection);
    }

    @Override
    public void onConnectionAcquire(Connection connection) {
        log.tracev("{0}: Acquire connection {1}", datasourceName, connection);
    }

    @Override
    public void onConnectionCreation(Connection connection) {
        log.tracev("{0}: Created connection {1}", datasourceName, connection);
    }

    @Override
    public void onConnectionLeak(Connection connection, Thread thread) {
        log.infov("{0}: Connection leak of {1}", datasourceName, connection);
    }

    @Override
    public void onConnectionReap(Connection connection) {
        log.tracev("{0}: Closing idle connection {1}", datasourceName, connection);
    }

    @Override
    public void onConnectionReturn(Connection connection) {
        log.tracev("{0}: Returning connection {1}", datasourceName, connection);
    }

    @Override
    public void onConnectionDestroy(Connection connection) {
        log.tracev("{0}: Destroyed connection {1}", datasourceName, connection);
    }

    @Override
    public void onWarning(String warning) {
        // See https://github.com/quarkusio/quarkus/issues/44047
        if (warning != null && warning.contains("JDBC resources leaked")) {
            log.debugv("{0}: {1}", datasourceName, warning);
        } else {
            log.warnv("{0}: {1}", datasourceName, warning);
        }
    }

    @Override
    public void onInfo(String message) {
        log.infov("{0}: {1}", datasourceName, message);
    }

    @Override
    public void onWarning(Throwable throwable) {
        if (transactionRequirementWarningActive && CONN_WITHOUT_TX.equals(throwable.getMessage())) {
            log.warnv(throwable, "{0}", datasourceName);
        } else {
            log.warnv("{0}: {1}", datasourceName, throwable.getMessage());
            log.debug("Cause: ", throwable);
        }
    }
}
