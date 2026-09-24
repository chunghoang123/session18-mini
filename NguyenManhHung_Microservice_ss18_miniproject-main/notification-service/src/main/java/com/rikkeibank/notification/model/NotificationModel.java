package com.rikkeibank.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationModel implements Serializable {
    private String id;
    private String type; // SUCCESS, FAILED, ALERT
    private String recipient;
    private String title;
    private String content;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
