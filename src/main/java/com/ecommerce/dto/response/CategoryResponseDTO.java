package com.ecommerce.dto.response;

import com.ecommerce.category.Category;

import java.time.LocalDateTime;

public record CategoryResponseDTO(
		Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CategoryResponseDTO from(Category category) {
        if (category == null) return null;
        return new CategoryResponseDTO(category.getId(),category.getName(), category.getDescription(),
                category.getCreatedAt(), category.getUpdatedAt());
    }
}
