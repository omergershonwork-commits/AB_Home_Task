package com.ab.orders.persistence;

import com.ab.orders.domain.ProcessedOrder;

/** Persists a processed order to the configured target system. */
public interface TargetOrderSink {
    void save(String sourceSystem, ProcessedOrder order);
}
