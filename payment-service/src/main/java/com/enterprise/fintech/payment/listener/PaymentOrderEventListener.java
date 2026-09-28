package com.enterprise.fintech.payment.listener;

import com.enterprise.fintech.common.event.OrderEvent;
import com.enterprise.fintech.common.event.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class PaymentOrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentOrderEventListener.class);

    private static final String ORDER_EVENTS_QUEUE = "fintech.order.events.queue";
    private static final String SAGA_COMPENSATION_QUEUE = "fintech.saga.compensation.queue";

    private final JmsTemplate jmsTemplate;

    public PaymentOrderEventListener(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @JmsListener(destination = ORDER_EVENTS_QUEUE)
    public void onOrderEventReceived(OrderEvent orderEvent) {
        log.info("[JMS ActiveMQ] Received Order Event: {} for Order ID: {} Amount: {} [Trace: {}]",
                orderEvent.getEventType(), orderEvent.getOrderId(), orderEvent.getAmount(), orderEvent.getTraceId());

        if (orderEvent.getEventType() == OrderEvent.EventType.ORDER_CREATED) {
            // Check for simulated business failure to demonstrate Saga Compensation pattern
            if (orderEvent.getAmount().compareTo(new BigDecimal("500000.00")) > 0) {
                log.warn("[Saga Failure] Amount {} exceeds automatic clearance limit. Triggering Saga Compensation...",
                        orderEvent.getAmount());

                PaymentEvent compensationEvent = new PaymentEvent(
                        "PAY-FAILED-" + UUID.randomUUID().toString().substring(0, 6),
                        orderEvent.getOrderId(),
                        orderEvent.getAmount(),
                        PaymentEvent.Status.FAILED,
                        null,
                        "Anti-Money Laundering (AML) limit exceeded. Transaction flagged.",
                        Instant.now(),
                        orderEvent.getTraceId()
                );

                jmsTemplate.convertAndSend(SAGA_COMPENSATION_QUEUE, compensationEvent);
                log.info("[Saga Failure] Dispatched compensation event to {}", SAGA_COMPENSATION_QUEUE);
            } else {
                log.info("[JMS ActiveMQ] Order {} successfully cleared asynchronously.", orderEvent.getOrderId());
            }
        }
    }
}
