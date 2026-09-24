package com.rikkeibank.transaction.kafka;

import com.rikkeibank.common.events.TransferCompletedEvent;
import com.rikkeibank.common.events.TransferFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventProducer {

    public static final String TRANSFER_COMPLETED_TOPIC = "transfer-completed-events";
    public static final String TRANSFER_FAILED_TOPIC = "transfer-failed-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTransferCompleted(TransferCompletedEvent event) {
        try {
            log.info("Publishing TransferCompletedEvent to Kafka topic [{}]: txId={}", 
                    TRANSFER_COMPLETED_TOPIC, event.getTransactionId());
            kafkaTemplate.send(TRANSFER_COMPLETED_TOPIC, event.getTransactionId(), event);
        } catch (Exception e) {
            log.warn("Failed to publish TransferCompletedEvent to Kafka: {}", e.getMessage());
        }
    }

    public void publishTransferFailed(TransferFailedEvent event) {
        try {
            log.info("Publishing TransferFailedEvent to Kafka topic [{}]: txId={}, reason={}", 
                    TRANSFER_FAILED_TOPIC, event.getTransactionId(), event.getReason());
            kafkaTemplate.send(TRANSFER_FAILED_TOPIC, event.getTransactionId(), event);
        } catch (Exception e) {
            log.warn("Failed to publish TransferFailedEvent to Kafka: {}", e.getMessage());
        }
    }
}
