package com.ab.orders.source.a;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SourceAOrderDto(
        String orderId,
        String customerId,
        String customerName,
        String country,
        LocalDateTime orderDate,
        String productCode,
        Integer quantity,
        BigDecimal unitPrice
) {
}
