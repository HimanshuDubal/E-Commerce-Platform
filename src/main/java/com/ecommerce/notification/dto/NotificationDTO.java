package com.ecommerce.notification.dto;

import java.time.LocalDateTime;

public record NotificationDTO(
        String type,
        String title,
        String message,
        Long orderId,
        String orderStatus,
        LocalDateTime occurredAt) {
}
