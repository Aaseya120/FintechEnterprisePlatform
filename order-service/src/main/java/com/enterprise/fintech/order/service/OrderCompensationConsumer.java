package com.enterprise.fintech.order.service;

import com.enterprise.fintech.common.event.PaymentEvent;
import com.enterprise.fintech.order.config.JmsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCompensationConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCompensationConsumer.class);

    private final OrderService orderService;

    public OrderCompensationConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @JmsListener(destination = JmsConfig.SAGA_COMPENSATION_QUEUE)
    public void handlePaymentFailureCompensation(PaymentEvent paymentEvent) {
        log.warn("Received Saga Compensation Event for Order ID: {}, Reason: {}",
                paymentEvent.getOrderId(), paymentEvent.getReason());
        try {
            orderService.compensateOrder(paymentEvent.getOrderId(), paymentEvent.getReason());
        } catch (Exception ex) {
            log.error("Failed to compensate order {}: {}", paymentEvent.getOrderId(), ex.getMessage());
        }
    }
}
