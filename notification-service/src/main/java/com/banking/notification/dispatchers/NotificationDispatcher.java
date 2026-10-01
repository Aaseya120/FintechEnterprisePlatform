package com.banking.notification.dispatchers;

import com.banking.notification.domain.NotificationLog.NotificationChannel;

public interface NotificationDispatcher {
    NotificationChannel getChannel();
    String send(String recipient, String subject, String body);
}
