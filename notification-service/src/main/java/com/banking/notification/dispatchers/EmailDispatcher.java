package com.banking.notification.dispatchers;

import com.banking.notification.domain.NotificationLog.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EmailDispatcher implements NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatcher.class);

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public String send(String recipient, String subject, String body) {
        String messageId = "EMAIL_" + UUID.randomUUID().toString().substring(0, 10);
        log.info("Dispatched Email to [{}] Subject: [{}] [MsgId: {}]", recipient, subject, messageId);
        return messageId;
    }
}
