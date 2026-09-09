package com.ab.orders.source.b;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
