package com.ab.orders.dtos.b;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for an order received from Source System B.
 *
 * <pre>
 * {
 *   "order_number": "ORD-20001",
 *   "customer": {
 *     "id": "CUST-842",
 *     "first_name": "Jane",
 *     "last_name": "Miller",
 *     "country_code": "DE"
 *   },
 *   "created_at": "2026-09-01T11:15:00",
 *   "item": {
 *     "sku": "P200",
 *     "units": 3,
 *     "price": 80.00
 *   }
 * }
 * </pre>
 */
public record SourceBOrderDto(
        @JsonProperty("order_number") String orderNumber,
        CustomerDto customer,
        @JsonProperty("created_at") LocalDateTime createdAt,
        ItemDto item
) {
    public record CustomerDto(
            String id,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            @JsonProperty("country_code") String countryCode
    ) {
    }

    public record ItemDto(
            String sku,
            Integer units,
            BigDecimal price
    ) {
    }
}
