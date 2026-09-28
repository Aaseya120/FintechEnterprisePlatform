package com.enterprise.fintech.order.service;

import com.enterprise.fintech.common.event.OrderEvent;
import com.enterprise.fintech.order.config.JmsConfig;
import com.enterprise.fintech.order.domain.OutboxEvent;
import com.enterprise.fintech.order.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxPublisherJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherJob.class);

    private final OutboxEventRepository outboxEventRepository;
    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisherJob(OutboxEventRepository outboxEventRepository,
                              JmsTemplate jmsTemplate,
                              ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.poll.interval-ms:5000}")
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents();
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events to publish to ActiveMQ", pendingEvents.size());
        for (OutboxEvent event : pendingEvents) {
            try {
                OrderEvent orderEvent = objectMapper.readValue(event.getPayload(), OrderEvent.class);
                jmsTemplate.convertAndSend(JmsConfig.ORDER_EVENTS_QUEUE, orderEvent);

                event.setProcessed(true);
                outboxEventRepository.save(event);
                log.info("Published Outbox Event ID {} (Order: {}) to queue {}",
                        event.getId(), event.getAggregateId(), JmsConfig.ORDER_EVENTS_QUEUE);
            } catch (Exception ex) {
                event.incrementRetry();
                outboxEventRepository.save(event);
                log.error("Failed to publish Outbox Event ID {}: {}", event.getId(), ex.getMessage());
            }
        }
    }
}
