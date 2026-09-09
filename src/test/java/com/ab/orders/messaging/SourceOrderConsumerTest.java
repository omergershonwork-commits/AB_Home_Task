package com.ab.orders.messaging;

import com.ab.orders.domain.CanonicalOrder;
import com.ab.orders.normalization.SourceAOrderNormalizer;
import com.ab.orders.normalization.SourceBOrderNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SourceOrderConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void sourceAIsNormalizedAndPublished() throws Exception {
        NormalizedOrderPublisher publisher = mock(NormalizedOrderPublisher.class);
        SourceAOrderConsumer consumer = new SourceAOrderConsumer(
                objectMapper,
                new SourceAOrderNormalizer(),
                publisher
        );

        consumer.consume("""
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
                """);

        ArgumentCaptor<CanonicalOrder> captor = ArgumentCaptor.forClass(CanonicalOrder.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().sourceSystem()).isEqualTo("SOURCE_A");
        assertThat(captor.getValue().orderReference()).isEqualTo("ORD-10001");
    }

    @Test
    void sourceBIsNormalizedAndPublished() throws Exception {
        NormalizedOrderPublisher publisher = mock(NormalizedOrderPublisher.class);
        SourceBOrderConsumer consumer = new SourceBOrderConsumer(
                objectMapper,
                new SourceBOrderNormalizer(),
                publisher
        );

        consumer.consume("""
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
                """);

        ArgumentCaptor<CanonicalOrder> captor = ArgumentCaptor.forClass(CanonicalOrder.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().sourceSystem()).isEqualTo("SOURCE_B");
        assertThat(captor.getValue().customerFullName()).isEqualTo("Jane Miller");
    }
}
