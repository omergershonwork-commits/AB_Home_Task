package com.ab.orders.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/** Retries failed Kafka records briefly and then routes the original record to the matching DLQ. */
@Configuration
public class KafkaErrorConfig {

    @Bean
    DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.topics.source-a-raw}") String sourceATopic,
            @Value("${app.kafka.topics.source-b-raw}") String sourceBTopic,
            @Value("${app.kafka.topics.normalized}") String normalizedTopic,
            @Value("${app.kafka.topics.source-a-dlq}") String sourceADlq,
            @Value("${app.kafka.topics.source-b-dlq}") String sourceBDlq,
            @Value("${app.kafka.topics.normalized-dlq}") String normalizedDlq
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(
                        resolveDlq(record.topic(), sourceATopic, sourceBTopic, normalizedTopic, sourceADlq, sourceBDlq, normalizedDlq),
                        record.partition()
                )
        );

        return new DefaultErrorHandler(recoverer, new FixedBackOff(500L, 2L));
    }

    private String resolveDlq(
            String topic,
            String sourceATopic,
            String sourceBTopic,
            String normalizedTopic,
            String sourceADlq,
            String sourceBDlq,
            String normalizedDlq
    ) {
        if (topic.equals(sourceATopic)) {
            return sourceADlq;
        }
        if (topic.equals(sourceBTopic)) {
            return sourceBDlq;
        }
        if (topic.equals(normalizedTopic)) {
            return normalizedDlq;
        }
        throw new IllegalArgumentException("No DLQ configured for topic: " + topic);
    }
}
