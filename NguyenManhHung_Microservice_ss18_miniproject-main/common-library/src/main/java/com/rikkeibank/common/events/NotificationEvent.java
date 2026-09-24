package com.rikkeibank.common.events;

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
public class NotificationEvent implements Serializable {
    private String eventId;
    private String recipient;
    private String title;
    private String message;
    private String channel; // EMAIL, SMS, PUSH
    @Builder.Default
    private Instant timestamp = Instant.now();
}
