package com.ab.orders.messaging;

import com.ab.orders.domain.CanonicalOrder;
import com.ab.orders.domain.ProcessedOrder;
import com.ab.orders.persistence.TargetOrder;
import com.ab.orders.persistence.TargetOrderSink;
import com.ab.orders.processing.OrderProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Consumes normalized orders in batches, applies business processing, and persists them together. */
@Component
@RequiredArgsConstructor
public class NormalizedOrderConsumer {

    private final ObjectMapper objectMapper;
    private final OrderProcessor orderProcessor;
    private final TargetOrderSink targetOrderSink;

    @KafkaListener(
            topics = "${app.kafka.topics.normalized}",
            groupId = "normalized-order-processor",
            containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consume(List<String> payloads) throws Exception {
        List<TargetOrder> orders = new ArrayList<>(payloads.size());

        for (String payload : payloads) {
            CanonicalOrder canonicalOrder = objectMapper.readValue(payload, CanonicalOrder.class);
            ProcessedOrder processedOrder = orderProcessor.process(canonicalOrder);
            orders.add(new TargetOrder(canonicalOrder.sourceSystem(), processedOrder));
        }

        targetOrderSink.saveAll(orders);
    }
}
