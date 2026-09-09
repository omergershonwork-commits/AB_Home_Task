package com.ab.orders.persistence;

import com.ab.orders.domain.ProcessedOrder;

/** Processed order together with internal source metadata required by persistence. */
public record TargetOrder(
        String sourceSystem,
        ProcessedOrder order
) {
}
