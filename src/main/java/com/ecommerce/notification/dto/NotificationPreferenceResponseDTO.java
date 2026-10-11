package com.ecommerce.notification.dto;

import com.ecommerce.notification.NotificationPreference;

public record NotificationPreferenceResponseDTO(
        boolean realtimeOrderStatusEnabled,
        boolean orderInvoiceEmailEnabled) {

    public static NotificationPreferenceResponseDTO from(NotificationPreference preference) {
        return new NotificationPreferenceResponseDTO(
                preference.isRealtimeOrderStatusEnabled(),
                preference.isOrderInvoiceEmailEnabled());
    }
}
