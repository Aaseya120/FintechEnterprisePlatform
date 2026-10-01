package com.banking.notification.dispatchers;

import com.banking.notification.domain.NotificationLog.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PushDispatcher implements NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PushDispatcher.class);

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public String send(String recipient, String subject, String body) {
        String pushId = "FCM_" + UUID.randomUUID().toString().substring(0, 10);
        log.info("Dispatched Mobile Push Notification to [{}] [FCM Token/UserId: {}]: {}", subject, recipient, body);
        return pushId;
    }
}
