package com.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.ecommerce.order.Order;

public record OrderResponseDTO(
        Long orderId,
        Long customerId,
        String customerName,
        String status,
        BigDecimal totalAmount,
        String shippingAddress,
        List<OrderItemResponseDTO> items,
        String paymentStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static OrderResponseDTO from(Order order) {
        if (order == null) return null;
        List<OrderItemResponseDTO> items = order.getItems() == null ? List.of()
                : order.getItems().stream().map(OrderItemResponseDTO::from).toList();
        return new OrderResponseDTO(
                order.getId(),
                order.getUser() == null ? null : order.getUser().getId(),
                order.getUser() == null ? null : order.getUser().getName(),
                order.getStatus() == null ? null : order.getStatus().name(),
                order.getTotalAmount(),
                order.getShippingAddress(),
                items,
                order.getPayment() == null || order.getPayment().getStatus() == null
                        ? null : order.getPayment().getStatus().name(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
