package com.ab.orders.messaging;

import com.ab.orders.domain.CanonicalOrder;
import com.ab.orders.domain.ProcessedOrder;
import com.ab.orders.persistence.FailedOrderPersistenceException;
import com.ab.orders.persistence.TargetOrder;
import com.ab.orders.persistence.TargetOrderSink;
import com.ab.orders.processing.OrderProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.BatchListenerFailedException;
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
    public void consume(List<String> payloads) {
        List<TargetOrder> successfulOrders = new ArrayList<>(payloads.size());

        for (int i = 0; i < payloads.size(); i++) {
            try {
                CanonicalOrder canonicalOrder = objectMapper.readValue(payloads.get(i), CanonicalOrder.class);
                ProcessedOrder processedOrder = orderProcessor.process(canonicalOrder);
                successfulOrders.add(new TargetOrder(canonicalOrder.sourceSystem(), processedOrder));
            } catch (Exception exception) {
                // Records before the failed index may be committed by the Kafka error handler,
                // so persist that successful prefix before identifying the failed record.
                persistOrReportDatabaseFailure(successfulOrders);

                throw new BatchListenerFailedException(
                        "Failed to process normalized order at batch index " + i,
                        exception,
                        i
                );
            }
        }

        persistOrReportDatabaseFailure(successfulOrders);
    }

    private void persistOrReportDatabaseFailure(List<TargetOrder> orders) {
        if (orders.isEmpty()) {
            return;
        }

        try {
            targetOrderSink.saveAll(orders);
        } catch (FailedOrderPersistenceException exception) {
            // The persistence list is always the contiguous prefix of this Kafka batch,
            // so the persistence index maps directly to the Kafka batch index.
            throw new BatchListenerFailedException(
                    "Database rejected normalized order at batch index " + exception.failedIndex(),
                    exception,
                    exception.failedIndex()
            );
        }
    }
}
