package com.rikkeibank.notification.service;

import com.rikkeibank.notification.model.NotificationModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Service
public class NotificationService {

    private final Sinks.Many<NotificationModel> notificationSink = Sinks.many().multicast().onBackpressureBuffer();
    private final List<NotificationModel> history = Collections.synchronizedList(new LinkedList<>());
    private static final int MAX_HISTORY = 100;

    public void processNotification(NotificationModel notification) {
        log.info("Processing notification: [{}] {} - Recipient: {}", 
                notification.getType(), notification.getTitle(), notification.getRecipient());

        // Add to history with size limit
        history.add(0, notification);
        if (history.size() > MAX_HISTORY) {
            history.remove(history.size() - 1);
        }

        // Emit to reactive subscribers (SSE)
        Sinks.EmitResult result = notificationSink.tryEmitNext(notification);
        if (result.isFailure()) {
            log.warn("Failed to emit notification to reactive stream: {}", result);
        }
    }

    public Flux<NotificationModel> getNotificationStream() {
        return notificationSink.asFlux();
    }

    public List<NotificationModel> getRecentNotifications() {
        return List.copyOf(history);
    }
}
