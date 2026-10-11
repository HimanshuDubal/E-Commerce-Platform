package com.ecommerce.dto.response;

import com.ecommerce.product.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponseDTO(
		Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        String imageUrl,
        Long categoryId,
        String categoryName,
        Long sellerId,
        String sellerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponseDTO from(Product product) {
        if (product == null) return null;
        return new ProductResponseDTO(
        		product.getId(),
        		product.getName(), product.getDescription(), product.getPrice(),
                product.getStock(), product.getImageUrl(),
                product.getCategory() == null ? null : product.getCategory().getId(),
                product.getCategory() == null ? null : product.getCategory().getName(),
                product.getSeller() == null ? null : product.getSeller().getId(),
                product.getSeller() == null ? null : product.getSeller().getName(),
                product.getCreatedAt(), product.getUpdatedAt()
        );
    }
}
