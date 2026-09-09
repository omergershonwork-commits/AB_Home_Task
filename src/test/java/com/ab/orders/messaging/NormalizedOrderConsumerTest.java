package com.ab.orders.messaging;

import com.ab.orders.domain.CanonicalOrder;
import com.ab.orders.domain.ProcessedOrder;
import com.ab.orders.persistence.TargetOrderSink;
import com.ab.orders.processing.OrderProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NormalizedOrderConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void processesAndPersistsNormalizedOrder() throws Exception {
        OrderProcessor processor = mock(OrderProcessor.class);
        TargetOrderSink sink = mock(TargetOrderSink.class);
        ProcessedOrder processedOrder = new ProcessedOrder(
                "ORD-10001",
                new ProcessedOrder.Customer("CUST-501", "John Smith", "United States"),
                LocalDateTime.parse("2026-09-01T10:30:00"),
                new ProcessedOrder.Product("P100", 2, new BigDecimal("125.50")),
                new BigDecimal("251.00"),
                "USD"
        );
        when(processor.process(any(CanonicalOrder.class))).thenReturn(processedOrder);

        NormalizedOrderConsumer consumer = new NormalizedOrderConsumer(objectMapper, processor, sink);

        consumer.consume("""
                {
                  "sourceSystem": "SOURCE_A",
                  "orderReference": "ORD-10001",
                  "customerReference": "CUST-501",
                  "customerFullName": "John Smith",
                  "countryCode": "US",
                  "orderTimestamp": "2026-09-01T10:30:00",
                  "productCode": "P100",
                  "quantity": 2,
                  "unitPrice": 125.50
                }
                """);

        ArgumentCaptor<CanonicalOrder> captor = ArgumentCaptor.forClass(CanonicalOrder.class);
        verify(processor).process(captor.capture());
        verify(sink).save("SOURCE_A", processedOrder);
        assertThat(captor.getValue().orderReference()).isEqualTo("ORD-10001");
        assertThat(captor.getValue().countryCode()).isEqualTo("US");
    }
}
