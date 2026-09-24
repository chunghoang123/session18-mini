package com.rikkeibank.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.common.events.TransferCompletedEvent;
import com.rikkeibank.common.events.TransferFailedEvent;
import com.rikkeibank.notification.model.NotificationModel;
import com.rikkeibank.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "transfer-completed-events", groupId = "notification-group")
    public void consumeTransferCompleted(String message) {
        try {
            TransferCompletedEvent event = objectMapper.readValue(message, TransferCompletedEvent.class);
            log.info("Kafka Consumer: Received TransferCompletedEvent for txId={}", event.getTransactionId());

            NotificationModel notification = NotificationModel.builder()
                    .id("NOTIF-" + UUID.randomUUID().toString().substring(0, 8))
                    .type("SUCCESS")
                    .recipient(event.getFromAccount() + " & " + event.getToAccount())
                    .title("Biến động số dư: Chuyển khoản thành công")
                    .content(String.format("Tài khoản %s đã chuyển thành công số tiền %s VND đến tài khoản %s. Nội dung: %s",
                            event.getFromAccount(), event.getAmount(), event.getToAccount(), event.getNote()))
                    .timestamp(Instant.now())
                    .build();

            notificationService.processNotification(notification);
        } catch (Exception e) {
            log.warn("Error parsing TransferCompletedEvent message: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "transfer-failed-events", groupId = "notification-group")
    public void consumeTransferFailed(String message) {
        try {
            TransferFailedEvent event = objectMapper.readValue(message, TransferFailedEvent.class);
            log.info("Kafka Consumer: Received TransferFailedEvent for txId={}", event.getTransactionId());

            NotificationModel notification = NotificationModel.builder()
                    .id("NOTIF-" + UUID.randomUUID().toString().substring(0, 8))
                    .type("FAILED")
                    .recipient(event.getFromAccount())
                    .title("Giao dịch chuyển khoản thất bại")
                    .content(String.format("Giao dịch chuyển %s VND từ tài khoản %s đến %s thất bại. Lý do: %s. Tiền đã được giữ nguyên/hoàn lại tài khoản.",
                            event.getAmount(), event.getFromAccount(), event.getToAccount(), event.getReason()))
                    .timestamp(Instant.now())
                    .build();

            notificationService.processNotification(notification);
        } catch (Exception e) {
            log.warn("Error parsing TransferFailedEvent message: {}", e.getMessage());
        }
    }
}
