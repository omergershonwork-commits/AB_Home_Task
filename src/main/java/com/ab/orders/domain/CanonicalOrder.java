package com.ab.orders.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CanonicalOrder(
        String sourceSystem,
        String orderReference,
        String customerReference,
        String customerFullName,
        String countryCode,
        LocalDateTime orderTimestamp,
        String productCode,
        Integer quantity,
        BigDecimal unitPrice
) {
}
