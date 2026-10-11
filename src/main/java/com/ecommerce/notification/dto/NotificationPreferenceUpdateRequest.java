package com.ecommerce.notification.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationPreferenceUpdateRequest(
        @NotNull Boolean realtimeOrderStatusEnabled,
        @NotNull Boolean orderInvoiceEmailEnabled) {
}
