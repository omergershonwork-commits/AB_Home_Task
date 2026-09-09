package com.ab.orders.dtos.a;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for an order received from Source System A.
 *
 * <pre>
 * {
 *   "orderId": "ORD-10001",
 *   "customerId": "CUST-501",
 *   "customerName": "John Smith",
 *   "country": "US",
 *   "orderDate": "2026-09-01T10:30:00",
 *   "productCode": "P100",
 *   "quantity": 2,
 *   "unitPrice": 125.50
 * }
 * </pre>
 */
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
