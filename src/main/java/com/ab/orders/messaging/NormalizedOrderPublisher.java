package com.ab.orders.messaging;

import com.ab.orders.domain.CanonicalOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes canonical orders to the shared normalized Kafka topic. */
@Component
public class NormalizedOrderPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String normalizedTopic;

    public NormalizedOrderPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.normalized}") String normalizedTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.normalizedTopic = normalizedTopic;
    }

    public void publish(CanonicalOrder order) {
        try {
            String key = order.sourceSystem() + ":" + order.orderReference();
            String payload = objectMapper.writeValueAsString(order);
            kafkaTemplate.send(normalizedTopic, key, payload).join();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize normalized order", e);
        }
    }
}
