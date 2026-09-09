package com.ab.orders.persistence;

import java.util.List;

/** Persists processed orders to the configured target system. */
public interface TargetOrderSink {
    void saveAll(List<TargetOrder> orders);
}
