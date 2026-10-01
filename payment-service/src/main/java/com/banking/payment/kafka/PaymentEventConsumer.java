package com.banking.payment.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(
            topics = {"banking.transactions.initiated", "banking.transactions.completed"},
            groupId = "payment-audit-consumer-group"
    )
    public void consumeTransactionEvent(ConsumerRecord<String, Object> record) {
        log.info("Received transaction event [topic: {}, key: {}, partition: {}, offset: {}]: payload={}",
                record.topic(), record.key(), record.partition(), record.offset(), record.value());

        // Process ledger notification, AML transaction monitoring, etc.
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, Object> record) {
        log.error("CRITICAL: Message dispatched to Dead Letter Topic (DLT)! [topic: {}, key: {}, payload: {}]",
                record.topic(), record.key(), record.value());
        // Alert operations / PagerDuty / Splunk alert trigger
    }
}
