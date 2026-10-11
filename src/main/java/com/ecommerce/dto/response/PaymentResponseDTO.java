package com.ecommerce.dto.response;

import com.ecommerce.payment.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponseDTO(
        Long id,
        Long orderId,
        String transactionId,
        String status,
        BigDecimal amount,
        String paymentMethod,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PaymentResponseDTO from(Payment payment) {
        if (payment == null) return null;
        return new PaymentResponseDTO(
                payment.getId(), payment.getOrder() == null ? null : payment.getOrder().getId(),
                payment.getTransactionId(),
                payment.getStatus() == null ? null : payment.getStatus().name(),
                payment.getAmount(), payment.getPaymentMethod(),
                payment.getCreatedAt(), payment.getUpdatedAt()
        );
    }
}
