package com.ab.orders.dtos;

import com.ab.orders.dtos.a.SourceAOrderDto;
import com.ab.orders.dtos.b.SourceBOrderDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SourceOrderDtoDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void deserializesSourceAOrder() throws Exception {
        String json = """
                {
                  "orderId": "ORD-10001",
                  "customerId": "CUST-501",
                  "customerName": "John Smith",
                  "country": "US",
                  "orderDate": "2026-09-01T10:30:00",
                  "productCode": "P100",
                  "quantity": 2,
                  "unitPrice": 125.50
                }
                """;

        SourceAOrderDto order = objectMapper.readValue(json, SourceAOrderDto.class);

        assertThat(order.orderId()).isEqualTo("ORD-10001");
        assertThat(order.customerName()).isEqualTo("John Smith");
        assertThat(order.quantity()).isEqualTo(2);
        assertThat(order.unitPrice()).isEqualByComparingTo(new BigDecimal("125.50"));
    }

    @Test
    void deserializesSourceBOrder() throws Exception {
        String json = """
                {
                  "order_number": "ORD-20001",
                  "customer": {
                    "id": "CUST-842",
                    "first_name": "Jane",
                    "last_name": "Miller",
                    "country_code": "DE"
                  },
                  "created_at": "2026-09-01T11:15:00",
                  "item": {
                    "sku": "P200",
                    "units": 3,
                    "price": 80.00
                  }
                }
                """;

        SourceBOrderDto order = objectMapper.readValue(json, SourceBOrderDto.class);

        assertThat(order.orderNumber()).isEqualTo("ORD-20001");
        assertThat(order.customer().firstName()).isEqualTo("Jane");
        assertThat(order.customer().countryCode()).isEqualTo("DE");
        assertThat(order.item().units()).isEqualTo(3);
        assertThat(order.item().price()).isEqualByComparingTo(new BigDecimal("80.00"));
    }
}
