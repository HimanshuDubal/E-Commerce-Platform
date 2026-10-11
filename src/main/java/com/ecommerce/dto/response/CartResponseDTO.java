package com.ecommerce.dto.response;

import com.ecommerce.cart.Cart;
import com.ecommerce.cart.CartItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CartResponseDTO(

        List<CartItemResponseDTO> items,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CartResponseDTO from(Cart cart) {
        if (cart == null) return null;
        List<CartItemResponseDTO> items = cart.getItems() == null ? List.of()
                : cart.getItems().stream().map(CartItemResponseDTO::from).toList();
        BigDecimal total = items.stream().map(CartItemResponseDTO::lineTotal)
                .filter(value -> value != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponseDTO(items, total, cart.getCreatedAt(), cart.getUpdatedAt());
    }
}
