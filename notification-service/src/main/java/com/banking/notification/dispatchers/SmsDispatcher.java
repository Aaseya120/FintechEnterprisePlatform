package com.banking.notification.dispatchers;

import com.banking.notification.domain.NotificationLog.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SmsDispatcher implements NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(SmsDispatcher.class);

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public String send(String recipient, String subject, String body) {
        String messageId = "SMS_" + UUID.randomUUID().toString().substring(0, 10);
        log.info("Dispatched SMS to [{}] [MsgId: {}]: {}", recipient, messageId, body);
        return messageId;
    }
}
