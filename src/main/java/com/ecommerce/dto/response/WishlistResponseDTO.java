package com.ecommerce.dto.response;

import com.ecommerce.wishlist.Wishlist;

import java.time.LocalDateTime;
import java.util.List;


public record WishlistResponseDTO(
       
        List<ProductSummaryResponseDTO> products,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static WishlistResponseDTO from(Wishlist wishlist) {
        if (wishlist == null) return null;
        List<ProductSummaryResponseDTO> products = wishlist.getProducts() == null ? List.of()
                : wishlist.getProducts().stream().map(ProductSummaryResponseDTO::from).toList();
        return new WishlistResponseDTO(products,
                wishlist.getCreatedAt(), wishlist.getUpdatedAt());
    }
}
