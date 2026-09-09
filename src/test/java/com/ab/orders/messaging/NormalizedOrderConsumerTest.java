package com.ab.orders.messaging;

import com.ab.orders.domain.CanonicalOrder;
import com.ab.orders.domain.ProcessedOrder;
import com.ab.orders.persistence.TargetOrder;
import com.ab.orders.persistence.TargetOrderSink;
import com.ab.orders.processing.OrderProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NormalizedOrderConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void processesAndPersistsNormalizedOrdersAsBatch() throws Exception {
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

        String payload = """
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
                """;

        consumer.consume(List.of(payload, payload));

        verify(processor, times(2)).process(any(CanonicalOrder.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TargetOrder>> captor = ArgumentCaptor.forClass(List.class);
        verify(sink).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().getFirst().sourceSystem()).isEqualTo("SOURCE_A");
        assertThat(captor.getValue().getFirst().order().orderReference()).isEqualTo("ORD-10001");
    }
}
