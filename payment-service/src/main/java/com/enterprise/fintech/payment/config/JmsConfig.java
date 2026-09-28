package com.enterprise.fintech.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class JmsConfig {

    public static final String ORDER_EVENTS_QUEUE = "fintech.order.events.queue";
    public static final String SAGA_COMPENSATION_QUEUE = "fintech.saga.compensation.queue";

    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");

        Map<String, Class<?>> typeIdMappings = new HashMap<>();
        typeIdMappings.put("OrderEvent", com.enterprise.fintech.common.event.OrderEvent.class);
        typeIdMappings.put("PaymentEvent", com.enterprise.fintech.common.event.PaymentEvent.class);
        converter.setTypeIdMappings(typeIdMappings);

        return converter;
    }
}
