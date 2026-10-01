package com.banking.notification.service;

import com.banking.notification.dispatchers.NotificationDispatcher;
import com.banking.notification.domain.NotificationLog;
import com.banking.notification.domain.NotificationLog.NotificationChannel;
import com.banking.notification.repository.NotificationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationManager {

    private final List<NotificationDispatcher> dispatchers;
    private final NotificationLogRepository logRepository;

    public NotificationManager(List<NotificationDispatcher> dispatchers, NotificationLogRepository logRepository) {
        this.dispatchers = dispatchers;
        this.logRepository = logRepository;
    }

    @Transactional
    public void notifyCustomer(String recipient, NotificationChannel channel, String eventType, String subject, String body) {
        NotificationDispatcher dispatcher = dispatchers.stream()
                .filter(d -> d.getChannel() == channel)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No dispatcher registered for channel: " + channel));

        String ref = dispatcher.send(recipient, subject, body);

        NotificationLog log = new NotificationLog(
                UUID.randomUUID().toString(),
                recipient,
                channel,
                eventType,
                subject,
                body,
                ref
        );
        logRepository.save(log);
    }

    @Transactional
    public void broadcastOmniChannel(String recipient, String eventType, String subject, String body) {
        for (NotificationDispatcher dispatcher : dispatchers) {
            String ref = dispatcher.send(recipient, subject, body);
            NotificationLog log = new NotificationLog(
                    UUID.randomUUID().toString(),
                    recipient,
                    dispatcher.getChannel(),
                    eventType,
                    subject,
                    body,
                    ref
            );
            logRepository.save(log);
        }
    }
}
