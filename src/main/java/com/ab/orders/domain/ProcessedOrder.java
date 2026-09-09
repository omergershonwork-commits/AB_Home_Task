package com.ab.orders.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Final business-level order shape ready for the target sink. */
public record ProcessedOrder(
        String orderReference,
        Customer customer,
        LocalDateTime orderTimestamp,
        Product product,
        BigDecimal totalOrderValue,
        String currency
) {
    public record Customer(
            String customerReference,
            String fullName,
            String country
    ) {
    }

    public record Product(
            String code,
            Integer quantity,
            BigDecimal unitPrice
    ) {
    }
}
