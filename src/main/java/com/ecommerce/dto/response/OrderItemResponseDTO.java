package com.ecommerce.dto.response;

import com.ecommerce.order.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponseDTO(
        
        ProductSummaryResponseDTO product,
        Integer quantity,
        BigDecimal priceAtPurchase,
        BigDecimal lineTotal
) {
    public static OrderItemResponseDTO from(OrderItem item) {
        if (item == null) return null;
        Integer quantity = item.getQuantity();
        BigDecimal unitPrice = item.getPriceAtPurchase() == null ? BigDecimal.ZERO : item.getPriceAtPurchase();
        BigDecimal lineTotal = quantity == null ? BigDecimal.ZERO
                : unitPrice.multiply(BigDecimal.valueOf(quantity));
        return new OrderItemResponseDTO(ProductSummaryResponseDTO.from(item.getProduct()),
                quantity, item.getPriceAtPurchase(), lineTotal);
    }
}
