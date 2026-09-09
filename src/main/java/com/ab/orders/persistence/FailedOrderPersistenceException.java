package com.ab.orders.persistence;

/** Indicates that a deterministic database/data error was isolated to one order in a persistence batch. */
public class FailedOrderPersistenceException extends RuntimeException {

    private final int failedIndex;

    public FailedOrderPersistenceException(int failedIndex, Throwable cause) {
        super("Database rejected order at batch index " + failedIndex, cause);
        this.failedIndex = failedIndex;
    }

    public int failedIndex() {
        return failedIndex;
    }
}
