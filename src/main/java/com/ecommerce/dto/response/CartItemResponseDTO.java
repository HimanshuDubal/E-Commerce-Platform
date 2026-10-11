package com.ecommerce.dto.response;

import com.ecommerce.cart.CartItem;

import java.math.BigDecimal;

public record CartItemResponseDTO(
		Long id,
        ProductSummaryResponseDTO product,
        Integer quantity,
        BigDecimal lineTotal
) {
    public static CartItemResponseDTO from(CartItem item) {
        if (item == null) return null;
        Integer quantity = item.getQuantity();
        BigDecimal price = item.getProduct() == null || item.getProduct().getPrice() == null
                ? BigDecimal.ZERO : item.getProduct().getPrice();
        BigDecimal lineTotal = quantity == null ? BigDecimal.ZERO : price.multiply(BigDecimal.valueOf(quantity));
        return new CartItemResponseDTO(item.getId(),ProductSummaryResponseDTO.from(item.getProduct()),
                quantity, lineTotal);
    }
}
