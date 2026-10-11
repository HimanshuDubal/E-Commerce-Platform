package com.ecommerce.dto.response;

import com.ecommerce.product.Product;

import java.math.BigDecimal;

public record ProductSummaryResponseDTO(
		Long id,
        String name,
        BigDecimal price,
        Integer stock,
        String imageUrl
) {
    public static ProductSummaryResponseDTO from(Product product) {
        if (product == null) return null;
        return new ProductSummaryResponseDTO(product.getId(),product.getName(), product.getPrice(),
                product.getStock(), product.getImageUrl());
    }
}
