package com.banking.notification.kafka;

import com.banking.notification.service.NotificationManager;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaConsumer.class);
    private final NotificationManager notificationManager;

    public NotificationKafkaConsumer(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    @KafkaListener(topics = "banking.transactions.completed", groupId = "notification-consumer-group")
    public void onTransactionCompleted(ConsumerRecord<String, Object> record) {
        log.info("Notification Service received Transaction Completed event for key: {}", record.key());
        notificationManager.broadcastOmniChannel(
                "customer@bank.com",
                "TRANSACTION_ALERT",
                "Your Account Has Been Debited / Credited",
                "A financial transfer has successfully completed on your account: " + record.value()
        );
    }

    @KafkaListener(topics = "banking.loan.disbursed", groupId = "notification-consumer-group")
    public void onLoanDisbursed(ConsumerRecord<String, Object> record) {
        log.info("Notification Service received Loan Disbursed event: {}", record.key());
        notificationManager.broadcastOmniChannel(
                "customer@bank.com",
                "LOAN_DISBURSEMENT",
                "Your Loan Has Been Approved & Disbursed!",
                "Funds from your approved loan have been credited to your disbursement account."
        );
    }

    @KafkaListener(topics = "banking.card.blocked", groupId = "notification-consumer-group")
    public void onCardBlocked(ConsumerRecord<String, Object> record) {
        log.warn("Notification Service received Card Blocked event: {}", record.key());
        notificationManager.broadcastOmniChannel(
                "customer@bank.com",
                "SECURITY_ALERT",
                "URGENT: Your Banking Card Has Been Blocked",
                "Your card was blocked to protect your account. Reason: " + record.value()
        );
    }
}
