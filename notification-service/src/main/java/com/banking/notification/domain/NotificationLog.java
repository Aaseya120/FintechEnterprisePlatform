package com.banking.notification.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
    name = "notification_logs",
    indexes = {
        @Index(name = "idx_notif_recipient_channel", columnList = "recipient, channel"),
        @Index(name = "idx_notif_sent_at", columnList = "sent_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class NotificationLog {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "recipient", length = 100, nullable = false)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20, nullable = false)
    private NotificationChannel channel;

    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType;

    @Column(name = "subject", length = 200, nullable = false)
    private String subject;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "provider_reference", length = 100)
    private String providerReference;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    public enum NotificationChannel { SMS, EMAIL, PUSH }

    public NotificationLog(String id, String recipient, NotificationChannel channel, String eventType,
                           String subject, String content, String providerReference) {
        this.id = id;
        this.recipient = recipient;
        this.channel = channel;
        this.eventType = eventType;
        this.subject = subject;
        this.content = content;
        this.status = "SENT";
        this.providerReference = providerReference;
        this.sentAt = Instant.now();
    }
}
