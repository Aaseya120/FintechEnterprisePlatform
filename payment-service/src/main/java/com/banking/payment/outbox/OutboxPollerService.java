package com.banking.payment.outbox;

import com.banking.payment.domain.OutboxEventEntity;
import com.banking.payment.kafka.PaymentEventProducer;
import com.banking.payment.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OutboxPollerService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPollerService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final PaymentEventProducer eventProducer;

    public OutboxPollerService(OutboxEventRepository outboxEventRepository, PaymentEventProducer eventProducer) {
        this.outboxEventRepository = outboxEventRepository;
        this.eventProducer = eventProducer;
    }

    /**
     * Polls unpublished outbox messages from Oracle/Postgres DB and reliably forwards to Kafka.
     * Prevents dual-write inconsistencies and supports event-driven Saga consistency.
     */
    @Scheduled(fixedDelay = 500)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEventEntity> pendingEvents = outboxEventRepository.findPendingEvents(PageRequest.of(0, 50));
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending Outbox events to relay to Kafka", pendingEvents.size());

        for (OutboxEventEntity event : pendingEvents) {
            try {
                eventProducer.publish(event.getTopic(), event.getAggregateId(), event.getPayload(), event.getId())
                        .get(); // Synchronously wait for Kafka broker ACK before marking DB row

                event.markPublished();
                outboxEventRepository.save(event);
                log.info("Relayed Outbox event {} to topic {}", event.getId(), event.getTopic());
            } catch (Exception e) {
                log.error("Outbox relay failed for event {}: {}", event.getId(), e.getMessage());
                event.incrementRetry();
                outboxEventRepository.save(event);
                // Break early on persistent Kafka connectivity issues to avoid spamming
                break;
            }
        }
    }
}
