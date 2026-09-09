package com.ab.orders.messaging;

import com.ab.orders.dtos.a.SourceAOrderDto;
import com.ab.orders.normalization.SourceAOrderNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes Source A raw messages, normalizes them, and publishes the canonical order. */
@Component
public class SourceAOrderConsumer {

    private final ObjectMapper objectMapper;
    private final SourceAOrderNormalizer normalizer;
    private final NormalizedOrderPublisher publisher;

    public SourceAOrderConsumer(
            ObjectMapper objectMapper,
            SourceAOrderNormalizer normalizer,
            NormalizedOrderPublisher publisher
    ) {
        this.objectMapper = objectMapper;
        this.normalizer = normalizer;
        this.publisher = publisher;
    }

    @KafkaListener(topics = "${app.kafka.topics.source-a-raw}", groupId = "source-a-normalizer")
    public void consume(String payload) throws Exception {
        SourceAOrderDto order = objectMapper.readValue(payload, SourceAOrderDto.class);
        publisher.publish(normalizer.normalize(order));
    }
}
