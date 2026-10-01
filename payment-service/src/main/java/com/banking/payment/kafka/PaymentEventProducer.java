package com.banking.payment.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Component
public class PaymentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, Object>> publish(String topic, String key, Object event, String correlationId) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, event);

        if (correlationId != null) {
            record.headers().add(new RecordHeader("X-Correlation-ID", correlationId.getBytes(StandardCharsets.UTF_8)));
        }

        log.info("Publishing event to Kafka [topic: {}, key: {}, correlationId: {}]", topic, key, correlationId);
        return kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully delivered event to Kafka topic {} [partition: {}, offset: {}]",
                        topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event to Kafka topic {} [key: {}]: {}", topic, key, ex.getMessage(), ex);
            }
        });
    }
}
