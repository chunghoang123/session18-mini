package com.rikkeibank.notification.controller;

import com.rikkeibank.common.dto.ApiResponse;
import com.rikkeibank.notification.model.NotificationModel;
import com.rikkeibank.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<NotificationModel> streamNotifications() {
        log.info("Client subscribed to notification SSE stream");
        return notificationService.getNotificationStream();
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<NotificationModel>>> getHistory() {
        List<NotificationModel> list = notificationService.getRecentNotifications();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/simulate")
    public ResponseEntity<ApiResponse<NotificationModel>> simulateNotification(
            @RequestParam(defaultValue = "1000000001") String account,
            @RequestParam(defaultValue = "500000") String amount) {
        NotificationModel model = NotificationModel.builder()
                .id("NOTIF-" + UUID.randomUUID().toString().substring(0, 8))
                .type("SUCCESS")
                .recipient(account)
                .title("Biến động số dư giả lập")
                .content(String.format("Tài khoản %s vừa thực hiện giao dịch thử nghiệm số tiền %s VND", account, amount))
                .timestamp(Instant.now())
                .build();

        notificationService.processNotification(model);
        return ResponseEntity.ok(ApiResponse.success("Simulation dispatched", model));
    }
}
