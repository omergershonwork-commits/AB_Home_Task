package com.ab.orders.messaging;

import com.ab.orders.domain.CanonicalOrder;
import com.ab.orders.domain.ProcessedOrder;
import com.ab.orders.persistence.TargetOrderSink;
import com.ab.orders.processing.OrderProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes normalized orders, applies business processing, and persists the final result. */
@Component
@RequiredArgsConstructor
public class NormalizedOrderConsumer {

    private final ObjectMapper objectMapper;
    private final OrderProcessor orderProcessor;
    private final TargetOrderSink targetOrderSink;

    @KafkaListener(
            topics = "${app.kafka.topics.normalized}",
            groupId = "normalized-order-processor"
    )
    public void consume(String payload) throws Exception {
        CanonicalOrder canonicalOrder = objectMapper.readValue(payload, CanonicalOrder.class);
        ProcessedOrder processedOrder = orderProcessor.process(canonicalOrder);
        targetOrderSink.save(canonicalOrder.sourceSystem(), processedOrder);
    }
}
