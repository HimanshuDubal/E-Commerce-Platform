package com.ecommerce.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Immutable snapshot: event listeners never need to serialize or lazily load JPA entities. */
public record OrderLifecycleEvent(
        Type type,
        Long orderId,
        Long userId,
        String customerName,
        String customerEmail,
        String previousStatus,
        String status,
        BigDecimal totalAmount,
        String shippingAddress,
        List<InvoiceLine> items,
        LocalDateTime occurredAt) {

    public OrderLifecycleEvent {
        items = items == null ? List.of() : List.copyOf(items);
        occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
    }

    public enum Type {
        ORDER_PLACED,
        ORDER_STATUS_CHANGED,
        ORDER_CANCELLED
    }

    public record InvoiceLine(
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {
    }
}
