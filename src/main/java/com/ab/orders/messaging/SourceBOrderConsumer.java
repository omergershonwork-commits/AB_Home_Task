package com.ab.orders.messaging;

import com.ab.orders.dtos.b.SourceBOrderDto;
import com.ab.orders.normalization.SourceBOrderNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes Source B raw messages, normalizes them, and publishes the canonical order. */
@Component
public class SourceBOrderConsumer {

    private final ObjectMapper objectMapper;
    private final SourceBOrderNormalizer normalizer;
    private final NormalizedOrderPublisher publisher;

    public SourceBOrderConsumer(
            ObjectMapper objectMapper,
            SourceBOrderNormalizer normalizer,
            NormalizedOrderPublisher publisher
    ) {
        this.objectMapper = objectMapper;
        this.normalizer = normalizer;
        this.publisher = publisher;
    }

    @KafkaListener(topics = "${app.kafka.topics.source-b-raw}", groupId = "source-b-normalizer")
    public void consume(String payload) throws Exception {
        SourceBOrderDto order = objectMapper.readValue(payload, SourceBOrderDto.class);
        publisher.publish(normalizer.normalize(order));
    }
}
